#!/bin/sh
# Builds and 1 the DistributedTextEditor.
# Can be passed number of 1 you want running, as argument.
# Variables
BASE=$(pwd)
SOURCE="$BASE/src"
BUILD="$BASE/build"
MAIN=DistributedTextEditor

# Create build-directory if none exist
mkdir -p $BUILD

# Go to src/ and compile
cd $SOURCE
javac -nowarn -d $BUILD "$MAIN.java"

# Go to build/
cd $BUILD

# Run number of 1, passed as argument. Default is 1.
if [ -z "$1" ]; then
  java $MAIN &
else
  i=1
  while [ "$i" -le "$1" ]; do
    java $MAIN &
    i=$(($i + 1))
  done
fi