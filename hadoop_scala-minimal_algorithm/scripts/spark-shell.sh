#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
source 'paths.sh'

"$SPARKDIR/bin/spark-shell" "$@"
