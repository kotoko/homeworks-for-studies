# Sequence
This is small program for learning basics of assembler.

## Description
Program takes 1 parameter - path to file. Opens a file and reads byte by byte. It treats a byte as a number from 0 to 255. We check if file has a valid pattern. Every 0 is a separator. The pattern is correct if every group of numbers (group = numbers between zeros) is the same in the sense that we can permutate one group and then we get another group. There is also requirement that every number in each group may occut at most once. Exit code is the result of validation.

## Build
Requirements:

* linux 64-bit
* nasm (tested on =2.13.01)

Build:
```
nasm -f elf64 -o sequence.o sequence.asm
ld --fatal-warnings -o sequence sequence.o
```

## Run
```
./sequence ./path/to/file_with_pattern_to_check
echo $?
```

Output:

* 0 - file has correct pattern
* 1 - file has invalid pattern
