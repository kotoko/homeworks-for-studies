#!/bin/bash

# Config
d="255.255.255.255"
C=33456
U=13456
b=524288
R=250
n=""
PLAY_BUFF=65536


name() {
	if [ ! "$n" = "" ]; then
		echo "-n" "$n"
	fi
}

dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

# Number of arguments
if [ "$#" -ne 1 ]; then
	echo "Expected 1 argument: path to sikradio-receiver"
	exit 1
fi

# Avoid stupid problems with path to binary
RADIO="$1"
if [ ! "${RADIO:0:1}" = "/" ]; then
	RADIO="./$1"
fi

# Validate path to binary
if [ ! -f "$RADIO" ]; then
	echo "Wrong path to sikradio-receiver: $RADIO"
	exit 1
fi

# Run radio receiver
"$RADIO" -d "$d" -C "$C" -U "$U" -b "$b" -R "$R" $(name) | play -t raw -c 2 -r 44100 -b 16 -e signed-integer --buffer "$PLAY_BUFF" -q -
