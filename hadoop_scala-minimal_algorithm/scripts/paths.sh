#!/bin/bash

TMP="/tmp"
PREFIX="$TMP/kotoko/pdd"

JAVADIR="$PREFIX/java-jdk-bin"
SPARKDIR="$PREFIX/spark-bin"
HADOOPDIR="$PREFIX/hadoop-bin"

PATH="$JAVADIR/bin:$HADOOPDIR/bin:$HADOOPDIR/sbin:$SPARKDIR/bin:$PATH"

HDFSDIR="/kotoko/duze2"
JAR="../jar/pdd-idea-compilation.jar"
