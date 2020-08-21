module Frontend(compileCode) where

import qualified AbsLatte as AbsL
import qualified AbsWithTypes as AbsT
import Control.Monad(foldM, mapM_)
import Control.Monad.Except(ExceptT, runExceptT, throwError)
import Control.Monad.Identity(Identity, runIdentity)
import Control.Monad.Reader(ReaderT, runReaderT, ask, local)
import Control.Monad.State.Strict(StateT, evalStateT, get, put)
import qualified Data.Map.Strict as M
import qualified ErrorMsg as E
import LineNr

type MapFns = M.Map AbsT.Ident (AbsT.Type, [AbsT.Arg], LineNr)
type MapVars = M.Map AbsT.Ident (AbsT.Type, LineNr)
type CurrentFn = (AbsT.Type, AbsT.Ident, [AbsT.Arg], LineNr)

compileCode :: AbsL.Program LineNr -> Either E.ErrorMsg AbsT.Program
compileCode tree1 = do
	let tree2 = addMissingBlocks tree1
	let tree3 = optimizationForTests tree2
	fns <- collectFns tree3
	tree4 <- calculateTypes fns tree3
	tree5 <- checkDuplicatedVariables tree4
	tree6 <- checkVoidVariables tree5
	let tree7 = calculateConstants tree6
	tree8 <- checkReturns tree7
	let tree = tree8
	return tree

-- Upewnij się że wszystkie if-y oraz while-e mają w sobie nowe bloki.
addMissingBlocks :: AbsL.Program LineNr -> AbsL.Program LineNr
addMissingBlocks tree = addProgram tree where
	addProgram :: AbsL.Program LineNr -> AbsL.Program LineNr
	addProgram (AbsL.Program ln topDefs) = AbsL.Program ln (map addTopDef topDefs)

	addTopDef :: AbsL.TopDef LineNr -> AbsL.TopDef LineNr
	addTopDef (AbsL.FnDef ln t n args (AbsL.Block ln2 stmts)) = AbsL.FnDef ln t n args (AbsL.Block ln2 $ map addStmt stmts)

	addStmt :: AbsL.Stmt LineNr -> AbsL.Stmt LineNr
	addStmt (AbsL.BStmt ln (AbsL.Block ln2 stmts)) = AbsL.BStmt ln (AbsL.Block ln2 $ map addStmt stmts)
	addStmt (AbsL.Cond ln e s) = AbsL.Cond ln e s' where
		s' :: AbsL.Stmt LineNr
		s' = case s of
			(AbsL.BStmt ln2 (AbsL.Block ln3 stmts)) -> AbsL.BStmt ln2 $ AbsL.Block ln3 $ map addStmt stmts
			_ -> AbsL.BStmt ln $ AbsL.Block ln [(addStmt s)]
	addStmt (AbsL.CondElse ln e s1 s2) = AbsL.CondElse ln e s1' s2' where
		s1' :: AbsL.Stmt LineNr
		s1' = case s1 of
			(AbsL.BStmt ln2 (AbsL.Block ln3 stmts)) -> AbsL.BStmt ln2 $ AbsL.Block ln3 $ map addStmt stmts
			_ -> AbsL.BStmt ln $ AbsL.Block ln [(addStmt s1)]
		s2' :: AbsL.Stmt LineNr
		s2' = case s2 of
			(AbsL.BStmt ln2 (AbsL.Block ln3 stmts)) -> AbsL.BStmt ln2 $ AbsL.Block ln3 $ map addStmt stmts
			_ -> AbsL.BStmt ln $ AbsL.Block ln [(addStmt s2)]
	addStmt (AbsL.While ln e s) = AbsL.While ln e s' where
		s' :: AbsL.Stmt LineNr
		s' = case s of
			(AbsL.BStmt ln2 (AbsL.Block ln3 stmts)) -> AbsL.BStmt ln2 $ AbsL.Block ln3 $ map addStmt stmts
			_ -> AbsL.BStmt ln $ AbsL.Block ln [(addStmt s)]
	addStmt s = s

-- Wykonaj optymalizację pod test "if(false){...}".
optimizationForTests :: AbsL.Program LineNr -> AbsL.Program LineNr
optimizationForTests tree = optProgram tree where
	optProgram :: AbsL.Program LineNr -> AbsL.Program LineNr
	optProgram (AbsL.Program ln topDefs) = AbsL.Program ln (map optTopDef topDefs)

	optTopDef :: AbsL.TopDef LineNr -> AbsL.TopDef LineNr
	optTopDef (AbsL.FnDef ln t n args (AbsL.Block ln2 stmts)) = AbsL.FnDef ln t n args (AbsL.Block ln2 $ map optStmt stmts)

	optStmt :: AbsL.Stmt LineNr -> AbsL.Stmt LineNr
	optStmt (AbsL.BStmt ln (AbsL.Block ln2 stmts)) = AbsL.BStmt ln (AbsL.Block ln2 $ map optStmt stmts)
	optStmt (AbsL.Cond ln e s) = case e of
		(AbsL.ELitFalse _) -> AbsL.Empty ln
		(AbsL.ELitTrue _)  -> optStmt s
		_                  -> AbsL.Cond ln e (optStmt s)
	optStmt (AbsL.CondElse ln e s1 s2) = case e of
		(AbsL.ELitTrue _)  -> optStmt s1
		(AbsL.ELitFalse _) -> optStmt s2
		_                  -> AbsL.CondElse ln e (optStmt s1) (optStmt s2)
	optStmt (AbsL.While ln e s) = case e of
		(AbsL.ELitFalse _) -> AbsL.Empty ln
		_                  -> AbsL.While ln e (optStmt s)
	optStmt s = s

