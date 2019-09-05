#!/bin/sh
echo "Kotlin LoC:"
( find ./ -name '*.kt' -print0 | xargs -0 cat ) | wc -l
echo "Java LoC:"
( find ./ -name '*.java' -print0 | xargs -0 cat ) | wc -l
