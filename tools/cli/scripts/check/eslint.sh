#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export ESLINT="eslint"
export ESLINT_OPTIONS="--ignore-path .gitignore --config config/eslint.json"
export FILES="source/**/*.ts"

# Lint the code
$PNPX $ESLINT $ESLINT_OPTIONS $FILES
