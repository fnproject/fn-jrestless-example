#!/bin/bash

release_version=$(cat release.version)
if [[ $release_version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
   echo "Building version $release_version"
else
   echo Invalid version $release_version
   exit 1
fi

mvn versions:set -D newVersion=${release_version}  -D generateBackupPoms=false versions:update-child-modules


