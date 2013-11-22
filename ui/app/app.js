App = Ember.Application.create({})


// Routes go here
App.Router.map(function() {
	this.resource("pool", function() {
		this.route("new", {path:"/new"})
	});
	this.resource('about');
})

// Connecting to Slicer
App.ApplicationAdapter = DS.RESTAdapter.extend({
  namespace: 'rest',
  pathForType: function(type) {
  	console.log("pathForType: "+ type)
  	return type;
  }
});