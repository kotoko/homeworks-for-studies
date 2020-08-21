module PrintAbsT(
printIdent,
printProgram,
printTopDef,
printArg,
printBlock,
printStmt,
printItem,
printType,
printExpr,
printAddOp,
printMulOp,
printRelOp
) where

import qualified AbsWithTypes as T
import Data.List(intercalate)

indent :: Integer -> String
indent x = if x > 0 then "  " ++ (indent (x-1)) else ""

printIdent :: T.Ident -> String
printIdent (T.Ident n) = n

printProgram :: T.Program -> String
printProgram (T.Program topDefs) = unlines $ map printTopDef topDefs

printTopDef :: T.TopDef -> String
printTopDef (T.FnDef t n args b _) = let x = 0 in
	printType t ++ " " ++ printIdent n ++ "(" ++ (intercalate ", " $ map printArg args) ++ ")\n" ++ printBlock x b

printArg :: T.Arg -> String
printArg (T.Arg t n) = printType t ++ " " ++ printIdent n

printBlock :: Integer -> T.Block -> String
printBlock x (T.Block stmts) = indent x ++ "{\n"
	++ (unlines $ map (printStmt (x+1)) stmts)
	++ indent x ++ "}"

printStmt :: Integer -> T.Stmt -> String
printStmt x (T.Empty _) = indent x ++ ";"
printStmt x (T.BStmt b _) = printBlock x b
printStmt x (T.Decl t items _) = indent x ++ printType t ++ " " ++ (intercalate ", " $ map printItem items) ++ ";"
printStmt x (T.Ass n e _) = indent x ++ printIdent n ++ " = " ++ printExpr e ++ ";"
printStmt x (T.Incr n _) = indent x ++ printIdent n ++ "++;"
printStmt x (T.Decr n _) = indent x ++ printIdent n ++ "--;"
printStmt x (T.Ret e _) = indent x ++ "return " ++ printExpr e ++ ";"
printStmt x (T.VRet _) = indent x ++ "return;"
printStmt x (T.Cond e s _) = indent x ++ "if (" ++ printExpr e ++ ")\n"
	++ printStmt (x+1) s
printStmt x (T.CondElse e s1 s2 _) = indent x ++ "if (" ++ printExpr e ++ ")\n"
	++ printStmt (x+1) s1 ++ "\n"
	++ indent x ++ "else\n"
	++ printStmt (x+1) s2
printStmt x (T.While e s _) = indent x ++ "while (" ++ printExpr e ++ ")\n"
	++ printStmt x s
printStmt x (T.SExp e _) = indent x ++ printExpr e ++ ";"

printItem :: T.Item -> String
printItem (T.NoInit n) = printIdent n
printItem (T.Init n e) = printIdent n ++ " = " ++ printExpr e

printType :: T.Type -> String
printType T.Int = "int"
printType T.Str = "string"
printType T.Bool = "boolean"
printType T.Void = "void"
printType (T.Fun t ts) = "FUN (" ++ (intercalate ", " $ map printType ts) ++ ") ->" ++ printType t

printExpr :: T.Expr -> String
printExpr (T.EVar n _ _) = printIdent n
printExpr (T.ELitInt nr _ _) = show nr
printExpr (T.ELitTrue _ _) = "true"
printExpr (T.ELitFalse _ _) = "false"
printExpr (T.EApp n exprs _ _) = printIdent n ++ "(" ++ (intercalate ", " $ map printExpr exprs) ++ ")"
printExpr (T.EString str _ _) = show str
printExpr (T.Neg e _ _) = "-(" ++ printExpr e ++ ")"
printExpr (T.Not e _ _) = "!(" ++ printExpr e ++ ")"
printExpr (T.EMul e1 op e2 _ _) = "(" ++ printExpr e1 ++ ") " ++ printMulOp op ++ " (" ++ printExpr e2 ++ ")"
printExpr (T.EAdd e1 op e2 _ _) = "(" ++ printExpr e1 ++ ") " ++ printAddOp op ++ " (" ++ printExpr e2 ++ ")"
printExpr (T.ERel e1 op e2 _ _) = "(" ++ printExpr e1 ++ ") " ++ printRelOp op ++ " (" ++ printExpr e2 ++ ")"
printExpr (T.EAnd e1 e2 _ _) = "(" ++ printExpr e1 ++ ") && (" ++ printExpr e2 ++ ")"
printExpr (T.EOr e1 e2 _ _) = "(" ++ printExpr e1 ++ ") || (" ++ printExpr e2 ++ ")"

printAddOp :: T.AddOp -> String
printAddOp T.Plus = "+"
printAddOp T.Minus = "-"

printMulOp :: T.MulOp -> String
printMulOp T.Times = "*"
printMulOp T.Div = "/"
printMulOp T.Mod = "%"

printRelOp :: T.RelOp -> String
printRelOp T.LTH = "<"
printRelOp T.LE = "<="
printRelOp T.GTH = ">"
printRelOp T.GE = ">="
printRelOp T.EQU = "=="
printRelOp T.NE = "!="
