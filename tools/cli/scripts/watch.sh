#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export TSC="tsc"
export TSC_OPTIONS="--project config/typescript.json --watch"

# Keep build the CLI when a source file changes
$PNPX $TSC $TSC_OPTIONS
