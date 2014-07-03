include BuildProperties.properties
build=$(build.major).$(build.minor).$(build.patch)
versionDir=Notion-$(build)
dir=zip-temp/$(versionDir)


# The Log4j setup

dist:
	(cd ui/ && make install)
	(cd Documentation && make install)
	./gradlew jar
	rm -rf zip-temp
	mkdir -p $(dir)
	cp -r build/libs/lib $(dir)
	cp notion $(dir)
	cp build/libs/Notion.jar $(dir)/Notion.jar
	cp Readme.md notion.example.yml $(dir)
	(cd zip-temp && zip -r $(versionDir).zip $(versionDir) && mv $(versionDir).zip ../)

watch:
	(cd Documentation && while :; do make html ; sleep 5s; done)

install: dist
	${MAKE} sync

server:
	./gradlew jar

sync:
	rsync -arvz zip-temp/$(versionDir) qin@qia:/research/images/Notion
