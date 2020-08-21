import CodeInput(CodeInput(..))
import Compiler(compileCode)
import Control.DeepSeq(force)
import Control.Exception(evaluate)
import Paths(envPath, runtimePath)
import System.IO(stdin, hGetContents, FilePath)
import System.IO.Temp(withSystemTempDirectory)
import System.Environment(getArgs, getProgName)
import System.Exit(exitFailure, exitSuccess, ExitCode(..))
import System.FilePath(combine, takeBaseName, takeDirectory, hasExtension, takeExtension)
import System.Process(createProcess, proc, waitForProcess, CreateProcess(..), StdStream(..))
import Utils(putStderr, putStderrLn, putStdout, putStdoutLn, bcFileExt, llvmFileExt, rstrip)

main :: IO ()
main = do
	args <- getArgs
	if elem "-h" args || elem "--help" args
		then usage
		else case args of
			[]     -> usage
			"-":_  -> hGetContents stdin >>= evaluate . force >>= run INStdin
			file:_ -> checkFile file >> readFile file >>= evaluate . force >>= run (INFile file)

run :: CodeInput -> String -> IO ()
run input code = do
	case (compileCode input code, input) of
		(Left err, _) -> fatalError err
		(Right compiledCode, INStdin) -> do
			putStdout compiledCode
			withSystemTempDirectory tempDirName (\dir -> do
					let file = combine dir "a.lat"
					run2 file dir compiledCode
				)
		(Right compiledCode, INFile file) -> do
			let dir = takeDirectory file
			run2 file dir compiledCode

run2 :: String -> String -> String -> IO ()
run2 file dir compiledCode = do
	let fileLlvm = llvmFileExt file
	putStdoutLn $ "Saving " ++ fileLlvm ++ " ..."
	writeFile fileLlvm compiledCode
	compileLLVM file dir
	endedSuccessfully

-- Skompiluj kod przy pomocy LLVM.
compileLLVM :: String -> String -> IO ()
compileLLVM file dir = do
	withSystemTempDirectory tempDirName (\tempDir -> do
			let fileLlvm = llvmFileExt file
			let fileBytecode = combine tempDir $ bcFileExt $ takeBaseName fileLlvm
			let runtimeLlvm = runtimePath
			let runtimeBytecode = combine tempDir $ bcFileExt $ takeBaseName runtimeLlvm
			let finalBytecode = combine dir $ bcFileExt $ takeBaseName fileLlvm
			putStdoutLn $ "Generating " ++ fileBytecode ++ " ..."
			runLlvmAssembler fileLlvm fileBytecode
			putStdoutLn $ "Generating " ++ runtimeBytecode ++ " ..."
			runLlvmAssembler runtimeLlvm runtimeBytecode
			putStdoutLn $ "Generating " ++ finalBytecode ++ " ..."
			runLlvmLinker [fileBytecode, runtimeBytecode] finalBytecode
		)

-- Uruchom asembler llvm-as dla danego pliku.
runLlvmAssembler :: FilePath -> FilePath -> IO ()
runLlvmAssembler src dst = do
	(_, _, Just errh, ph) <- createProcess (proc envPath ["llvm-as", "-o", dst, src]){ std_in = NoStream, std_out = NoStream, std_err = CreatePipe, use_process_jobs = True }
	rc <- waitForProcess ph
	case rc of
		ExitSuccess -> return ()
		ExitFailure _ -> do
			msg <- hGetContents errh
			fatalError msg

-- Uruchom linker llvm-link i stwórz jeden plik wynikowy.
runLlvmLinker :: [FilePath] -> FilePath -> IO ()
runLlvmLinker srcs dst = do
	(_, _, Just errh, ph) <- createProcess (proc envPath (["llvm-link", "-o", dst] ++ srcs)){ std_in = NoStream, std_out = NoStream, std_err = CreatePipe, use_process_jobs = True }
	rc <- waitForProcess ph
	case rc of
		ExitSuccess -> return ()
		ExitFailure _ -> do
			msg <- hGetContents errh
			fatalError msg

-- Sprawdź czy plik ma rozszerzenie *.lat.
checkFile :: FilePath -> IO ()
checkFile file =
	if hasExtension file && takeExtension file == ".lat"
		then return ()
		else putStderrLn "Invalid file extension! Expected: *.lat" >> exitFailure

-- Wyświetl instrukcję obsługi programu.
usage :: IO ()
usage = do
	name <- getProgName
	putStderr $ unlines
		[ "usage: " ++ name ++ " [-h] FILE"
		, "Compile program written in Latte language."
		, ""
		, "When FILE is -, read from standard input and write to standard output."
		, ""
		, "Options:"
		, "  -h, --help      Display help message."
		]
	exitFailure

-- Wypisz komunikat o błędzie i zakończ działanie.
fatalError :: String -> IO ()
fatalError err = do
	putStderrLn "ERROR"
	putStderrLn $ rstrip err
	exitFailure

-- Wypisz komunikat o sukcesie i zakończ działanie.
endedSuccessfully :: IO ()
endedSuccessfully = do
	putStderrLn "OK"
	exitSuccess

tempDirName :: String
tempDirName = "latc_llvm"
