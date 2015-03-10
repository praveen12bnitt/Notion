
define help

Makefile for Notion/ui
  watch	  - configure and run gulp watch
  install - configure, build the ui and install in ../src/main/resources
  config  - install node and bower components, then have gulp build vendor resources
  docs	  - make Notion documentation
  clean	  - delete the public directory
endef
export help

help:
	@echo "$$help"

watch: config
	gulp watch

# Copy to resources directory, deleting any extra files
install: config
	gulp build
	rsync -r --delete public ../src/main/resources/

config: node_modules bower_components
	gulp vendor

node_modules: package.json
	npm install

bower_components: bower.json
	bower --config.interactive=false install

docs:
	(cd ../Documentation && make html)

clean:
	rm -rf public
