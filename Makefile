include BuildProperties.properties
build=$(build.major).$(build.minor).$(build.patch)
versionDir=Notion-$(build)
dir=zip-temp/$(versionDir)
DATE := $(shell /bin/date +%F-%T)

# The Log4j setup

dist: build
	./gradlew jar
	rm -rf zip-temp
	mkdir -p $(dir)
	cp -r build/libs/lib $(dir)
	cp notion $(dir)
	cp build/libs/Notion.jar $(dir)/Notion.jar
	cp Readme.md notion.example.yml $(dir)
	(cd zip-temp && zip -r $(versionDir).zip $(versionDir) && mv $(versionDir).zip ../)

build:
	(cd ui/ && make install)
	(cd Documentation && make install)

watch:
	(cd Documentation && while :; do make html ; sleep 5s; done)

install: dist
	${MAKE} sync
	${MAKE} restart

server:
	./gradlew jar

sync:
	rsync -arvz zip-temp/$(versionDir)/ qin@qia:/research/images/Notion/$(versionDir)-$(DATE)
	ssh qin@qia "cd /research/images/Notion ;ln -sfn $(versionDir)-$(DATE) Notion"

restart:
	ssh root@qia service notion restart

.PHONY: build dist install watch server sync restart
