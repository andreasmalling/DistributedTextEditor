#!/bin/sh
# Builds and runs the DistributedTextEditor.
# Can be passed number of instances you want running, as argument.

# Variables
CWD=$(pwd)
SOURCE="$CWD/src"
BUILD="$CWD/build"
MAIN=DistributedTextEditor

# Create build-directory if none exist
mkdir -p $BUILD

# Go to src/ and compile
cd $SOURCE
javac -d $BUILD "$MAIN.java"

# Go to build/
cd $BUILD

# Run number of instances, passed as argument. Default is 1.
for i in $(eval echo {1..$1})
do
   java $MAIN &
done