-- Oblicz zbiór funkcji zdefiniowanych w programie.
-- Wykrywa duplikaty. Sprawdza istnienie funkcji "int main()".
collectFns :: AbsL.Program LineNr -> Either E.ErrorMsg MapFns
collectFns (AbsL.Program _ topDefs) = foldM collect initMap topDefs >>= checkMain where
	collect :: MapFns -> AbsL.TopDef LineNr -> Either E.ErrorMsg MapFns
	collect fns (AbsL.FnDef ln t n args _) = do
		let n' = convertIdent n
		let t' = convertType t
		let ts' = map convertArg args
		case M.lookup n' fns of
			Nothing -> return $ M.insert n' (t', ts', ln) fns
			Just (t2, _, ln2)  -> throwError $ E.DuplicatedFunctionErr t2 n' ln2 t' n' ln
	checkMain :: MapFns -> Either E.ErrorMsg MapFns
	checkMain fns = case M.lookup (AbsT.Ident "main") fns of
		Just (AbsT.Int, [], _) -> return fns
		_                      -> throwError E.MissingMainErr
	initMap :: MapFns
	initMap = M.fromList [
			(AbsT.Ident "printInt", (AbsT.Void, [AbsT.Arg AbsT.Int $ AbsT.Ident "a"], Nothing)),
			(AbsT.Ident "printString", (AbsT.Void, [AbsT.Arg AbsT.Str $ AbsT.Ident "a"], Nothing)),
			(AbsT.Ident "printBoolean", (AbsT.Void, [AbsT.Arg AbsT.Bool $ AbsT.Ident "a"], Nothing)),
			(AbsT.Ident "error", (AbsT.Void, [], Nothing)),
			(AbsT.Ident "readInt", (AbsT.Int, [], Nothing)),
			(AbsT.Ident "readString", (AbsT.Str, [], Nothing)),
			(AbsT.Ident "strConcat", (AbsT.Str, [AbsT.Arg AbsT.Str (AbsT.Ident "a"), AbsT.Arg AbsT.Str (AbsT.Ident "b")], Nothing)),
			(AbsT.Ident "strEqual", (AbsT.Bool, [AbsT.Arg AbsT.Str (AbsT.Ident "a"), AbsT.Arg AbsT.Str (AbsT.Ident "b")], Nothing))
		]  -- Wbudowane funkcje.
	convertIdent :: AbsL.Ident -> AbsT.Ident
	convertIdent (AbsL.Ident n) = AbsT.Ident n
	convertType :: AbsL.Type LineNr -> AbsT.Type
	convertType (AbsL.Int _) = AbsT.Int
	convertType (AbsL.Str _) = AbsT.Str
	convertType (AbsL.Bool _) = AbsT.Bool
	convertType (AbsL.Void _) = AbsT.Void
	convertType (AbsL.Fun _ t ts) = AbsT.Fun (convertType t) (map convertType ts)
	convertArg :: AbsL.Arg LineNr -> AbsT.Arg
	convertArg (AbsL.Arg _ t n) = AbsT.Arg (convertType t) (convertIdent n)

data TState = TState {
		varsT :: MapVars
	}
data TEnv = TEnv {
		currentFnT :: CurrentFn,
		fnsT :: MapFns
	}
type TMonad = StateT TState (ReaderT TEnv (ExceptT E.ErrorMsg Identity))
runTMonad :: TMonad a -> TState -> TEnv -> Either E.ErrorMsg a
runTMonad m state env = runIdentity $ runExceptT $ runReaderT (evalStateT m state) env

