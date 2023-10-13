#!/bin/sh

cd target && jar -xvf ../java/registry/target/registry.jar && cd -
docker build -t ghcr.io/sunbird-rc/sunbird-rc .

e () {
    echo $( echo ${1} | jq ".${2}" | sed 's/\"//g')
}
m=$(./target/metadata.sh)

org=$(e "${m}" "org")
hubuser=$(e "${m}" "hubuser")
name=$(e "${m}" "name")
version=$(e "${m}" "version")

artifactLabel=${ARTIFACT_LABEL:-bronze}

docker login -u "${hubuser}" -p`cat /home/ops/vault_pass`
docker push ${org}/${name}:${version}-${artifactLabel}
docker logout
