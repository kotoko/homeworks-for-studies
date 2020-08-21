module Backend(compileCode) where

import qualified AbsWithTypes as AbsT
import CodeInput(CodeInput(..))
import Control.Monad(mapM_)
import Control.Monad.Identity(Identity, runIdentity)
import Control.Monad.Reader(ReaderT, runReaderT, ask)
import Control.Monad.State.Strict(StateT, evalStateT, execStateT, get, put)
import Control.Monad.Writer.Strict(WriterT, execWriterT, tell)
import Data.Char(ord)
import Data.List(intercalate, sortOn)
import qualified Data.Map.Strict as M
import qualified ErrorMsg as E
import PrintAbsT(printIdent)
import PrintAbsT(printProgram)  -- TODO DEBUG

-- Skompiluj kod lub zwróć informację o błędzie.
compileCode :: CodeInput -> AbsT.Program -> Either E.ErrorMsg String
compileCode input tree1 = do
	let tree2 = renameVariables tree1
	let strings = collectStrings tree2
	let varTypes = collectVarTypes tree2
	let out = generateLLVM strings varTypes tree2
	let llvm = case input of
		INStdin -> let s = unlines $ map (\x -> "; " ++ x) $ lines $ printProgram tree2 in s ++ "\n" ++ out
		(INFile _) -> out
	return llvm

type ReMonad = StateT ReState Identity
data ReState = ReState {
		varsRe :: M.Map AbsT.Ident AbsT.Ident,
		counterRe :: Integer
	}
runReMonad :: ReMonad a -> ReState -> a
runReMonad m state = runIdentity $ evalStateT m state

