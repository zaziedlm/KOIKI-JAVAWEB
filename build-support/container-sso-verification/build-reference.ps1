$ErrorActionPreference = 'Stop'
python (Join-Path $PSScriptRoot 'build-reference.py') @args
exit $LASTEXITCODE
