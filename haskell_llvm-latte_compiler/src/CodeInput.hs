module CodeInput where

import System.FilePath(FilePath)

data CodeInput = INStdin | INFile FilePath
