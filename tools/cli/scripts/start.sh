#!/bin/sh

# Configuration
export NODE="$(command -v node)"
export NODE_OPTIONS="--no-warnings --loader ts-node/esm --es-module-specifier-resolution node"
export TS_NODE_PROJECT="config/typescript.json"

# Run the CLI
$NODE $NODE_OPTIONS source/main.ts $@
