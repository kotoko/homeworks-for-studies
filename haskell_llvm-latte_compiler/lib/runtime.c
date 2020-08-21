// 1) clang -O3 -S -emit-llvm -std=c17 runtime.c
// 2) Zmiana parametru w printBoolean z i32 na i1
// 3) Zmiana wyniku w strEqual z i32 na i1

#define _GNU_SOURCE

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define bool int

void printInt(int n) {
	printf("%d\n", n);
}

void printString(const char *s) {
	printf("%s\n", s);
}

void printBoolean(bool x) {
	if (x == 1) {
		printString("true");
	} else {
		printString("false");
	}
}

void error() {
	printString("runtime error");
	exit(EXIT_FAILURE);
}

int readInt() {
	char *line = NULL;
	size_t size = 0;
	if (getline(&line, &size, stdin) == -1) {
		printString("error during readInt()");
		error();
	}

	int n;
	if (sscanf(line, "%d", &n) != 1) {
		printString("error during readInt()");
		error();
	}

	free(line);

	return n;
}

char* readString() {
	char *line = NULL;
	size_t size = 0;
	if (getline(&line, &size, stdin) == -1) {
		printString("error during readString()");
		error();
	}

	// Usuwanie znaku nowej lini. Eh...
	size = strlen(line);
	if (size >= 1 && line[size - 1] == '\n') {
		line[size - 1] = '\0';
	}

	return line;
}

bool strEqual(char* s1, char* s2) {
	return strcmp(s1, s2) == 0;
}

char* strConcat(char* s1, char* s2) {
	char* t = malloc(strlen(s1) + strlen(s2) + 1);
	if (t == NULL) {
		printString("malloc() returned NULL");
		error();
	}
	return strcat(strcpy(t, s1), s2);
}
