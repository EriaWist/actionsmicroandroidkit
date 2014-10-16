#!/bin/sh

echo android project for ant
android update lib-project -p .

# echo update project appcompat
# android update lib-project -p ./library_projects/appcompat

# echo update project mediarouter
# android update lib-project -p ./library_projects/mediarouter

# echo update project google-play-services_lib
# android update lib-project -p ./library_projects/google-play-services_lib

echo build jar file
ant -buildfile build.xml clean release

echo rename jar file with version string
if [ ! -d "sdk_template/ezcast-sdk_lib/libs" ]; then
	mkdir sdk_template/ezcast-sdk_lib/libs
fi
mv bin/proguard/obfuscated.jar sdk_template/ezcast-sdk_lib/libs/$1_$2.jar

# move to Jenkins SDK job
# export SDK_VERSION_STRING=${BUILD_VERSION_STRING}
# export BUILD_YEAR=`date +'%Y'`
# export BUILD_DATE="`date `"

echo generate Javadoc
ant -buildfile javadoc.xml javadoc

