#!/bin/bash
echo "Cleaning chunkstories-api"
rm ../chunkstories-api/src/io/xol/chunkstories/* -r
echo "Copying the new stuff"
cp src/io/xol/chunkstories/api ../chunkstories-api/src/io/xol/chunkstories/ -R