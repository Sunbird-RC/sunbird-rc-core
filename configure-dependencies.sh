#!/bin/bash
echo "Configuring dependencies before build"
#cp java/registry/src/main/resources/config-test.properties.sample java/registry/src/main/resources/config-test.properties
cp java/registry/src/main/resources/application.yml.sample java/registry/src/main/resources/application.yml
cp java/registry/src/main/resources/schema-configuration-school-test.jsonld.sample java/registry/src/main/resources/schema-configuration-school-test.jsonld
cp java/schema-configuration/src/main/resources/validations.shex.sample java/schema-configuration/src/main/resources/validations.shex
echo "Configuration of dependencies completed"
