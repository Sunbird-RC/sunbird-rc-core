#!/bin/bash
echo "Configuring dependencies before build"
#cp java/registry/src/main/resources/config-test.properties.sample java/registry/src/main/resources/config-test.properties
cp java/registry/src/main/resources/application.yml.sample java/registry/src/main/resources/application.yml
cp java/registry/src/main/resources/schema-configuration-school-test.jsonld.sample java/registry/src/main/resources/schema-configuration-school-test.jsonld
cp java/schema-configuration/src/main/resources/validations.shex.sample java/schema-configuration/src/main/resources/validations.shex
cp java/registry/src/main/resources/validations_create.shex.sample java/registry/src/main/resources/validations_create.shex
cp java/registry/src/main/resources/validations_update.shex.sample java/registry/src/main/resources/validations_update.shex
echo "Configuration of dependencies completed"
