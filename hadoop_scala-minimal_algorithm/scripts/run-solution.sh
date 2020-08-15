#!/bin/bash
set -e

DIR_BACKUP="$(pwd)"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
source 'paths.sh'

if [ "$#" -ne 2 ]; then
	echo "Expected 2 parameters: dimensions in.csv"
fi

DIMENSIONS="$1"
IN_PATH="$DIR_BACKUP/$2"
IN=$(basename "$IN_PATH")
OUT="out.csv"

if [ ! -d "$TMP" ]; then
	echo "Missing '$TMP'!"
	exit 1
fi
mkdir -p "$TMP/spark-job-answer-$(whoami)/"
TEMPDIR=$(mktemp -d "$TMP/spark-job-answer-$(whoami)/XXXXXX")

./hadoop.sh fs -mkdir -p "$HDFSDIR"
./hadoop.sh fs -rm -f -r "$HDFSDIR/$OUT"
./hadoop.sh fs -put -f "$IN_PATH" "$HDFSDIR"
./spark-submit.sh "$JAR" "$DIMENSIONS" "$HDFSDIR/$IN" "$HDFSDIR/$OUT"
./hadoop.sh fs -copyToLocal "$HDFSDIR/$OUT" "$TEMPDIR/$OUT"

echo
echo "Answer:"
for filename in "$TEMPDIR/$OUT/"part*; do
	cat "$filename"
done
echo
echo "Answer copied to '$TEMPDIR/'"
