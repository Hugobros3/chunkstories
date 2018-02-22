#!/bin/sh
# Builds requirements for building chunkstories
# Assumes chunkstories-core and chunkstories-api project are in the parent folder
cd ../chunkstories-api
./gradlew install
cd ../chunkstories-core
./gradlew install
cd ../chunkstories