-- Oblicz typy w wyrażeniach.
calculateTypes :: MapFns -> AbsL.Program LineNr -> Either E.ErrorMsg AbsT.Program
calculateTypes fns tree = runTMonad (calcProgram tree) initState initEnv where
	initState :: TState
	initState = TState M.empty
	initEnv :: TEnv
	initEnv = TEnv (AbsT.Void, (AbsT.Ident "_"), [], Nothing) fns

	calcProgram :: AbsL.Program LineNr -> TMonad AbsT.Program
	calcProgram (AbsL.Program _ topDefs) = mapM calcTopDef topDefs >>= return . AbsT.Program

	addVar :: AbsT.Ident -> AbsT.Type -> LineNr -> TMonad ()
	addVar n t ln = do
		state <- get
		let vars = varsT state
		let vars' = M.insert n (t, ln) vars
		let state' = state {varsT = vars'}
		put state'
		return ()

	getVar :: AbsT.Ident -> LineNr -> TMonad (AbsT.Type, LineNr)
	getVar n ln = do
		state <- get
		let vars = varsT state
		case M.lookup n vars of
			Just var -> return var
			Nothing  -> do
				env <- ask
				let (fnT, fnN, _, fnLn) = currentFnT env
				throwError $ E.MissingVarErr fnT fnN fnLn n ln

	getFn :: AbsT.Ident -> LineNr -> TMonad (AbsT.Type, [AbsT.Arg], LineNr)
	getFn n ln = do
		env <- ask
		let fnsMap = fnsT env
		case M.lookup n fnsMap of
			Nothing -> do
				let (fnT, fnN, _, fnLn) = currentFnT env
				throwError $ E.MissingFnErr n ln fnT fnN fnLn
			Just (fnT, fnArgs, fnLn) -> return (fnT, fnArgs, fnLn)

	calcTopDef :: AbsL.TopDef LineNr -> TMonad AbsT.TopDef
	calcTopDef (AbsL.FnDef ln t n args b) = do
		state <- get
		let vars = varsT state
		let args2 = map calcArg args
		let n2 = calcIdent n
		let t2 = calcType t
		local (\env -> env {currentFnT = (t2, n2, args2, ln)}) (mapM_ (\(AbsT.Arg argT argN) -> addVar argN argT ln) args2)
		b2 <- local (\env -> env {currentFnT = (t2, n2, args2, ln)}) (calcBlock b)
		state2 <- get
		put (state2 {varsT = vars})
		return $ AbsT.FnDef t2 n2 args2 b2 ln

	calcArg :: AbsL.Arg LineNr -> AbsT.Arg
	calcArg (AbsL.Arg _ t n) = AbsT.Arg (calcType t) (calcIdent n)

	calcType :: AbsL.Type LineNr -> AbsT.Type
	calcType (AbsL.Int _) = AbsT.Int
	calcType (AbsL.Str _) = AbsT.Str
	calcType (AbsL.Bool _) = AbsT.Bool
	calcType (AbsL.Void _) = AbsT.Void
	calcType (AbsL.Fun _ t ts) = AbsT.Fun (calcType t) (map calcType ts)

	calcIdent :: AbsL.Ident -> AbsT.Ident
	calcIdent (AbsL.Ident n) = AbsT.Ident n

	calcBlock :: AbsL.Block LineNr -> TMonad AbsT.Block
	calcBlock (AbsL.Block _ stmts) = do
		state <- get
		let origVars = varsT state
		stmts2 <- mapM calcStmt stmts
		state2 <- get
		put (state2 {varsT = origVars})
		return $ AbsT.Block stmts2

	addItems :: LineNr -> AbsT.Type -> AbsT.Item -> TMonad ()
	addItems ln t (AbsT.NoInit n) = addVar n t ln
	addItems ln t (AbsT.Init n e) = do
		let t' = exprType e
		if t /= t'
			then throwError $ E.TypeMismatchErr t' t e ln
			else addVar n t ln

	calcStmt :: AbsL.Stmt LineNr -> TMonad AbsT.Stmt
	calcStmt (AbsL.Empty ln) = return $ AbsT.Empty ln
	calcStmt (AbsL.BStmt ln b) = do
		b2 <- calcBlock b
		return $ AbsT.BStmt b2 ln
	calcStmt (AbsL.Decl ln t i) = do
		let t2 = calcType t
		i2 <- mapM calcItem i
		mapM_ (addItems ln t2) i2
		return $ AbsT.Decl t2 i2 ln
	calcStmt (AbsL.Ass ln n e) = do
		let n2 = calcIdent n
		(varT, _) <- getVar n2 ln
		(t2, e2) <- calcExpr e
		if t2 == varT
			then return $ AbsT.Ass n2 e2 ln
			else throwError $ E.TypeMismatchErr t2 varT e2 ln
	calcStmt (AbsL.Incr ln n) = do
		let n2 = calcIdent n
		(t', _) <- getVar n2 ln
		if t' /= AbsT.Int
			then throwError $ E.TypeMismatchErr t' AbsT.Int (AbsT.EVar n2 ln t') ln
			else return $ AbsT.Incr n2 ln
	calcStmt (AbsL.Decr ln n) = do
		let n2 = calcIdent n
		(t', _) <- getVar n2 ln
		if t' /= AbsT.Int
			then throwError $ E.TypeMismatchErr t' AbsT.Int (AbsT.EVar n2 ln t') ln
			else return $ AbsT.Decr n2 ln
	calcStmt (AbsL.Ret ln e) = do
		env <- ask
		let (fnT, fnN, _, fnLn) = currentFnT env
		(t2, e2) <- calcExpr e
		if fnT == t2
			then return $ AbsT.Ret e2 ln
			else throwError $ E.TypeMismatchReturnErr t2 fnT fnN fnLn (AbsT.Ret e2 ln) ln
	calcStmt (AbsL.VRet ln) = do
		env <- ask
		let (fnT, fnN, _, fnLn) = currentFnT env
		if fnT == AbsT.Void
			then return $ AbsT.VRet ln
			else throwError $ E.TypeMismatchReturnErr AbsT.Void fnT fnN fnLn (AbsT.VRet ln) ln
	calcStmt (AbsL.Cond ln e s) = do
		(t2, e2) <- calcExpr e
		if t2 /= AbsT.Bool
			then throwError $ E.TypeMismatchErr t2 AbsT.Bool e2 ln
			else do
				s2 <- calcStmt s
				return $ AbsT.Cond e2 s2 ln
	calcStmt (AbsL.CondElse ln e s s') = do
		(t2, e2) <- calcExpr e
		if t2 /= AbsT.Bool
			then throwError $ E.TypeMismatchErr t2 AbsT.Bool e2 ln
			else do
				s2 <- calcStmt s
				s'2 <- calcStmt s'
				return $ AbsT.CondElse e2 s2 s'2 ln
	calcStmt (AbsL.While ln e s) = do
		(t2, e2) <- calcExpr e
		if t2 /= AbsT.Bool
			then throwError $ E.TypeMismatchErr t2 AbsT.Bool e2 ln
			else do
				s2 <- calcStmt s
				return $ AbsT.While e2 s2 ln
	calcStmt (AbsL.SExp ln e) = do
		(_, e2) <- calcExpr e
		return $ AbsT.SExp e2 ln

	calcExpr :: AbsL.Expr LineNr -> TMonad (AbsT.Type, AbsT.Expr)
	calcExpr (AbsL.EVar ln n) = do
		let n2 = calcIdent n
		(t2, _) <- getVar n2 ln
		return (t2, (AbsT.EVar n2 ln t2))
	calcExpr (AbsL.ELitInt ln nr) = return (AbsT.Int, (AbsT.ELitInt nr ln AbsT.Int))
	calcExpr (AbsL.ELitTrue ln) = return (AbsT.Bool, (AbsT.ELitTrue ln AbsT.Bool))
	calcExpr (AbsL.ELitFalse ln) = return (AbsT.Bool, (AbsT.ELitFalse ln AbsT.Bool))
	calcExpr (AbsL.EApp ln n exprs) = let
			checkArgs :: [AbsT.Arg] -> [(AbsT.Type, AbsT.Expr)] -> TMonad ()
			checkArgs ((AbsT.Arg t1 _):args) ((t2, e2):types) =
				if t1 /= t2
					then throwError $ E.TypeMismatchErr t2 t1 e2 ln
					else checkArgs args types
			checkArgs _ _ = return ()
		in do
			let n2 = calcIdent n
			(fnT, fnArgs, _) <- getFn n2 ln
			res <- mapM calcExpr exprs
			let (_, exprs2) = unzip res
			if length fnArgs /= length exprs2
				then do
					env <- ask
					let (currentT, currentN, _, currentLn) = currentFnT env
					let lenExprs2 = fromIntegral $ length exprs2
					let lenFnArgs = fromIntegral $ length fnArgs
					throwError $ E.WrongArgsApplyErr lenExprs2 lenFnArgs n2 ln currentT currentN currentLn
				else checkArgs fnArgs res >> return (fnT, (AbsT.EApp n2 exprs2 ln fnT))
	calcExpr (AbsL.EString ln str) = return (AbsT.Str, (AbsT.EString str ln AbsT.Str))
	calcExpr (AbsL.Neg ln e) = do
		(t2, e2) <- calcExpr e
		if t2 /= AbsT.Int
			then throwError $ E.TypeMismatchErr t2 AbsT.Int e2 ln
			else return (AbsT.Int, (AbsT.Neg e2 ln AbsT.Int))
	calcExpr (AbsL.Not ln e) = do
		(t2, e2) <- calcExpr e
		if t2 /= AbsT.Bool
			then throwError $ E.TypeMismatchErr t2 AbsT.Bool e2 ln
			else return (AbsT.Bool, (AbsT.Not e2 ln AbsT.Bool))
	calcExpr (AbsL.EMul ln e op e') = do
		(t2, e2) <- calcExpr e
		(t'2, e'2) <- calcExpr e'
		let op2 = calcMulOp op
		case (t2, t'2) of
			(AbsT.Int, AbsT.Int) -> return (AbsT.Int, (AbsT.EMul e2 op2 e'2 ln AbsT.Int))
			(AbsT.Int, _)        -> throwError $ E.TypeMismatchErr t'2 AbsT.Int e'2 ln
			_                    -> throwError $ E.TypeMismatchErr t2 AbsT.Int e2 ln
	calcExpr (AbsL.EAdd ln e op e') = do
		(t2, e2) <- calcExpr e
		(t'2, e'2) <- calcExpr e'
		let op2 = calcAddOp op
		case (t2, op2, t'2) of
			(AbsT.Str, AbsT.Plus, AbsT.Str) -> return (AbsT.Str, (AbsT.EAdd e2 op2 e'2 ln AbsT.Str))
			(AbsT.Int, _, AbsT.Int)         -> return (AbsT.Int, (AbsT.EAdd e2 op2 e'2 ln AbsT.Int))
			(AbsT.Str, AbsT.Plus, _)        -> throwError $ E.TypeMismatchErr t'2 AbsT.Str e'2 ln
			(AbsT.Int, _, _)                -> throwError $ E.TypeMismatchErr t'2 AbsT.Int e'2 ln
			_                               -> throwError $ E.TypeMismatchErr t2 AbsT.Int e2 ln
	calcExpr (AbsL.ERel ln e op e') = do
		(t2, e2) <- calcExpr e
		(t'2, e'2) <- calcExpr e'
		let op2 = calcRelOp op
		case (t2, op2, t'2) of
			(AbsT.Int, AbsT.EQU, AbsT.Int)   -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Int, AbsT.EQU, _)          -> throwError $ E.TypeMismatchErr t'2 AbsT.Int e'2 ln
			(AbsT.Int, AbsT.NE, AbsT.Int)    -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Int, AbsT.NE, _)           -> throwError $ E.TypeMismatchErr t'2 AbsT.Int e'2 ln
			(AbsT.Str, AbsT.EQU, AbsT.Str)   -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Str, AbsT.EQU, _)          -> throwError $ E.TypeMismatchErr t'2 AbsT.Str e'2 ln
			(AbsT.Str, AbsT.NE, AbsT.Str)    -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Str, AbsT.NE, _)           -> throwError $ E.TypeMismatchErr t'2 AbsT.Str e'2 ln
			(AbsT.Bool, AbsT.EQU, AbsT.Bool) -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Bool, AbsT.EQU, _)         -> throwError $ E.TypeMismatchErr t'2 AbsT.Bool e'2 ln
			(AbsT.Bool, AbsT.NE, AbsT.Bool)  -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Bool, AbsT.NE, _)          -> throwError $ E.TypeMismatchErr t'2 AbsT.Bool e'2 ln
			(AbsT.Int, _, AbsT.Int)          -> return (AbsT.Bool, (AbsT.ERel e2 op2 e'2 ln AbsT.Bool))
			(AbsT.Int, _, _)                  -> throwError $ E.TypeMismatchErr t'2 AbsT.Int e'2 ln
			_                                -> throwError $ E.TypeMismatchErr t2 AbsT.Int e2 ln
	calcExpr (AbsL.EAnd ln e e') = do
		(t2, e2) <- calcExpr e
		(t'2, e'2) <- calcExpr e'
		case (t2, t'2) of
			(AbsT.Bool, AbsT.Bool) -> return (AbsT.Bool, (AbsT.EAnd e2 e'2 ln AbsT.Bool))
			(AbsT.Bool, _)         -> throwError $ E.TypeMismatchErr t'2 AbsT.Bool e'2 ln
			_                      -> throwError $ E.TypeMismatchErr t2 AbsT.Bool e2 ln
	calcExpr (AbsL.EOr ln e e') = do
		(t2, e2) <- calcExpr e
		(t'2, e'2) <- calcExpr e'
		case (t2, t'2) of
			(AbsT.Bool, AbsT.Bool) -> return (AbsT.Bool, (AbsT.EOr e2 e'2 ln AbsT.Bool))
			(AbsT.Bool, _)         -> throwError $ E.TypeMismatchErr t'2 AbsT.Bool e'2 ln
			_                      -> throwError $ E.TypeMismatchErr t2 AbsT.Bool e2 ln

	calcMulOp :: AbsL.MulOp LineNr -> AbsT.MulOp
	calcMulOp (AbsL.Times _) = AbsT.Times
	calcMulOp (AbsL.Div _) = AbsT.Div
	calcMulOp (AbsL.Mod _) = AbsT.Mod

	calcAddOp :: AbsL.AddOp LineNr -> AbsT.AddOp
	calcAddOp (AbsL.Plus _) = AbsT.Plus
	calcAddOp (AbsL.Minus _) = AbsT.Minus

	calcRelOp :: AbsL.RelOp LineNr -> AbsT.RelOp
	calcRelOp (AbsL.LTH _) = AbsT.LTH
	calcRelOp (AbsL.LE _) = AbsT.LE
	calcRelOp (AbsL.GTH _) = AbsT.GTH
	calcRelOp (AbsL.GE _) = AbsT.GE
	calcRelOp (AbsL.EQU _) = AbsT.EQU
	calcRelOp (AbsL.NE _) = AbsT.NE

	calcItem :: AbsL.Item LineNr -> TMonad AbsT.Item
	calcItem (AbsL.NoInit _ n) = return $ AbsT.NoInit $ calcIdent n
	calcItem (AbsL.Init _ n e) = do
		let n2 = calcIdent n
		(_, e2) <- calcExpr e
		return $ AbsT.Init n2 e2

type RMonad = ExceptT E.ErrorMsg Identity
runRMonad :: RMonad a -> Either E.ErrorMsg a
runRMonad m = runIdentity $ runExceptT m

-- Sprawdź czy funkcja zawsze kończy się returnem. Może usuąć martwy kod po returnie.
checkReturns :: AbsT.Program -> Either E.ErrorMsg AbsT.Program
checkReturns tree = runRMonad (checkProgram tree) where
	checkProgram :: AbsT.Program -> RMonad AbsT.Program
	checkProgram (AbsT.Program topdefs) = mapM checkTopDef topdefs >>= return . AbsT.Program

	checkTopDef :: AbsT.TopDef -> RMonad AbsT.TopDef
	checkTopDef (AbsT.FnDef t n args b ln) = do
		(ret, b') <- checkBlock b
		let newTopDef = AbsT.FnDef t n args b' ln
		case (ret, t) of
			(_, AbsT.Void) -> return newTopDef
			(True, _)      -> return newTopDef
			(False, _)     -> throwError $ E.MissingReturnErr t n ln

	checkBlock :: AbsT.Block -> RMonad (Bool, AbsT.Block)
	checkBlock (AbsT.Block stmts) = do
		(ret, stmts') <- step stmts
		return (ret, AbsT.Block stmts')

	step :: [AbsT.Stmt] -> RMonad (Bool, [AbsT.Stmt])
	step [] = return (False, [])
	step (s:stmts) = do
		(ret, s') <- checkStmt s
		if ret
			then return (True, [s'])
			else do
				(ret2, stmts') <- step stmts
				return (ret2, s':stmts')

	checkStmt :: AbsT.Stmt -> RMonad (Bool, AbsT.Stmt)
	checkStmt s@(AbsT.Empty _) = return (False, s)
	checkStmt (AbsT.BStmt b ln) = do
		(ret, b') <- checkBlock b
		return (ret, AbsT.BStmt b' ln)
	checkStmt s@(AbsT.Decl _ _ _) = return (False, s)
	checkStmt s@(AbsT.Ass _ _ _) = return (False, s)
	checkStmt s@(AbsT.Incr _ _) = return (False, s)
	checkStmt s@(AbsT.Decr _ _) = return (False, s)
	checkStmt s@(AbsT.Ret _ _) = return (True, s)
	checkStmt s@(AbsT.VRet _) = return (True, s)
	checkStmt (AbsT.Cond e s ln) = do
		(ret, s') <- checkStmt s
		let newStmt = AbsT.Cond e s' ln
		case (ret, e) of
			(True, (AbsT.ELitTrue _ _)) -> return (True, newStmt)
			_                      -> return (False, newStmt)
	checkStmt (AbsT.CondElse e s s' ln) = do
		(ret1, s1) <- checkStmt s
		(ret2, s2) <- checkStmt s'
		let newStmt = AbsT.CondElse e s1 s2 ln
		case (ret1, ret2, e) of
			(True, True, _)             -> return (True, newStmt)
			(True, _, (AbsT.ELitTrue _ _))  -> return (True, newStmt)
			(_, True, (AbsT.ELitFalse _ _)) -> return (True, newStmt)
			_                           -> return (False, newStmt)
	checkStmt (AbsT.While e s ln) = do
		(ret, s') <- checkStmt s
		let newStmt = AbsT.While e s' ln
		case (ret, e) of
			(True, AbsT.ELitTrue _ _) -> return (True, newStmt)
			_                     -> return (False, newStmt)
	checkStmt s@(AbsT.SExp _ _) = return (False, s)

type DMonad = StateT DState (ReaderT DEnv (ExceptT E.ErrorMsg Identity))
data DEnv = DEnv {
		currentFnD :: CurrentFn
	}
data DState = DState {
		varsD :: MapVars
	}
runDMonad :: DMonad a -> DState -> DEnv -> Either E.ErrorMsg a
runDMonad m state env = runIdentity $ runExceptT $ runReaderT (evalStateT m state) env

-- Sprawdź czy zawsze w każdym bloku jest co najwyżej jedna zmienna o danej nazwie.
checkDuplicatedVariables :: AbsT.Program -> Either E.ErrorMsg AbsT.Program
checkDuplicatedVariables p = runDMonad (checkProgram p) initState initEnv >> return p where
	initState :: DState
	initState = DState M.empty
	initEnv :: DEnv
	initEnv = DEnv (AbsT.Void, (AbsT.Ident "_"), [], Nothing)

	checkProgram :: AbsT.Program -> DMonad ()
	checkProgram (AbsT.Program topDefs) = mapM_ checkTopDef topDefs

	addVar :: AbsT.Ident -> AbsT.Type -> LineNr -> DMonad ()
	addVar n t ln = do
		state <- get
		let vars = varsD state
		env <- ask
		let (fnT, fnN, _, fnLn) = currentFnD env
		case M.lookup n vars of
			(Just (t', ln')) -> throwError $ E.DuplicatedVarErr fnT fnN fnLn t' n ln' t n ln
			Nothing -> do
				let vars2 = M.insert n (t,ln) vars
				put (state {varsD = vars2})

	checkArg :: AbsT.Arg -> DMonad ()
	checkArg (AbsT.Arg t n) = do
		env <- ask
		let (_, _, _, fnLn) = currentFnD env
		addVar n t fnLn

	checkTopDef :: AbsT.TopDef -> DMonad ()
	checkTopDef (AbsT.FnDef t n args (AbsT.Block stmts) ln) = do
		state <- get
		let vars = varsD state
		let current = (t, n, args, ln)
		local (\env -> env {currentFnD = current}) $ mapM_ checkArg args
		local (\env -> env {currentFnD = current}) $ mapM_ checkStmt stmts
		state2 <- get
		put (state2 {varsD = vars})

	checkItem :: AbsT.Type -> LineNr -> AbsT.Item -> DMonad ()
	checkItem t ln i = do
		let n = case i of {(AbsT.NoInit n') -> n'; (AbsT.Init n' _) -> n'}
		addVar n t ln

	checkStmt :: AbsT.Stmt -> DMonad ()
	checkStmt (AbsT.BStmt b _) = checkBlock b
	checkStmt (AbsT.Decl t items ln) = mapM_ (checkItem t ln) items
	checkStmt (AbsT.Cond _ s _) = checkStmt s
	checkStmt (AbsT.CondElse _ s s' _) = checkStmt s >> checkStmt s'
	checkStmt (AbsT.While _ s _) = checkStmt s
	checkStmt _ = return ()

	checkBlock :: AbsT.Block -> DMonad ()
	checkBlock (AbsT.Block stmts) = do
		state <- get
		let vars = varsD state
		put (state {varsD = M.empty})
		mapM_ checkStmt stmts
		state2 <- get
		put (state2 {varsD = vars})

type VMonad = ReaderT VEnv (ExceptT E.ErrorMsg Identity)
data VEnv = VEnv {
		currentFnV :: CurrentFn
	}
runVMonad :: VMonad a -> VEnv -> Either E.ErrorMsg a
runVMonad m env = runIdentity $ runExceptT $ runReaderT m env

-- Sprawdź czy nie ma zmiennych typu 'void'.
checkVoidVariables :: AbsT.Program -> Either E.ErrorMsg AbsT.Program
checkVoidVariables p = runVMonad (checkProgram p) initEnv >> return p where
	initEnv :: VEnv
	initEnv = VEnv (AbsT.Void, (AbsT.Ident "_"), [], Nothing)

	checkProgram :: AbsT.Program -> VMonad ()
	checkProgram (AbsT.Program topDefs) = mapM_ checkTopDef topDefs

	checkTopDef :: AbsT.TopDef -> VMonad ()
	checkTopDef (AbsT.FnDef t n args b ln) = do
		let current = (t, n, args, ln)
		local (\env -> env {currentFnV = current}) $ mapM_ checkArg args
		local (\env -> env {currentFnV = current}) $ checkBlock b

	checkArg :: AbsT.Arg -> VMonad ()
	checkArg (AbsT.Arg t n) = if t /= AbsT.Void
		then return ()
		else do
			env <- ask
			let (fnT, fnN, _, fnLn) = currentFnV env
			throwError $ E.VoidVarErr fnT fnN fnLn n fnLn

	checkBlock :: AbsT.Block -> VMonad ()
	checkBlock (AbsT.Block stmts) = mapM_ checkStmt stmts

	itemName :: AbsT.Item -> AbsT.Ident
	itemName (AbsT.NoInit n) = n
	itemName (AbsT.Init n _) = n

	checkStmt :: AbsT.Stmt -> VMonad ()
	checkStmt (AbsT.BStmt b _) = checkBlock b
	checkStmt (AbsT.Decl t items ln) = if t /= AbsT.Void
		then return ()
		else do
			env <- ask
			let (fnT, fnN, _, fnLn) = currentFnV env
			let n = case items of {(n':_) -> itemName n'; _ -> AbsT.Ident "var_??"}
			throwError $ E.VoidVarErr fnT fnN fnLn n ln

	checkStmt (AbsT.Cond _ s _) = checkStmt s
	checkStmt (AbsT.CondElse _ s s' _) = checkStmt s >> checkStmt s'
	checkStmt (AbsT.While _ s _) = checkStmt s
	checkStmt _ = return ()

-- Oblicz wyrażenia ze zhardkodowanymi stałymi w kodzie programu.
calculateConstants :: AbsT.Program -> AbsT.Program
calculateConstants p = calcProgram p where
	calcProgram :: AbsT.Program -> AbsT.Program
	calcProgram (AbsT.Program topDefs) = AbsT.Program $ map calcTopDef topDefs

	calcTopDef :: AbsT.TopDef -> AbsT.TopDef
	calcTopDef (AbsT.FnDef t n args b ln) = AbsT.FnDef t n args (calcBlock b) ln

	calcBlock :: AbsT.Block -> AbsT.Block
	calcBlock (AbsT.Block stmts) = AbsT.Block $ map calcStmt stmts

	calcStmt :: AbsT.Stmt -> AbsT.Stmt
	calcStmt (AbsT.BStmt b ln) = AbsT.BStmt (calcBlock b) ln
	calcStmt (AbsT.Decl t items ln) = AbsT.Decl t (map calcItem items) ln
	calcStmt (AbsT.Ass n e ln) = AbsT.Ass n (calcExpr e) ln
	calcStmt (AbsT.Ret e ln) = AbsT.Ret (calcExpr e) ln
	calcStmt (AbsT.Cond e s ln) = case calcExpr e of
		(AbsT.ELitTrue _ _)  -> calcStmt s
		(AbsT.ELitFalse _ _) -> AbsT.Empty ln
		newE                 -> AbsT.Cond newE (calcStmt s) ln
	calcStmt (AbsT.CondElse e s s' ln) = case calcExpr e of
		(AbsT.ELitTrue _ _)  -> calcStmt s
		(AbsT.ELitFalse _ _) -> calcStmt s'
		newE                 -> AbsT.CondElse newE (calcStmt s) (calcStmt s') ln
	calcStmt (AbsT.While e s ln) = case calcExpr e of
		(AbsT.ELitFalse _ _) -> AbsT.Empty ln
		newE                 -> AbsT.While newE (calcStmt s) ln
	calcStmt (AbsT.SExp e ln) = AbsT.SExp (calcExpr e) ln
	calcStmt s = s

	calcItem :: AbsT.Item -> AbsT.Item
	calcItem i@(AbsT.NoInit _) = i
	calcItem (AbsT.Init n e) = AbsT.Init n (calcExpr e)

	calcExpr :: AbsT.Expr -> AbsT.Expr
	calcExpr (AbsT.EApp n exprs ln t) = AbsT.EApp n (map calcExpr exprs) ln t
	calcExpr (AbsT.Neg e ln t) = case calcExpr e of
		(AbsT.ELitInt x _ _) -> (AbsT.ELitInt ((-1) * x) ln t)
		newE                 -> (AbsT.Neg newE ln t)
	calcExpr (AbsT.Not e ln t) = case calcExpr e of
		(AbsT.ELitFalse _ _) -> AbsT.ELitTrue ln t
		(AbsT.ELitTrue _ _)  -> AbsT.ELitFalse ln t
		newE                 -> AbsT.Not newE ln t
	calcExpr (AbsT.EMul e op e' ln t) = case (calcExpr e, calcExpr e') of
		((AbsT.ELitInt x _ _), (AbsT.ELitInt x' _ _)) -> AbsT.ELitInt (intMul x op x') ln t
		(newE, newE')                                 -> AbsT.EMul newE op newE' ln t
	calcExpr (AbsT.EAdd e op e' ln t) = case (calcExpr e, calcExpr e') of
		((AbsT.ELitInt x _ _), (AbsT.ELitInt x' _ _)) -> AbsT.ELitInt (intAdd x op x') ln t
		((AbsT.EString x _ _), (AbsT.EString x' _ _)) -> AbsT.EString (strAdd x op x') ln t
		(newE, newE')                                 -> AbsT.EAdd newE op newE' ln t
	calcExpr (AbsT.ERel e op e' ln t) = case (calcExpr e, calcExpr e') of
			((AbsT.ELitInt x _ _), (AbsT.ELitInt x' _ _)) -> if intRel x op x'
				then AbsT.ELitTrue ln t
				else AbsT.ELitFalse ln t
			((AbsT.EString x _ _), (AbsT.EString x' _ _)) -> if strRel x op x'
				then AbsT.ELitTrue ln t
				else AbsT.ELitFalse ln t
			((AbsT.ELitTrue _ _), (AbsT.ELitTrue _ _)) -> if boolRel True op True
				then AbsT.ELitTrue ln t
				else AbsT.ELitFalse ln t
			((AbsT.ELitTrue _ _), (AbsT.ELitFalse _ _)) -> if boolRel True op False
				then AbsT.ELitTrue ln t
				else AbsT.ELitFalse ln t
			((AbsT.ELitFalse _ _), (AbsT.ELitTrue _ _)) -> if boolRel False op True
				then AbsT.ELitTrue ln t
				else AbsT.ELitFalse ln t
			((AbsT.ELitFalse _ _), (AbsT.ELitFalse _ _)) -> if boolRel False op False
				then AbsT.ELitTrue ln t
				else AbsT.ELitFalse ln t
			(newE, newE') -> AbsT.ERel newE op newE' ln t
	calcExpr (AbsT.EAnd e e' ln t) = case (calcExpr e, calcExpr e') of
		((AbsT.ELitTrue _ _) , (AbsT.ELitTrue _ _))  -> AbsT.ELitTrue ln t
		(_                   , (AbsT.ELitFalse _ _)) -> AbsT.ELitFalse ln t
		((AbsT.ELitFalse _ _), _)                    -> AbsT.ELitFalse ln t
		(newE, newE')                                -> AbsT.EAnd newE newE' ln t
	calcExpr (AbsT.EOr e e' ln t) = case (calcExpr e, calcExpr e') of
		((AbsT.ELitFalse _ _), (AbsT.ELitFalse _ _)) -> AbsT.ELitFalse ln t
		((AbsT.ELitTrue _ _) , _)                    -> AbsT.ELitTrue ln t
		(_                   , (AbsT.ELitTrue _ _))  -> AbsT.ELitTrue ln t
		(newE, newE')                                -> AbsT.EOr newE newE' ln t
	calcExpr e = e

	intMul :: Integer -> AbsT.MulOp -> Integer -> Integer
	intMul x AbsT.Times y = x * y
	intMul x AbsT.Div y   = x `div` y
	intMul x AbsT.Mod y   = x `mod` y

	intAdd :: Integer -> AbsT.AddOp -> Integer -> Integer
	intAdd x AbsT.Plus y  = x + y
	intAdd x AbsT.Minus y = x - y

	strAdd :: String -> AbsT.AddOp -> String -> String
	strAdd x AbsT.Plus y = x ++ y
	strAdd _ _ _         = undefined

	intRel :: Integer -> AbsT.RelOp -> Integer -> Bool
	intRel x AbsT.LTH y = x < y
	intRel x AbsT.LE y  = x <= y
	intRel x AbsT.GTH y = x > y
	intRel x AbsT.GE y  = x >= y
	intRel x AbsT.EQU y = x == y
	intRel x AbsT.NE y  = x /= y

	strRel :: String -> AbsT.RelOp -> String -> Bool
	strRel x AbsT.EQU y = x == y
	strRel x AbsT.NE y  = x /= y
	strRel _ _ _        = undefined

	boolRel :: Bool -> AbsT.RelOp -> Bool -> Bool
	boolRel x AbsT.EQU y = x == y
	boolRel x AbsT.NE y  = x /= y
	boolRel _ _ _        = undefined

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
