

#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
build: target/claim-0.0.1-SNAPSHOT.jar
	echo ${SOURCES}
	cd target && docker build -t ghcr.io/sunbird-rc/sunbird-rc-claim-ms ..

target/claim-0.0.1-SNAPSHOT.jar: $(SOURCES)
	echo $(SOURCES)
	../mvnw -DskipTests clean install