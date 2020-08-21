module ParserGlue(parse) where

import AbsLatte
import Control.Monad.Except(throwError)
import ErrM(Err(Ok, Bad))
import ErrorMsg(ErrorMsg(ParseErr))
import LineNr(LineNr)
import ParLatte(myLexer, pProgram)

-- Sparsuj kod programu do drzewa abstrakcyjnego.
parse :: String -> Either ErrorMsg (Program LineNr)
parse codeText =
	let
		myLLexer = myLexer
		tokens = myLLexer codeText
	in case pProgram tokens of
		Bad s -> throwError $ ParseErr tokens s
		Ok tree -> return tree
