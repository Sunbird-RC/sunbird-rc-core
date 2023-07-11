IMAGE:=dockerhub/sunbird-rc-credentials-service

.PHONY: docker publish test

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
test:
#	Resetting vault of identity-service before running the tests
	make -C ../identity-service stop
	make -C ../credential-schema stop
	make -C ../identity-service vault-reset
# 	Creating an external docker network to connnect services in different compose
	@docker network create rcw-test || echo ""
#	Starting dependent services 
	make -C ../identity-service compose-init
	make -C ../credential-schema compose-init
	@docker-compose -f docker-compose-test.yml down
	@docker-compose  -f docker-compose-test.yml up --build --abort-on-container-exit

#	Stopping all dependent services
	make -C ../identity-service stop
	make -C ../identity-service vault-reset
	make -C ../credential-schema stop