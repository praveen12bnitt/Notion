include BuildProperties.properties
build=$(build.major).$(build.minor).$(build.patch)
versionDir=Notion-$(build)
dir=zip-temp/$(versionDir)
DATE := $(shell /bin/date +%F-%T)


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
	rm -rf src/main/resources/public
	(cd ui/ && make clean install)
	(cd Documentation && make clean install)

install: dist
	${MAKE} sync
	${MAKE} restart

sync:
	rsync -arvz zip-temp/$(versionDir)/ qin@qia:/research/images/Notion/$(versionDir)-$(DATE)
	ssh qin@qia "cd /research/images/Notion ;ln -sfn $(versionDir)-$(DATE) Notion"

restart:
	ssh root@qia service notion restart

.PHONY: build dist install watch server sync restart
