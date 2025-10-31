#!/bin/bash

echo "Building L2Tool..."
echo

./gradlew build

if [ $? -eq 0 ]; then
    echo
    echo "Build successful!"
    echo "JAR file location: build/libs/l2tool.jar"
else
    echo
    echo "Build failed!"
    exit 1
fi

