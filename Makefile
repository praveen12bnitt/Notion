

include BuildProperties.properties
build=$(build.major).$(build.minor).$(build.patch)
versionDir=Notion-$(build)
dir=zip-temp/$(versionDir)


dist:
	(cd Documentation && make html)
	(cd Documentation && make html)
	(cd ui/ && brunch build)
	ant jar
	rm -rf zip-temp
	mkdir -p $(dir)
	cp -r lib $(dir)
	cp Notion.jar $(dir)/Notion-$(build).jar
	mkdir -p $(dir)/Documentation
	cp -r Documentation/_build/html $(dir)/Documentation
	cp Documentation/_build/epub/Notion.epub $(dir)/Documentation
	(cd zip-temp && zip -r ../$(versionDir).zip $(versionDir))
