#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export PRETTIER="prettier"
export PRETTIER_OPTIONS="--ignore-path .gitignore --config config/prettier.json --check"
export FILES="**/*.{md,yaml,ts}"

# Check code formatting
$PNPX $PRETTIER $PRETTIER_OPTIONS $FILES
