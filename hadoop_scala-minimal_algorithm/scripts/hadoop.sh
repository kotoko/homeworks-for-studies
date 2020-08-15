#!/bin/bash
set -e

DIR_BACKUP="$(pwd)"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
source 'paths.sh'

cd "$DIR_BACKUP"

"$HADOOPDIR/bin/hadoop" "$@"
