#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export NP="np"

# Build the CLI
sh scripts/build.sh
# Create a release
$PNPX $NP
