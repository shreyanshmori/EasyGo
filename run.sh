#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

JAVA_SOURCES=$(find . -name "*.java" \
  ! -path "./build/*" \
  ! -path "./target/*" \
  ! -path "./.gradle/*" \
  ! -path "./gradle/*")

javac -cp ".:lib/*" $JAVA_SOURCES
java -cp ".:lib/*" Main
