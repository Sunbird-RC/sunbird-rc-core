rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
build: target/consent-0.0.1-SNAPSHOT.jar
	echo ${SOURCES}
	cd target && docker build -t dockerhub/sunbird-rc-consent-ms ..

target/claim-0.0.1-SNAPSHOT.jar: $(SOURCES)
	echo $(SOURCES)
	../mvnw -DskipTests clean install