-- Zmień nazwy zmiennych, tak żeby były unikatowe.
renameVariables :: AbsT.Program -> AbsT.Program
renameVariables p = runReMonad (renameProgram p) initState where
	initState :: ReState
	initState = ReState M.empty 0

	putVar :: AbsT.Ident -> ReMonad AbsT.Ident
	putVar n = do
		state <- get
		let vars = varsRe state
		let counter = counterRe state
		let n' = AbsT.Ident ("var" ++ show counter)
		let vars2 = M.insert n n' vars
		put (state {varsRe = vars2, counterRe = counter + 1})
		return n'

	getVar :: AbsT.Ident -> ReMonad AbsT.Ident
	getVar n = do
		state <- get
		let vars = varsRe state
		return $ vars M.! n

	renameProgram :: AbsT.Program -> ReMonad AbsT.Program
	renameProgram (AbsT.Program topDefs) = mapM renameTopDef topDefs >>= return . AbsT.Program

	renameTopDef :: AbsT.TopDef -> ReMonad AbsT.TopDef
	renameTopDef (AbsT.FnDef t n args b ln) = do
		state <- get
		let vars = varsRe state
		args2 <- mapM renameArg args
		b2 <- renameBlock b
		state2 <- get
		put (state2 {varsRe = vars})
		return $ AbsT.FnDef t n args2 b2 ln

	renameArg :: AbsT.Arg -> ReMonad AbsT.Arg
	renameArg (AbsT.Arg t n) = do
		n' <- putVar n
		return $ AbsT.Arg t n'

	renameBlock :: AbsT.Block -> ReMonad AbsT.Block
	renameBlock (AbsT.Block stmts) = do
		state <- get
		let vars = varsRe state
		b2 <- mapM renameStmt stmts
		state2 <- get
		put (state2 {varsRe = vars})
		return $ AbsT.Block b2

	renameStmt :: AbsT.Stmt -> ReMonad AbsT.Stmt
	renameStmt (AbsT.BStmt b ln) = do
		b2 <- renameBlock b
		return $ AbsT.BStmt b2 ln
	renameStmt (AbsT.Decl t items ln) = do
		items2 <- mapM renameItem items
		return $ AbsT.Decl t items2 ln
	renameStmt (AbsT.Ass n e ln) = do
		n2 <- getVar n
		e2 <- renameExpr e
		return $ AbsT.Ass n2 e2 ln
	renameStmt (AbsT.Incr n ln) = do
		n2 <- getVar n
		return $ AbsT.Incr n2 ln
	renameStmt (AbsT.Decr n ln) = do
		n2 <- getVar n
		return $ AbsT.Decr n2 ln
	renameStmt (AbsT.Ret e ln) = do
		e2 <- renameExpr e
		return $ AbsT.Ret e2 ln
	renameStmt (AbsT.Cond e s ln) = do
		e2 <- renameExpr e
		s2 <- renameStmt s
		return $ AbsT.Cond e2 s2 ln
	renameStmt (AbsT.CondElse e s s' ln) = do
		e2 <- renameExpr e
		s2 <- renameStmt s
		s'2 <- renameStmt s'
		return $ AbsT.CondElse e2 s2 s'2 ln
	renameStmt (AbsT.While e s ln) = do
		e2 <- renameExpr e
		s2 <- renameStmt s
		return $ AbsT.While e2 s2 ln
	renameStmt (AbsT.SExp e ln) = do
		e2 <- renameExpr e
		return $ AbsT.SExp e2 ln
	renameStmt s = return s

	renameItem :: AbsT.Item -> ReMonad AbsT.Item
	renameItem (AbsT.NoInit n) = do
		n2 <- putVar n
		return $ AbsT.NoInit n2
	renameItem (AbsT.Init n e) = do
		e2 <- renameExpr e
		n2 <- putVar n
		return $ AbsT.Init n2 e2

	renameExpr :: AbsT.Expr -> ReMonad AbsT.Expr
	renameExpr (AbsT.EVar n ln t) = do
		n2 <- getVar n
		return $ AbsT.EVar n2 ln t
	renameExpr (AbsT.EApp n exprs ln t) = do
		exprs2 <- mapM renameExpr exprs
		return $ AbsT.EApp n exprs2 ln t
	renameExpr (AbsT.Neg e ln t) = do
		e2 <- renameExpr e
		return $ AbsT.Neg e2 ln t
	renameExpr (AbsT.Not e ln t) = do
		e2 <- renameExpr e
		return $ AbsT.Not e2 ln t
	renameExpr (AbsT.EMul e op e' ln t) = do
		e2 <- renameExpr e
		e'2 <- renameExpr e'
		return $ AbsT.EMul e2 op e'2 ln t
	renameExpr (AbsT.EAdd e op e' ln t) = do
		e2 <- renameExpr e
		e'2 <- renameExpr e'
		return $ AbsT.EAdd e2 op e'2 ln t
	renameExpr (AbsT.ERel e op e' ln t) = do
		e2 <- renameExpr e
		e'2 <- renameExpr e'
		return $ AbsT.ERel e2 op e'2 ln t
	renameExpr (AbsT.EAnd e e' ln t) = do
		e2 <- renameExpr e
		e'2 <- renameExpr e'
		return $ AbsT.EAnd e2 e'2 ln t
	renameExpr (AbsT.EOr e e' ln t) = do
		e2 <- renameExpr e
		e'2 <- renameExpr e'
		return $ AbsT.EOr e2 e'2 ln t
	renameExpr e = return e

data SState = SState {
		stringsS :: M.Map String String
	}
type SMonad = StateT SState Identity
runSMonad :: SMonad a -> SState -> SState
runSMonad m state = runIdentity $ execStateT m state

-- Nadaj nazwy wszystkim napisom (literałom).
collectStrings :: AbsT.Program -> M.Map String String
collectStrings p = stringsS $ runSMonad (collectProgram p) initState where
	initState :: SState
	initState = SState M.empty

	addString :: String -> SMonad ()
	addString s = do
		state <- get
		let strings = stringsS state
		case M.lookup s strings of
			(Just _) -> return ()
			Nothing -> do
				let strings2 = M.insert s (".str" ++ show (M.size strings)) strings
				put (state {stringsS = strings2})

	collectProgram :: AbsT.Program -> SMonad ()
	collectProgram (AbsT.Program topDefs) = mapM_ collectTopDef topDefs

	collectTopDef :: AbsT.TopDef -> SMonad ()
	collectTopDef (AbsT.FnDef _ _ _ b _) = collectBlock b

	collectBlock :: AbsT.Block -> SMonad ()
	collectBlock (AbsT.Block stmts) = mapM_ collectStmt stmts

	collectItem :: AbsT.Type -> AbsT.Item -> SMonad ()
	collectItem _ (AbsT.Init _ e) = collectExpr e
	collectItem AbsT.Str (AbsT.NoInit _) = addString ""
	collectItem _ _ = return ()

	collectStmt :: AbsT.Stmt -> SMonad ()
	collectStmt (AbsT.BStmt b _) = collectBlock b
	collectStmt (AbsT.Decl t items _) = mapM_ (collectItem t) items
	collectStmt (AbsT.Ass _ e _) = collectExpr e
	collectStmt (AbsT.Ret e _) = collectExpr e
	collectStmt (AbsT.Cond e s _) = collectExpr e >> collectStmt s
	collectStmt (AbsT.CondElse e s s' _) = collectExpr e >> collectStmt s >> collectStmt s'
	collectStmt (AbsT.While e s _) = collectExpr e >> collectStmt s
	collectStmt (AbsT.SExp e _) = collectExpr e
	collectStmt _ = return ()

	collectExpr :: AbsT.Expr -> SMonad ()
	collectExpr (AbsT.EApp _ exprs _ _) = mapM_ collectExpr exprs
	collectExpr (AbsT.EString s _ _) = addString s
	collectExpr (AbsT.Neg e _ _) = collectExpr e
	collectExpr (AbsT.Not e _ _) = collectExpr e
	collectExpr (AbsT.EMul e _ e' _ _) = collectExpr e >> collectExpr e'
	collectExpr (AbsT.EAdd e _ e' _ _) = collectExpr e >> collectExpr e'
	collectExpr (AbsT.ERel e _ e' _ _) = collectExpr e >> collectExpr e'
	collectExpr (AbsT.EAnd e e' _ _) = collectExpr e >> collectExpr e'
	collectExpr (AbsT.EOr e e' _ _) = collectExpr e >> collectExpr e'
	collectExpr _ = return ()

-- Zapamiętaj typy zmiennych.
collectVarTypes :: AbsT.Program -> M.Map AbsT.Ident AbsT.Type
collectVarTypes p = collectProgram p M.empty where
	collectProgram :: AbsT.Program -> M.Map AbsT.Ident AbsT.Type -> M.Map AbsT.Ident AbsT.Type
	collectProgram (AbsT.Program topDefs) m = foldr collectTopDef m topDefs

	collectTopDef :: AbsT.TopDef -> M.Map AbsT.Ident AbsT.Type -> M.Map AbsT.Ident AbsT.Type
	collectTopDef (AbsT.FnDef _ _ args b _) m = collectBlock b $ foldr collectArg m args

	collectArg :: AbsT.Arg -> M.Map AbsT.Ident AbsT.Type -> M.Map AbsT.Ident AbsT.Type
	collectArg (AbsT.Arg t n) m = M.insert n t m

	collectBlock :: AbsT.Block -> M.Map AbsT.Ident AbsT.Type -> M.Map AbsT.Ident AbsT.Type
	collectBlock (AbsT.Block stmts) m = foldr collectStmt m stmts

	collectStmt :: AbsT.Stmt -> M.Map AbsT.Ident AbsT.Type -> M.Map AbsT.Ident AbsT.Type
	collectStmt (AbsT.BStmt b _) m = collectBlock b m
	collectStmt (AbsT.Decl t items _) m = foldr (collectItem t) m items
	collectStmt (AbsT.Cond _ s _) m = collectStmt s m
	collectStmt (AbsT.CondElse _ s s' _) m = collectStmt s' $ collectStmt s m
	collectStmt (AbsT.While _ s _) m = collectStmt s m
	collectStmt _ m = m

	collectItem :: AbsT.Type -> AbsT.Item -> M.Map AbsT.Ident AbsT.Type -> M.Map AbsT.Ident AbsT.Type
	collectItem t (AbsT.NoInit n) m = M.insert n t m
	collectItem t (AbsT.Init n _) m = M.insert n t m

-- Gdzie znajduje się wartość zmiennej/wyrażenia.
data Place =
	Mem String
	| Reg String
	| ValInt Integer
	| ValBool Bool
	| ValStr String

data GState = GState {
		labelG :: Integer,
		registerG :: Integer,
		modifiedVarsG :: M.Map AbsT.Ident Bool,
		placeVarsG :: M.Map AbsT.Ident Place,
		wasReturnG :: Bool,
		currentLabelG :: String
	}
data GEnv = GEnv {
		stringsG :: M.Map String String,
		varTypesG :: M.Map AbsT.Ident AbsT.Type
	}
type GMonad = StateT GState (ReaderT GEnv (WriterT String Identity))
runGMonad :: GMonad a -> GState -> GEnv -> String
runGMonad m state env = runIdentity $ execWriterT $ runReaderT (evalStateT m state) env

-- Wygeneruj kod dla llvm.
generateLLVM :: M.Map String String -> M.Map AbsT.Ident AbsT.Type -> AbsT.Program -> String
generateLLVM strings varTypes p = runGMonad (genProgram p) initState initEnv where
	initState :: GState
	initState = GState 0 0 M.empty M.empty False ""
	initEnv :: GEnv
	initEnv = GEnv strings varTypes

	-- Nowy rejestr.
	newReg :: GMonad String
	newReg = do
		state <- get
		let nr = registerG state
		put (state {registerG = nr + 1})
		return $ "r" ++ show nr

	-- Nowa etykieta.
	newLabel :: GMonad String
	newLabel = do
		state <- get
		let nr = labelG state
		put (state {labelG = nr + 1})
		return $ "L" ++ show nr

	normaliseString :: String -> String
	normaliseString = concat . map (\c ->
			if ord c == 10 then "\\0A"  -- Nowa linia.
			else if 32 <= ord c && ord c <= 126 then [c]
			else "?"  -- Niewidzialne znaki.
		)

	genStrings :: GMonad ()
	genStrings = do
		env <- ask
		let strings = stringsG env
		mapM_ (\(s, n) -> do
				let len = (length s) + 1  -- Na końcu będzie NULL.
				tell $ "@" ++ n ++ ".internal = constant [" ++ show len ++ " x i8] c\"" ++ normaliseString s ++ "\\00\", align 1" ++ "\n"
				tell $ "@" ++ n ++ " = global i8* getelementptr inbounds ([" ++ show len ++ " x i8], [" ++ show len ++ " x i8]* @" ++ n ++ ".internal, i32 0, i32 0), align 8" ++ "\n"
			) (sortOn snd $ M.toList strings)

	genInternalFns :: GMonad ()
	genInternalFns = mapM_ gen fns where
		fns :: [AbsT.TopDef]
		fns = [
				(AbsT.FnDef AbsT.Void (AbsT.Ident "printInt") [AbsT.Arg AbsT.Int (AbsT.Ident "_")] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Void (AbsT.Ident "printString") [AbsT.Arg AbsT.Str (AbsT.Ident "_")] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Void (AbsT.Ident "printBoolean") [AbsT.Arg AbsT.Bool (AbsT.Ident "_")] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Void (AbsT.Ident "error") [] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Int (AbsT.Ident "readInt") [] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Str (AbsT.Ident "readString") [] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Str (AbsT.Ident "strConcat") [AbsT.Arg AbsT.Str (AbsT.Ident "_"), AbsT.Arg AbsT.Str (AbsT.Ident "_")] (AbsT.Block []) Nothing),
				(AbsT.FnDef AbsT.Bool (AbsT.Ident "strEqual") [AbsT.Arg AbsT.Str (AbsT.Ident "_"), AbsT.Arg AbsT.Str (AbsT.Ident "_")] (AbsT.Block []) Nothing)
			]

		genArgs' :: [AbsT.Arg] -> GMonad ()
		genArgs' args = tell $ intercalate ", " $ map (\(AbsT.Arg t _) -> typeLLVM t) args

		gen :: AbsT.TopDef -> GMonad ()
		gen (AbsT.FnDef t n args _ _) = do
			tell $ "declare " ++ typeLLVM t ++ " @" ++ printIdent n ++ "("
			genArgs' args
			tell $ ")" ++ "\n"

	genProgram :: AbsT.Program -> GMonad ()
	genProgram (AbsT.Program topDefs) = do
		genStrings
		tell "\n"
		genInternalFns
		tell "\n"
		mapM_ (\topDef -> genTopDef topDef >> tell "\n") topDefs

	genTopDef :: AbsT.TopDef -> GMonad ()
	genTopDef (AbsT.FnDef t n args b _) = do
		state <- get
		put (state {modifiedVarsG = M.empty, placeVarsG = M.empty, wasReturnG = False})
		tell $ "define " ++ typeLLVM t ++ " @" ++ printIdent n ++ "("
		genArgs args
		tell $ ") {" ++ "\n"
		label <- newLabel
		state2 <- get
		put (state2 {currentLabelG = label})
		tell $ indent ++ label ++ ":" ++ "\n"
		initArgs args
		genVars b
		genStringLiterals b
		genBlock b
		state2 <- get
		if wasReturnG state2 then return () else genStmt $ AbsT.VRet Nothing
		tell $ "}" ++ "\n"

	-- Wygeneruj wszystkie parametry funkcji.
	genArgs :: [AbsT.Arg] -> GMonad ()
	genArgs args = do
		tell $ intercalate ", " $ map (\(AbsT.Arg t n) -> typeLLVM t ++ " %" ++ printIdent n ++ ".ro") args
		mapM_ (\(AbsT.Arg _ n) -> do
				state <- get
				let placeVars = placeVarsG state
				let modifiedVars = modifiedVarsG state
				let placeVars2 = M.insert n (Reg (printIdent n ++ ".ro")) placeVars
				let modifiedVars2 = M.insert n True modifiedVars
				put (state {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
			) args

	-- Zainicjalizuj pamięć na kopie parametrów funkcji.
	initArgs :: [AbsT.Arg] -> GMonad ()
	initArgs args = mapM_ copy args where
		copy :: AbsT.Arg -> GMonad ()
		copy (AbsT.Arg t n) = do
			-- ~ tell $ indent ++ "%" ++ printIdent n ++ " = alloca " ++ typeLLVM t ++ "\n"
			-- ~ tell $ indent ++ "store " ++ typeLLVM t ++ " %" ++ printIdent n ++ ".ro, " ++ typeLLVM t ++ "* %" ++ printIdent n ++ "\n"
			tell $ indent ++ "%" ++ printIdent n ++ " = alloca " ++ typeLLVM t ++ "\n"

	-- Załaduj wszystkie napisy używane w tej funkcji.
	genStringLiterals :: AbsT.Block -> GMonad ()
	genStringLiterals b = do
			env <- ask
			let strings = stringsG env
			mapM_ gen (l strings)
		where
			l :: M.Map String String -> [String]
			l strings = collect strings b

			gen :: String -> GMonad ()
			gen n = do
				tell $ indent ++ "%" ++ n ++ " = load " ++ typeLLVM AbsT.Str ++ ", " ++ typeLLVM AbsT.Str ++ "* @" ++ n ++ "\n"

			collect :: M.Map String String -> AbsT.Block -> [String]
			collect strings b = map snd $ sortOn snd $ M.toList $ collectBlock strings b M.empty

			collectBlock :: M.Map String String -> AbsT.Block -> M.Map String String -> M.Map String String
			collectBlock strings (AbsT.Block stmts) ss = foldr (collectStmt strings) ss stmts

			collectStmt :: M.Map String String -> AbsT.Stmt -> M.Map String String -> M.Map String String
			collectStmt strings (AbsT.BStmt b _) ss = collectBlock strings b ss
			collectStmt strings (AbsT.Decl t items _) ss = foldr (collectItem strings t) ss items
			collectStmt strings (AbsT.Ass _ e _) ss = collectExpr strings e ss
			collectStmt strings (AbsT.Ret e _) ss = collectExpr strings e ss
			collectStmt strings (AbsT.Cond e s _) ss = collectStmt strings s $ collectExpr strings e ss
			collectStmt strings (AbsT.CondElse e s s' _) ss = collectStmt strings s' $ collectStmt strings s $ collectExpr strings e ss
			collectStmt strings (AbsT.While e s _) ss = collectStmt strings s $ collectExpr strings e ss
			collectStmt strings (AbsT.SExp e _) ss = collectExpr strings e ss
			collectStmt _ _ ss = ss

			collectItem :: M.Map String String -> AbsT.Type -> AbsT.Item -> M.Map String String -> M.Map String String
			collectItem strings t (AbsT.NoInit _) ss = if t == AbsT.Str
				then M.insert "" (strings M.! "") ss
				else ss
			collectItem strings _ (AbsT.Init _ e) ss = collectExpr strings e ss

			collectExpr :: M.Map String String -> AbsT.Expr -> M.Map String String -> M.Map String String
			collectExpr strings (AbsT.EApp _ exprs _ _) ss = foldr (collectExpr strings) ss exprs
			collectExpr strings (AbsT.EString x _ _) ss = M.insert x (strings M.! x) ss
			collectExpr strings (AbsT.Neg e _ _) ss = collectExpr strings e ss
			collectExpr strings (AbsT.Not e _ _) ss = collectExpr strings e ss
			collectExpr strings (AbsT.EMul e _ e' _ _) ss = collectExpr strings e' $ collectExpr strings e ss
			collectExpr strings (AbsT.EAdd e _ e' _ _) ss = collectExpr strings e' $ collectExpr strings e ss
			collectExpr strings (AbsT.ERel e _ e' _ _) ss = collectExpr strings e' $ collectExpr strings e ss
			collectExpr strings (AbsT.EAnd e e' _ _) ss = collectExpr strings e' $ collectExpr strings e ss
			collectExpr strings (AbsT.EOr e e' _ _) ss = collectExpr strings e' $ collectExpr strings e ss
			collectExpr _ _ ss = ss

	-- Zainicjalizuj pamięć na wszystkie zmienne lokalne w funkcji.
	genVars :: AbsT.Block -> GMonad ()
	genVars b = mapM_ gen vars where
		vars :: [(AbsT.Type, AbsT.Ident)]
		vars = collectVars b

		gen :: (AbsT.Type, AbsT.Ident) -> GMonad ()
		gen (t, n) = do
			tell $ indent ++ "%" ++ printIdent n ++ " = alloca " ++ typeLLVM t ++ "\n"

		collectVars :: AbsT.Block -> [(AbsT.Type, AbsT.Ident)]
		collectVars b = sortOn snd $ map fst $ M.toList $ collectBlock b M.empty

		collectBlock :: AbsT.Block -> M.Map (AbsT.Type, AbsT.Ident) Bool -> M.Map (AbsT.Type, AbsT.Ident) Bool
		collectBlock (AbsT.Block stmts) vars = foldr collectStmt vars stmts

		collectStmt :: AbsT.Stmt -> M.Map (AbsT.Type, AbsT.Ident) Bool -> M.Map (AbsT.Type, AbsT.Ident) Bool
		collectStmt (AbsT.BStmt b _) vars = collectBlock b vars
		collectStmt (AbsT.Decl t items _) vars = foldr (\i acc -> M.insert (t, itemIdent i) True acc) vars items
		collectStmt (AbsT.Cond _ s _) vars = collectStmt s vars
		collectStmt (AbsT.CondElse _ s s' _) vars = collectStmt s' $ collectStmt s vars
		collectStmt (AbsT.While _ s _) vars = collectStmt s vars
		collectStmt _ vars = vars

	genBlock :: AbsT.Block -> GMonad ()
	genBlock (AbsT.Block stmts) = mapM_ genStmt stmts

	genStmt :: AbsT.Stmt -> GMonad ()
	genStmt (AbsT.Empty _) = return ()
	genStmt (AbsT.BStmt b _) = genBlock b
	genStmt (AbsT.Decl t items _) = mapM_ gen items where
		gen :: AbsT.Item -> GMonad ()
		gen (AbsT.NoInit n) = do
			state <- get
			let modifiedVars = modifiedVarsG state
			let modifiedVars2 = M.insert n True modifiedVars
			let placeVars = placeVarsG state
			let pl = case t of
				AbsT.Int  -> ValInt 0
				AbsT.Bool -> ValBool False
				AbsT.Str  -> ValStr ""
				_         -> undefined
			let placeVars2 = M.insert n pl placeVars
			put (state {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
		gen (AbsT.Init n e) = do
			pl <- genExpr e
			state <- get
			let modifiedVars = modifiedVarsG state
			let modifiedVars2 = M.insert n True modifiedVars
			let placeVars = placeVarsG state
			let placeVars2 = M.insert n pl placeVars
			put (state {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
	genStmt (AbsT.Ass n e _) = do
		pl <- genExpr e
		state <- get
		let modifiedVars = modifiedVarsG state
		let modifiedVars2 = M.insert n True modifiedVars
		let placeVars = placeVarsG state
		let placeVars2 = M.insert n pl placeVars
		put (state {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
	genStmt (AbsT.Incr n _ ) = do
		state <- get
		let placeVars = placeVarsG state
		case placeVars M.! n of
			-- Znana wartość.
			(ValInt x) -> do
				state2 <- get
				let placeVars2 = M.insert n (ValInt $ x+1) $ placeVarsG state2
				let modifiedVars2 = M.insert n True $ modifiedVarsG state2
				put (state2 {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
			-- Pamięć lub rejestr.
			_ -> do
				(_, vpl) <- loadVar n AbsT.Int
				reg <- newReg
				state2 <- get
				let placeVars2 = M.insert n (Reg reg) $ placeVarsG state2
				let modifiedVars2 = M.insert n True $ modifiedVarsG state2
				put (state2 {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
				tell $ indent ++ "%" ++ reg ++ " = add " ++ typeLLVM AbsT.Int ++ " 1, " ++ vpl ++ "\n"
	genStmt (AbsT.Decr n _ ) = do
		state <- get
		let placeVars = placeVarsG state
		case placeVars M.! n of
			-- Znana wartość.
			(ValInt x) -> do
				state2 <- get
				let placeVars2 = M.insert n (ValInt $ x-1) $ placeVarsG state2
				let modifiedVars2 = M.insert n True $ modifiedVarsG state2
				put (state2 {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
			-- Pamięć lub rejestr.
			_ -> do
				(_, vpl) <- loadVar n AbsT.Int
				reg <- newReg
				state2 <- get
				let placeVars2 = M.insert n (Reg reg) $ placeVarsG state2
				let modifiedVars2 = M.insert n True $ modifiedVarsG state2
				put (state2 {placeVarsG = placeVars2, modifiedVarsG = modifiedVars2})
				tell $ indent ++ "%" ++ reg ++ " = sub " ++ typeLLVM AbsT.Int ++ " " ++ vpl ++ ", 1" ++ "\n"
	genStmt (AbsT.Ret e _) = do
		pl <- genExpr e
		let t = exprType e
		let prefix = indent ++ "ret " ++ typeLLVM t
		state <- get
		put (state {wasReturnG = True})
		case pl of
			m@(Mem _) -> do
				reg <- loadMem m t
				tell $ prefix ++ " %" ++ reg ++ "\n"
			(Reg reg) -> tell $ prefix ++ " %" ++ reg ++ "\n"
			(ValInt x) -> tell $ prefix ++ " " ++ show x ++ "\n"
			(ValBool x) -> tell $ prefix ++ " " ++ (if x then show 1 else show 0) ++ "\n"
			(ValStr x) -> do
				env <- ask
				let strings = stringsG env
				tell $ prefix ++ " %" ++ (strings M.! x) ++ "\n"
	genStmt (AbsT.VRet _) = do
		state <- get
		put (state {wasReturnG = True})
		tell $ indent ++ "ret void" ++ "\n"
	genStmt (AbsT.Cond e s _) = do
		pl <- genExpr e
		let se = case pl of
			(Reg reg) -> "%" ++ reg
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		saveModified
		labelPos   <- newLabel
		labelAfter <- newLabel
		-- Warunek
		tell $ indent ++ "br " ++ typeLLVM AbsT.Bool ++ " " ++ se ++ ", label %" ++ labelPos ++ ", label %" ++ labelAfter ++ "\n"

		-- Ciało if-a.
		tell $ indent ++ labelPos ++ ":" ++ "\n"
		state2 <- get
		put (state2 {currentLabelG = labelPos})
		genStmt s
		moveToMemAll
		state3 <- get
		let wasReturn = wasReturnG state3
		if not wasReturn
			then tell $ indent ++ "br label %" ++ labelAfter ++ "\n"
			else return ()

		-- Po if-ie.
		tell $ indent ++ labelAfter ++ ":" ++ "\n"
		state4 <- get
		put (state4 {currentLabelG = labelAfter, wasReturnG = False})
	genStmt (AbsT.CondElse e s s' _) = do
		pl <- genExpr e
		let se = case pl of
			(Reg reg) -> "%" ++ reg
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		saveModified
		state <- get
		labelPos   <- newLabel
		labelNeg   <- newLabel
		labelAfter <- newLabel
		-- Warunek
		tell $ indent ++ "br " ++ typeLLVM AbsT.Bool ++ " " ++ se ++ ", label %" ++ labelPos ++ ", label %" ++ labelNeg ++ "\n"

		-- Ciało if-a.
		tell $ indent ++ labelPos ++ ":" ++ "\n"
		state2 <- get
		put (state2 {currentLabelG = labelPos})
		genStmt s
		moveToMemAll
		state3 <- get
		let wasReturn1 = wasReturnG state3
		if not wasReturn1
			then tell $ indent ++ "br label %" ++ labelAfter ++ "\n"
			else return ()

		-- Ciało else-a.
		tell $ indent ++ labelNeg ++ ":" ++ "\n"
		let origModifiedVars = modifiedVarsG state
		let origPlaceVars = placeVarsG state
		state4 <- get
		put (state4 {
				currentLabelG = labelNeg,
				modifiedVarsG = origModifiedVars,
				placeVarsG = origPlaceVars,
				wasReturnG = False
			})
		genStmt s'
		moveToMemAll
		state5 <- get
		let wasReturn2 = wasReturnG state5
		if not wasReturn2
			then tell $ indent ++ "br label %" ++ labelAfter ++ "\n"
			else return ()

		-- Po if-ie.
		if wasReturn1 && wasReturn2
			then do
				state6 <- get
				put (state6 {wasReturnG = True})
			else do
				tell $ indent ++ labelAfter ++ ":" ++ "\n"
				state6 <- get
				put (state6 {wasReturnG = False, currentLabelG = labelAfter})

	genStmt (AbsT.While e s _) = do
		labelCond  <- newLabel
		labelLoop  <- newLabel
		labelAfter <- newLabel
		moveToMemAll
		tell $ indent ++ "br label %" ++ labelCond ++ "\n"

		-- Warunek
		tell $ indent ++ labelCond ++ ":" ++ "\n"
		state <- get
		put (state {currentLabelG = labelCond})
		pl <- genExpr e
		let se = case pl of
			(Reg reg) -> "%" ++ reg
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		tell $ indent ++ "br " ++ typeLLVM AbsT.Bool ++ " " ++ se ++ ", label %" ++ labelLoop ++ ", label %" ++ labelAfter ++ "\n"

		-- Ciało pętli.
		tell $ indent ++ labelLoop ++ ":" ++ "\n"
		state2 <- get
		put (state2 {currentLabelG = labelLoop})
		genStmt s
		moveToMemAll
		tell $ indent ++ "br label %" ++ labelCond ++ "\n"

		-- Po pętli.
		tell $ indent ++ labelAfter ++ ":" ++ "\n"
		state3 <- get
		let wasReturn = case (wasReturnG state3, e) of
			(True, (AbsT.ELitTrue _ _)) -> True
			_                           -> False
		put (state3 {
				currentLabelG = labelAfter,
				wasReturnG = wasReturn
			})
		if wasReturn
			then tell $ indent ++ "br label %" ++ labelAfter ++ "\n"
			else return ()

	genStmt (AbsT.SExp e _) = genExpr e >> return ()

	-- Zapisuje zmodyfikowane zmienne do pamięci.
	saveModified :: GMonad ()
	saveModified = do
			state <- get
			let modifiedVars = modifiedVarsG state
			mapM_ save $ map fst $ M.toList modifiedVars
		where
			save :: AbsT.Ident -> GMonad ()
			save n = do
				env <- ask
				let varTypes = varTypesG env
				let t = varTypes M.! n
				state <- get
				let pl = (placeVarsG state) M.! n
				let modifiedVars2 = M.delete n $ modifiedVarsG state
				put (state {modifiedVarsG = modifiedVars2})
				case pl of
					(Mem _) -> return ()
					(Reg reg) -> do
						tell $ indent ++ "store " ++ typeLLVM t ++ " %" ++ reg ++ ", " ++ typeLLVM t ++ "* %" ++ printIdent n ++ "\n"
					(ValInt x) -> do
						tell $ indent ++ "store " ++ typeLLVM t ++ " " ++ show x ++ ", " ++ typeLLVM t ++ "* %" ++ printIdent n ++ "\n"
					(ValBool x) -> do
						tell $ indent ++ "store " ++ typeLLVM t ++ " " ++ show (if x then 1 else 0) ++ ", " ++ typeLLVM t ++ "* %" ++ printIdent n ++ "\n"
					(ValStr x) -> do
						let strings = stringsG env
						tell $ indent ++ "store " ++ typeLLVM t ++ " %" ++ (strings M.! x) ++ ", " ++ typeLLVM t ++ "* %" ++ printIdent n ++ "\n"

	-- Zapisuje zmodyfikowane zmienne do pamięci oraz „zapomina” o istniejących wartościach w rejestrach.
	moveToMemAll :: GMonad ()
	moveToMemAll = do
			saveModified
			state <- get
			mapM_ reset $ map fst $ M.toList $ placeVarsG state
		where
			reset :: AbsT.Ident -> GMonad ()
			reset n = do
				state <- get
				let placeVars2 = M.insert n (Mem $ printIdent n) $ placeVarsG state
				put (state {placeVarsG = placeVars2})

	-- Dla danego miejsca w pamięci zwraca rejestr z załadowaną wartością z niej.
	loadMem :: Place -> AbsT.Type -> GMonad String
	loadMem (Mem m) t = do
		reg <- newReg
		tell $ indent ++ "%" ++ reg ++" = load " ++ typeLLVM t ++ ", " ++ typeLLVM t ++ "* %" ++ m ++ "\n"
		return reg
	loadMem _ _ = undefined

	-- Zwraca napisy postaci "i32 0", "i32 %5"
	-- Ładuje zmienną do rejestru jeśli jest w pamięci lub daje jej wartość.
	loadVar :: AbsT.Ident -> AbsT.Type -> GMonad (String, String)
	loadVar n t = do
		state <- get
		let placeVars = placeVarsG state
		let pl = placeVars M.! n
		case (pl,t) of
			((Mem m), AbsT.Int) -> do
				reg <- newReg
				state <- get
				let placeVars = placeVarsG state
				let placeVars2 = M.insert n (Reg reg) placeVars
				put (state {placeVarsG = placeVars2})
				tell $ indent ++ "%" ++ reg ++ " = load " ++ typeLLVM AbsT.Int ++ ", " ++ typeLLVM AbsT.Int ++ "* %" ++ m ++ "\n"
				return (typeLLVM AbsT.Int, "%" ++ reg)
			((Mem m), AbsT.Bool) -> do
				reg <- newReg
				state <- get
				let placeVars = placeVarsG state
				let placeVars2 = M.insert n (Reg reg) placeVars
				put (state {placeVarsG = placeVars2})
				tell $ indent ++ "%" ++ reg ++ " = load " ++ typeLLVM AbsT.Bool ++ ", " ++ typeLLVM AbsT.Bool ++ "* %" ++ m ++ "\n"
				return (typeLLVM AbsT.Bool, "%" ++ reg)
			((Mem m), AbsT.Str) -> do
				reg <- newReg
				state <- get
				let placeVars = placeVarsG state
				let placeVars2 = M.insert n (Reg reg) placeVars
				put (state {placeVarsG = placeVars2})
				tell $ indent ++ "%" ++ reg ++ " = load " ++ typeLLVM AbsT.Str ++ ", " ++ typeLLVM AbsT.Str ++ "* %" ++ m ++ "\n"
				return (typeLLVM AbsT.Str, "%" ++ reg)
			((Reg r), _) -> return (typeLLVM t, "%" ++ r)
			((ValInt x), AbsT.Int)   -> return (typeLLVM t, show x)
			((ValBool x), AbsT.Bool) -> return (typeLLVM t, show (if x then 1 else 0))
			((ValStr x), AbsT.Str) -> do
				env <- ask
				let strings = stringsG env
				return (typeLLVM t ++ "*", "%" ++ strings M.! x)
			_ -> undefined

	genExpr :: AbsT.Expr -> GMonad Place
	genExpr (AbsT.EVar n _ t) = do
		state <- get
		let placeVars = placeVarsG state
		case placeVars M.! n of
			m@(Mem _) -> do
				reg <- loadMem m t
				state2 <- get
				let placeVars2 = M.insert n (Reg reg) $ placeVarsG state2
				put (state2 {placeVarsG = placeVars2})
				return $ Reg reg
			pl -> return pl
	genExpr (AbsT.ELitInt x _ _) = return $ ValInt x
	genExpr (AbsT.ELitTrue _ _) = return $ ValBool True
	genExpr (AbsT.ELitFalse _ _) = return $ ValBool False
	genExpr (AbsT.EApp n exprs _ t) = do
			places <- mapM genExpr exprs
			let types = map exprType exprs
			exprs' <- mapM genAppHelp $ zip places types
			reg <- newReg
			let prefix = if t /= AbsT.Void then indent ++ "%" ++ reg ++ " = " else indent
			tell $ prefix ++ "call " ++ typeLLVM t ++ " @" ++ printIdent n ++ "(" ++ (intercalate ", " exprs') ++")" ++ "\n"
			return $ Reg reg
		where
			genAppHelp :: (Place, AbsT.Type) -> GMonad String
			genAppHelp (pl, t) = do
				case pl of
					(Reg reg)   -> return $ typeLLVM t ++ " %" ++ reg
					(ValInt x)  -> return $ typeLLVM t ++ " " ++ show x
					(ValBool x) -> return $ typeLLVM t ++ " " ++ (if x then show 1 else show 0)
					(ValStr x)  -> do
						env <- ask
						let strings = stringsG env
						return $ typeLLVM t ++ " %" ++ (strings M.! x)
					_ -> undefined
	genExpr (AbsT.EString x _ _) = return $ ValStr x
	genExpr (AbsT.Neg e _ _) = do
		pl <- genExpr e
		let t = exprType e
		case pl of
			(Reg reg) -> do
				reg2 <- newReg
				tell $ indent ++ "%" ++ reg2 ++ " = mul " ++ typeLLVM t ++ " -1, %" ++ reg ++ "\n"
				return $ Reg reg2
			(ValInt x) -> return (ValInt (-x))
			_ -> undefined
	genExpr (AbsT.Not e _ _) = do
		pl <- genExpr e
		let t = exprType e
		case pl of
			(Reg reg) -> do
				reg2 <- newReg
				tell $ indent ++ "%" ++ reg2 ++ " = sub " ++ typeLLVM t ++ " 1, %" ++ reg ++ "\n"
				return $ Reg reg2
			(ValBool x) -> return (ValBool (not x))
			_ -> undefined
	genExpr (AbsT.EMul e op e' _ t) = do
		-- MOD:  srem i32 a, b
		-- MUL:  mul i32 a, b
		-- DIV:  sdiv i32 a, b
		pl <- genExpr e
		pl' <- genExpr e'
		let sop = case op of
			AbsT.Times -> "mul"
			AbsT.Div   -> "sdiv"
			AbsT.Mod   -> "srem"
		let se = case pl of
			(Reg reg)  -> "%" ++ reg
			(ValInt x) -> show x
			_ -> undefined
		let se' = case pl' of
			(Reg reg)  -> "%" ++ reg
			(ValInt x) -> show x
			_ -> undefined
		reg2 <- newReg
		tell $ indent ++ "%" ++ reg2 ++ " = " ++ sop ++ " " ++ typeLLVM t ++ " " ++ se ++ ", " ++ se' ++ "\n"
		return $ Reg reg2
	genExpr (AbsT.EAdd e op e' _ t) = do
		case t of
			AbsT.Str -> do
				pl <- genExpr e
				pl' <- genExpr e'
				env <- ask
				let strings = stringsG env
				let reg = case pl of
					(Reg reg)  -> reg
					(ValStr x) -> strings M.! x
					_ -> undefined
				let reg' = case pl' of
					(Reg reg')  -> reg'
					(ValStr x) -> strings M.! x
					_ -> undefined
				reg2 <- newReg
				tell $ indent ++ "%" ++ reg2 ++ " = call " ++ typeLLVM t ++ " @strConcat(" ++ typeLLVM t ++ " %" ++ reg ++ ", " ++ typeLLVM t ++ " %" ++ reg' ++ ")" ++ "\n"
				return $ Reg reg2
			AbsT.Int -> do
				pl <- genExpr e
				pl' <- genExpr e'
				let sop = case op of
					AbsT.Plus  -> "add"
					AbsT.Minus -> "sub"
				let se = case pl of
					(Reg reg)  -> "%" ++ reg
					(ValInt x) -> show x
					_ -> undefined
				let se' = case pl' of
					(Reg reg)  -> "%" ++ reg
					(ValInt x) -> show x
					_ -> undefined
				reg2 <- newReg
				tell $ indent ++ "%" ++ reg2 ++ " = " ++ sop ++ " " ++ typeLLVM t ++ " " ++ se ++ ", " ++ se' ++ "\n"
				return $ Reg reg2
			_ -> undefined
	genExpr (AbsT.ERel e op e' _ _) = do
		let t = exprType e
		case t of
			AbsT.Str -> do
				pl <- genExpr e
				pl' <- genExpr e'
				env <- ask
				let strings = stringsG env
				let reg = case pl of
					(Reg reg)  -> reg
					(ValStr x) -> strings M.! x
					_ -> undefined
				let reg' = case pl' of
					(Reg reg')  -> reg'
					(ValStr x) -> strings M.! x
					_ -> undefined
				case op of
					AbsT.EQU -> do
						reg2 <- newReg
						tell $ indent ++ "%" ++ reg2 ++ " = call " ++ typeLLVM AbsT.Bool ++ " @strEqual(" ++ typeLLVM t ++ " %" ++ reg ++ ", " ++ typeLLVM t ++ " %" ++ reg' ++ ")" ++ "\n"
						return $ Reg reg2
					AbsT.NE -> do
						reg2 <- newReg
						reg3 <- newReg
						tell $ indent ++ "%" ++ reg2 ++ " = call " ++ typeLLVM AbsT.Bool ++ " @strEqual(" ++ typeLLVM t ++ " %" ++ reg ++ ", " ++ typeLLVM t ++ " %" ++ reg' ++ ")" ++ "\n"
						tell $ indent ++ "%" ++ reg3 ++ " = sub " ++ typeLLVM AbsT.Bool ++ " 1, %" ++ reg2 ++ "\n"
						return $ Reg reg3
					_ -> undefined
			_ -> do  -- Bool/Int
				pl <- genExpr e
				pl' <- genExpr e'
				let se = case pl of
					(Reg reg)  -> "%" ++ reg
					(ValInt x)  -> show x
					(ValBool x) -> if x then show 1 else show 0
					_ -> undefined
				let se' = case pl' of
					(Reg reg')  -> "%" ++ reg'
					(ValInt x)  -> show x
					(ValBool x) -> if x then show 1 else show 0
					_ -> undefined
				let sop = case op of
					AbsT.LTH -> "slt"
					AbsT.LE  -> "sle"
					AbsT.GTH -> "sgt"
					AbsT.GE  -> "sge"
					AbsT.EQU -> "eq"
					AbsT.NE  -> "ne"
				reg2 <- newReg
				tell $ indent ++ "%" ++ reg2 ++ " = icmp " ++ sop ++ " " ++ typeLLVM t ++ " " ++ se ++ ", " ++ se' ++ "\n"
				return $ Reg reg2
	genExpr (AbsT.EAnd e e' _ _) = do
		-- Pierwsze wyrażenie.
		pl <- genExpr e
		let se = case pl of
			(Reg reg)  -> "%" ++ reg
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		state <- get
		let labelEntry = currentLabelG state
		labelPos   <- newLabel
		labelAfter <- newLabel
		regCond <- newReg
		-- Warunek.
		tell $ indent ++ "%" ++ regCond ++ " = icmp ne " ++ typeLLVM AbsT.Bool ++ " " ++ se ++ ", 0" ++ "\n"
		tell $ indent ++ "br " ++ typeLLVM AbsT.Bool ++ " %" ++ regCond ++ ", label %" ++ labelPos ++ ", label %" ++ labelAfter ++ "\n"

		-- Drugie wyrażenie.
		tell $ indent ++ labelPos ++ ":" ++ "\n"
		state2 <- get
		put (state2 {currentLabelG = labelPos})
		pl' <- genExpr e'
		let se' = case pl' of
			(Reg reg')  -> "%" ++ reg'
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		state3 <- get
		let labelPos2 = currentLabelG state3
		tell $ indent ++ "br label %" ++ labelAfter ++ "\n"

		-- Zebranie wyniku.
		tell $ indent ++ labelAfter ++ ":" ++ "\n"
		state4 <- get
		put (state4 {currentLabelG = labelAfter})
		regResult <- newReg
		tell $ indent ++ "%" ++ regResult ++ " = phi " ++ typeLLVM AbsT.Bool ++ " [ 0, %" ++ labelEntry ++ " ], [ " ++ se' ++ ", %" ++ labelPos2 ++ " ]" ++ "\n"
		return $ Reg regResult
	genExpr (AbsT.EOr e e' _ _) = do
		-- Pierwsze wyrażenie.
		pl <- genExpr e
		let se = case pl of
			(Reg reg)  -> "%" ++ reg
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		state <- get
		let labelEntry = currentLabelG state
		labelPos   <- newLabel
		labelAfter <- newLabel
		regCond <- newReg
		-- Warunek.
		tell $ indent ++ "%" ++ regCond ++ " = icmp eq " ++ typeLLVM AbsT.Bool ++ " " ++ se ++ ", 0" ++ "\n"
		tell $ indent ++ "br " ++ typeLLVM AbsT.Bool ++ " %" ++ regCond ++ ", label %" ++ labelPos ++ ", label %" ++ labelAfter ++ "\n"

		-- ~ -- Drugie wyrażenie.
		tell $ indent ++ labelPos ++ ":" ++ "\n"
		state2 <- get
		put (state2 {currentLabelG = labelPos})
		pl' <- genExpr e'
		let se' = case pl' of
			(Reg reg')  -> "%" ++ reg'
			(ValBool x) -> if x then show 1 else show 0
			_ -> undefined
		state3 <- get
		let labelPos2 = currentLabelG state3
		tell $ indent ++ "br label %" ++ labelAfter ++ "\n"

		-- Zebranie wyniku.
		tell $ indent ++ labelAfter ++ ":" ++ "\n"
		state4 <- get
		put (state4 {currentLabelG = labelAfter})
		regResult <- newReg
		tell $ indent ++ "%" ++ regResult ++ " = phi " ++ typeLLVM AbsT.Bool ++ " [ 1, %" ++ labelEntry ++ " ], [ " ++ se' ++ ", %" ++ labelPos2 ++ " ]" ++ "\n"
		return $ Reg regResult

-- Daj typ wyrażenia.
exprType :: AbsT.Expr -> AbsT.Type
exprType (AbsT.EVar _ _ t) = t
exprType (AbsT.ELitInt _ _ t) = t
exprType (AbsT.ELitTrue _ t) = t
exprType (AbsT.ELitFalse _ t) = t
exprType (AbsT.EApp _ _ _ t) = t
exprType (AbsT.EString _ _ t) = t
exprType (AbsT.Neg _ _ t) = t
exprType (AbsT.Not _ _ t) = t
exprType (AbsT.EMul _ _ _ _ t) = t
exprType (AbsT.EAdd _ _ _ _ t) = t
exprType (AbsT.ERel _ _ _ _ t) = t
exprType (AbsT.EAnd _ _ _ t) = t
exprType (AbsT.EOr _ _ _ t) = t

typeLLVM :: AbsT.Type -> String
typeLLVM AbsT.Int = "i32"
typeLLVM AbsT.Str = "i8*"
typeLLVM AbsT.Bool = "i1"
typeLLVM AbsT.Void = "void"
typeLLVM _ = undefined

indent :: String
indent = "  "

itemIdent :: AbsT.Item -> AbsT.Ident
itemIdent (AbsT.NoInit n) = n
itemIdent (AbsT.Init n _) = n
