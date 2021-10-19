#!/bin/sh

# Configuration
export PNPX="$(command -v pnpx)"
export ESLINT="eslint"
export ESLINT_OPTIONS="--ignore-path .gitignore --config config/eslint.json --fix"
export FILES="source/**/*.ts"

# Fix linting errors in the code
$PNPX $ESLINT $ESLINT_OPTIONS $FILES
