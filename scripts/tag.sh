#!/bin/bash

# Get the current version
VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v INFO`

java -jar target/bmmm-${VERSION}.jar $@