#!/usr/bin/env sh
set -eu

echo "Generating official Apache Maven Wrapper..."
mvn wrapper:wrapper -Dmaven=3.9.16 -Dtype=bin

echo
echo "Wrapper generated."
echo "Next:"
echo "  ./mvnw clean verify"
