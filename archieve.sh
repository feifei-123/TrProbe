#!/bin/sh

echo "编译开始-------"
./gradlew -p logClient clean  bintrayUpload
./gradlew -p logService  clean  bintrayUpload
./gradlew -p store  clean  bintrayUpload
./gradlew -p breakpad-build clean  bintrayUpload
./gradlew -p crashcatcher  clean  bintrayUpload
./gradlew -p blockcatcher  clean  bintrayUpload
./gradlew -p trprobe  clean  bintrayUpload
echo "编译完成-------"
