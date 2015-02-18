
watch: config
	gulp watch

install: config
	gulp build
	# Copy to resources directory, deleting any extra files
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
