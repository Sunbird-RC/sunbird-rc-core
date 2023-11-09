#!/bin/bash

echo "Waiting service to launch on $1..."
i=0
while ! curl localhost:$1; do
  sleep 10
  ((i=i+1))
  if [[ $i -gt 60 ]]; then
    echo "Failed to get the service in sane state!"
    exit 1;
  fi
done

echo "$1 port accessible"
