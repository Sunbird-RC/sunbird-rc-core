#!/bin/bash -e
echo "Configuring dependencies before build"
cp java/registry/src/main/resources/schema-configuration-school-test.jsonld.sample java/registry/src/main/resources/schema-configuration-school-test.jsonld
cp java/schema-configuration/src/main/resources/validations.shex.sample java/schema-configuration/src/main/resources/validations.shex
cp java/registry/src/main/resources/application.yml.sample java/registry/src/main/resources/application.yml
cp java/registry/src/main/resources/validations_create.shex.sample java/registry/src/main/resources/validations_create.shex
cp java/registry/src/main/resources/validations_update.shex.sample java/registry/src/main/resources/validations_update.shex
cp java/registry/src/main/resources/schema-configuration.jsonld.sample java/registry/src/main/resources/schema-configuration.jsonld
cp java/registry/src/main/resources/frame.json.sample java/registry/src/main/resources/frame.json
cp java/registry/src/main/resources/audit_frame.json.sample java/registry/src/main/resources/audit_frame.json
echo "Configuration of dependencies completed"
