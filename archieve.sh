#!/bin/sh

echo "上传maven开始-------"
./gradlew -p logClient  clean build uploadArchives
./gradlew -p logService  clean build uploadArchives
./gradlew -p store  clean build uploadArchives
./gradlew -p breakpad-build clean build uploadArchives
./gradlew -p crashcatcher  clean build uploadArchives
./gradlew -p blockcatcher  clean build uploadArchives
./gradlew -p trprobe  clean build uploadArchives
echo "上传maven完成-------"