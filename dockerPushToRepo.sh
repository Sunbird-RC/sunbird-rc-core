#!/bin/sh
e () {
    echo $( echo ${1} | jq ".${2}" | sed 's/\"//g')
}
m=$(./metadata.sh)

org=$(e "${m}" "org")
hubuser=$(e "${m}" "hubuser")
name=$(e "${m}" "name")
version=$(e "${m}" "version")

artifactLabel=${ARTIFACT_LABEL:-bronze}

docker login -u "${hubuser}" -p`cat /home/opensaber/vault_pass`
docker push ${org}/${name}:${version}-${artifactLabel}
docker logout
