#!/bin/sh

echo android project for ant
android update lib-project -p .

echo update project appcompat
android update lib-project -p ./library_projects/appcompat

echo update project mediarouter
android update lib-project -p ./library_projects/mediarouter

echo update project google-play-services_lib
android update lib-project -p ./library_projects/google-play-services_lib

echo build jar file
ant -buildfile build.xml clean release

echo rename jar file with version string
mv bin/proguard/obfuscated.jar bin/proguard/$1_$2.jar

echo generate Javadoc
ant -buildfile javadoc.xml javadoc

