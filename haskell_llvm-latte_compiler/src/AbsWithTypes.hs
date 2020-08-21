module AbsWithTypes where

import LineNr

newtype Ident = Ident String
	deriving (Eq, Ord, Show, Read)

data Program = Program [TopDef]
	deriving (Eq, Ord, Show, Read)

data TopDef = FnDef Type Ident [Arg] Block LineNr
	deriving (Eq, Ord, Show, Read)

data Arg = Arg Type Ident
	deriving (Eq, Ord, Show, Read)

data Block = Block [Stmt]
	deriving (Eq, Ord, Show, Read)

data Stmt
	= Empty LineNr
	| BStmt Block LineNr
	| Decl Type [Item] LineNr
	| Ass Ident Expr LineNr
	| Incr Ident LineNr
	| Decr Ident LineNr
	| Ret Expr LineNr
	| VRet LineNr
	| Cond Expr Stmt LineNr
	| CondElse Expr Stmt Stmt LineNr
	| While Expr Stmt LineNr
	| SExp Expr LineNr
	deriving (Eq, Ord, Show, Read)

data Item = NoInit Ident | Init Ident Expr
	deriving (Eq, Ord, Show, Read)

data Type
	= Int | Str | Bool | Void | Fun Type [Type]
	deriving (Eq, Ord, Show, Read)

data Expr
	= EVar Ident LineNr Type
	| ELitInt Integer LineNr Type
	| ELitTrue LineNr Type
	| ELitFalse LineNr Type
	| EApp Ident [Expr] LineNr Type
	| EString String LineNr Type
	| Neg Expr LineNr Type
	| Not Expr LineNr Type
	| EMul Expr MulOp Expr LineNr Type
	| EAdd Expr AddOp Expr LineNr Type
	| ERel Expr RelOp Expr LineNr Type
	| EAnd Expr Expr LineNr Type
	| EOr Expr Expr LineNr Type
	deriving (Eq, Ord, Show, Read)

data AddOp = Plus | Minus
	deriving (Eq, Ord, Show, Read)

data MulOp = Times | Div | Mod
	deriving (Eq, Ord, Show, Read)

data RelOp = LTH | LE | GTH | GE | EQU | NE
	deriving (Eq, Ord, Show, Read)
