module Utils where

import Data.Char (isSpace)
import System.FilePath(replaceExtension, FilePath)
import System.IO(stdout, stderr, hPutStrLn, hPutStr)

-- Wypisz na standardowe wyjście.
putStdout :: String -> IO ()
putStdout = hPutStr stdout
putStdoutLn :: String -> IO ()
putStdoutLn = hPutStrLn stdout

-- Wypisz na standardowe wyjście błędów.
putStderr :: String -> IO ()
putStderr = hPutStr stderr
putStderrLn :: String -> IO ()
putStderrLn = hPutStrLn stderr

-- Zmień rozszrzenie piku na *.ll.
llvmFileExt :: FilePath -> FilePath
llvmFileExt file = replaceExtension file ".ll"

-- Zmień rozszrzenie piku na *.bc.
bcFileExt :: FilePath -> FilePath
bcFileExt file = replaceExtension file ".bc"

-- Usuń nowe linie z końca napisu.
-- https://stackoverflow.com/a/3373478
rstrip :: String -> String
rstrip = reverse . dropWhile isSpace . reverse
