module Compiler(compileCode) where

import qualified Backend as Backend
import CodeInput(CodeInput(..))
import Control.Monad.Except(throwError)
import qualified ErrorMsg as E
import qualified Frontend as Frontend
import qualified ParserGlue as Parser


-- Skompiluj kod lub zwróć informację o błędzie.
compileCode :: CodeInput -> String -> Either String String
compileCode input code = case compileCode' input code of
	Left err   -> throwError $ show err
	Right code -> return code

compileCode' :: CodeInput -> String -> Either E.ErrorMsg String
compileCode' input code = do
	tree1 <- Parser.parse code
	tree2 <- Frontend.compileCode tree1
	out <- Backend.compileCode input tree2
	return out
