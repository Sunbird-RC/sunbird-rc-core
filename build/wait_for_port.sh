#!/bin/bash

echo "Waiting jenkins to launch on $1..."

while ! curl localhost:$1; do
  sleep 1
done

echo "$1 port accessible"
