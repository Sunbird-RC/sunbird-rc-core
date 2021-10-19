#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export PRETTIER="prettier"
export PRETTIER_OPTIONS="--ignore-path .gitignore --config config/prettier.json --write"
export FILES="**/*.{md,yaml,ts}"

# Format source and documentation files
$PNPX $PRETTIER $PRETTIER_OPTIONS $FILES
