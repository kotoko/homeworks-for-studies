module ErrorMsg(ErrorMsg(..)) where

import qualified AbsWithTypes as T
import LexLatte(Token)
import LineNr
import PrintAbsT

data ErrorMsg
	= MissingMainErr
	| ParseErr [Token] String
	| DuplicatedFunctionErr T.Type T.Ident LineNr T.Type T.Ident LineNr  -- fn1 fn2
	| DuplicatedVarErr T.Type T.Ident LineNr T.Type T.Ident LineNr T.Type T.Ident LineNr  -- fn var1 var2
	| MissingVarErr T.Type T.Ident LineNr T.Ident LineNr  -- fn var1 (zmienna bez deklaracji)
	| TypeMismatchErr T.Type T.Type T.Expr LineNr  -- got_type expected_type expr
	| TypeMismatchReturnErr T.Type T.Type T.Ident LineNr T.Stmt LineNr  -- got_type fn stmt
	| MissingFnErr T.Ident LineNr T.Type T.Ident LineNr  -- fn_name fn
	| WrongArgsApplyErr Integer Integer T.Ident LineNr T.Type T.Ident LineNr  -- got_number expected_number fn_name fn
	| MissingReturnErr T.Type T.Ident LineNr  -- fn
	| VoidVarErr T.Type T.Ident LineNr T.Ident LineNr  -- fn var

indent :: String
indent = "  "

instance Show ErrorMsg where
	show MissingMainErr =
		"Missing entry point!" ++ "\n"
		++ indent ++ "Expected:" ++ "\n"
		++ indent ++ "int main() {...}"
	show (ParseErr tokens msg) =
		"Parse Failed...\n"
		++ "Tokens:\n" ++ show tokens ++ "\n"
		++ msg
	show (DuplicatedFunctionErr t1 n1 ln1 t2 n2 ln2) = let x = calcPrefix [ln1, ln2] in
		"Duplicated function definition!\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln1 ++ printType t1 ++ " " ++ printIdent n1 ++ "(...)" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln2 ++ printType t2 ++ " " ++ printIdent n2 ++ "(...)" ++ "\n"
		++ indent ++ pref1 x
	show (DuplicatedVarErr fnT fnN fnLn t1 n1 ln1 t2 n2 ln2) = let x = calcPrefix [ln1, ln2] in
		"Duplicated variable declaration!\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln1 ++ indent ++ printType t1 ++ " " ++ printIdent n1 ++ ";" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln2 ++ indent ++ printType t2 ++ " " ++ printIdent n2 ++ ";" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"
	show (MissingVarErr fnT fnN fnLn n ln) = let x = calcPrefix [ln, fnLn] in
		"Using variable without declaration!" ++ "\n"
		++ indent ++ "Undeclared variable: " ++ printIdent n ++ "\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln ++ indent ++ printIdent n ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"
	show (TypeMismatchErr t1 t2 e ln) = let x = calcPrefix [ln] in
		"Type mismatch in expression!" ++ "\n"
		++ indent ++ "Got:      " ++ printType t1 ++ "\n"
		++ indent ++ "Expected: " ++ printType t2 ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln ++ printExpr e ++ "\n"
		++ indent ++ pref1 x
	show (TypeMismatchReturnErr t fnT fnN fnLn s ln) = let x = calcPrefix [fnLn, ln] in
		"Type mismatch in 'return' statement!" ++ "\n"
		++ indent ++ "Got:      " ++ printType t ++ "\n"
		++ indent ++ "Expected: " ++ printType fnT ++ "\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln ++ indent ++ printStmt 0 s ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"
	show (MissingFnErr n ln fnT fnN fnLn) = let x = calcPrefix [ln, fnLn] in
		"Can not find function!" ++ "\n"
		++ indent ++ "Missing: " ++ printIdent n ++ "\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln ++ indent ++ printIdent n ++ "(...);" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"
	show (WrongArgsApplyErr nr1 nr2 n ln fnT fnN fnLn) = let x = calcPrefix [ln, fnLn] in
		"Wrong number of arguments in function call to " ++ printIdent n ++ "!" ++ "\n"
		++ indent ++ "Got:      " ++ show nr1 ++ "\n"
		++ indent ++ "Expected: " ++ show nr2 ++ "\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln ++ indent ++ printIdent n ++ "(...);" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"
	show (MissingReturnErr fnT fnN fnLn) = let x = calcPrefix [fnLn, Nothing] in
		"Missing return statement inside the function!" ++ "\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x Nothing ++ indent ++ "return ...;" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"
	show (VoidVarErr fnT fnN fnLn n ln) = let x = calcPrefix [fnLn, ln] in
		"Type 'void' is illegal in variable declaration!" ++ "\n"
		++ indent ++ pref2 x fnLn ++ printType fnT ++ " " ++ printIdent fnN ++ "(...) {" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref2 x ln ++ indent ++ printType T.Void ++ " " ++ printIdent n ++ ";" ++ "\n"
		++ indent ++ pref1 x ++ "\n"
		++ indent ++ pref1 x ++ "}"

-- Pusty prefix.
pref1 :: Integer -> String
pref1 x = if x > 0 then " " ++ pref1 (x-1) else "| "

-- Prefix z numerem linii.
pref2 :: Integer -> LineNr -> String
pref2 x ln = printLineNr ln ++ pref1 (x - (digitCountLineNr ln))

-- Oblicz wcięcie dla prefiksu.
calcPrefix :: [LineNr] -> Integer
calcPrefix l = 1 + (foldr max 0 $ map digitCountLineNr l)

printLineNr :: LineNr -> String
printLineNr Nothing = "??"
printLineNr (Just (row, _)) = show row

digitCountLineNr :: LineNr -> Integer
digitCountLineNr Nothing = 2  -- "??"
digitCountLineNr (Just (ln, _)) = fromIntegral $ digitCount ln

-- https://stackoverflow.com/a/25005986
digitCount :: Int -> Int
digitCount = go 1 . abs
	where
		go ds n = if n >= 10
			then go (ds + 1) (n `div` 10)
			else ds
