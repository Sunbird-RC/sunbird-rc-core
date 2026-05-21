#!/bin/bash

echo "Waiting service to launch on $1..."
i=0
while ! curl -s --max-time 5 localhost:"$1"; do
  sleep 10
  i=$((i+1))
  if [ "$i" -gt 90 ]; then
    echo "Failed to get the service on port $1 in sane state after $((i * 10))s!"
    exit 1;
  fi
done

echo "$1 port accessible"
