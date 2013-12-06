App = Ember.Application.create({
	LOG_TRANSITIONS: true
})


// Routes go here
App.Router.map(function() {
	// A 'resource' defines a group of routes that work together
	this.resource("pools", function() {	
		this.resource("pool", { path: ':poolKey'})
		this.route("new")
	})

	this.route('about');
	this.route("bill");
})

// Connecting to Slicer
App.ApplicationAdapter = DS.RESTAdapter.extend({
  namespace: 'rest',
  pathForType: function(type) {
  	console.log("pathForType: "+ type)
  	return type;
  },
  findHasMany: function ( store, record, url) {
  	console.log ( "Calling to get " + record + " from " + url )
  	return this._super ( store, record, url )
  }
});