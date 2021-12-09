#!/bin/bash -e
echo "Configuring dependencies before build"
cp java/registry/src/main/resources/frame.json.sample java/registry/src/main/resources/frame.json
#cp java/registry/src/main/resources/audit_frame.json.sample java/registry/src/main/resources/audit_frame.json
cp  java/claim/src/main/resources/application.properties.sample   java/claim/src/main/resources/application.properties
echo "Configuration of dependencies completed"
