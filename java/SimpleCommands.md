### Use podman compose to start test environment
```shell
podman compose -f docker-compose-v1.yml --env-file test_environments/test_with_distributedDefManager_nativeSearch.env up -d db keycloak

```

### Build registry image using podman
```shell
podman build -t ghcr.io/sunbird-rc/sunbird-rc-core .
```

### Enable docker host access via podman
```shell
export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
```

### Copy log files from container to host
```shell
podman cp sunbird-rc-core-registry-1:/app/logs/app.log app.log 

``` 