#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export TSC="tsc"
export TSC_OPTIONS="--project config/typescript.json"

# Build the CLI
$PNPX $TSC $TSC_OPTIONS
