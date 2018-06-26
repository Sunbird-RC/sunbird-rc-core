#!/bin/sh
e () {
    echo $( echo ${1} | jq ".${2}" | sed 's/\"//g')
}
m=$(./metadata.sh)

org=$(e "${m}" "org")
name=$(e "${m}" "name")
version=$(e "${m}" "version")

docker build -f ./Dockerfile -t ${org}/${name}:${version}-bronze .
