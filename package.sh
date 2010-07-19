#!/bin/bash

# Build
sbt clean update compile copy-resources package

DIST="target/dist"

# Package structure
rm -rf $DIST
mkdir -p $DIST/bin $DIST/lib $DIST/conf
cp lib_managed/scala_2.7.7/compile/* $DIST/lib
cp target/scala_2.7.7/notifier_2.7.7-1.0.jar $DIST/lib
cp target/scala_2.7.7/resources/* $DIST/conf

cp src/main/bash/* $DIST/bin
chmod +x $DIST/bin/*
