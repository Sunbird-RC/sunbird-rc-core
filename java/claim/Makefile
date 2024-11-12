

#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
build: target/claim-2.0.4-SNAPSHOT.jar
	echo ${SOURCES}
	cd target && docker build -t ghcr.io/sunbird-rc/sunbird-rc-claim-ms ..

target/claim-2.0.4-SNAPSHOT.jar: $(SOURCES)
	echo $(SOURCES)
	../mvnw -DskipTests clean install