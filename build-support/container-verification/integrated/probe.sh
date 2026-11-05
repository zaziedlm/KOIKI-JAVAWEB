#!/bin/bash
# Runtime base contract: bash. No curl dependency, credentials, or Session creation.
set -eu
exec 3<>/dev/tcp/127.0.0.1/8080
printf 'GET /actuator/health/readiness HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
IFS= read -r -t 3 status <&3
[[ "$status" == HTTP/1.1\ 200* ]]
