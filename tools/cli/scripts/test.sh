#!/bin/sh

# Check code format and lint it
sh scripts/check/prettier.sh
sh scripts/check/eslint.sh
