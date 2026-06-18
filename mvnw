#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Wrapper script for Maven
# See https://maven.apache.org/wrapper/

set -e

basedir="$(cd "$(dirname "$0")" && pwd)"

if [ -z "$MAVEN_WRAPPER_JAR" ]; then
  MAVEN_WRAPPER_JAR="$basedir/.mvn/wrapper/maven-wrapper.jar"
fi

if [ -z "$MAVEN_WRAPPER_PROPERTIES" ]; then
  MAVEN_WRAPPER_PROPERTIES="$basedir/.mvn/wrapper/maven-wrapper.properties"
fi

log_and_echo() {
  echo "$*" >&2
}

die() {
  log_and_echo "ERROR: $*"
  exit 1
}

[ -f "$MAVEN_WRAPPER_JAR" ] || die "maven-wrapper.jar not found in .mvn/wrapper"
[ -f "$MAVEN_WRAPPER_PROPERTIES" ] || die "maven-wrapper.properties not found in .mvn/wrapper"

# Extract Maven version and URL from properties
distributionUrl=$(grep -E "^distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2)
[ -z "$distributionUrl" ] && die "distributionUrl not found in maven-wrapper.properties"

exec java -cp "$MAVEN_WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
