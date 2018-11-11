#!/bin/bash

# rand from [1;254]
rand() {
	echo $RANDOM % 254 + 1 | bc
}

# Config
MCAST="239.73.$(rand).$(rand)"
P=23456
C=33456
p=1450
f=1048576
R=250
n="send.sh ($MCAST:$P)"


dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

# Number of arguments
if [ "$#" -lt 1 ] && [ "$#" -gt 2 ]; then
	echo "Expected 1 (or 2) argument(s): path_to_sikradio-sender [path_to_music]"
	exit 1
fi

# Avoid stupid problems with path to binary
RADIO="$1"
if [ ! "${RADIO:0:1}" = "/" ]; then
	RADIO="./$1"
fi

# Validate path to binary
if [ ! -f "$RADIO" ]; then
    echo "Wrong path to sikradio-sender: $RADIO"
    exit 1
fi

# Path to music
if [ "$#" -eq 2 ]; then
	MUSIC="$2"
else
	# Default music
	MUSIC="$dir/audio/Stairway_to_Heaven.mp3"
fi

# Validate path to music
if [ ! -f "$MUSIC" ]; then
	echo "Wrong path to music: $MUSIC"
	exit 1
fi

# Run radio sender
if [ "${MUSIC##*.}" = "mp3" ] || [ "${MUSIC##*.}" = "wav" ] || [ "${MUSIC##*.}" = "MP3" ] || [ "${MUSIC##*.}" = "WAV" ]; then
	(>&2 echo "Piping music using 'sox' command...")
	sleep 0.7
	while true; do sox -q "$MUSIC" -r 44100 -b 16 -e signed-integer -c 2 -t raw -; done | pv -q -L $((44100*4))
else
	(>&2 echo "Piping music as raw data...")
	sleep 0.7
	while true; do pv -q -L $((44100*4)) "$MUSIC"; done
fi | "$RADIO" -a "$MCAST" -P "$P" -C "$C" -p "$p" -f "$f" -R "$R" -n "$n"
