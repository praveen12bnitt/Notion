App.PoolsRoute = Ember.Route.extend({
	model: function() {
		console.log ( "Grabbing data!")
		var nodes = this.store.find ( 'pool' )
		// console.log ( nodes )
		// var nodes = [{id:'1', name:'foo'}]
		// return nodes
		return nodes
	}
});

App.PoolsPoolRoute = Ember.Route.extend({
	model: function(params) {
		console.log ( "PoolRoute Model for ", params.pool_id)
		var pool = this.store.getById ( 'pool', params.pool_id)
		console.log ( pool )
		return pool
	}
})


App.PoolRoute = Ember.Route.extend({

	model: function(params) {
		console.log ( "PoolRoute Model for ", params.pool_id)
		var pool = this.store.getById ( 'pool', params.pool_id)
		console.log ( pool )
		return pool
	}
})


App.PoolsNewRoute = Ember.Route.extend({
	model: function() {
		console.log("Adding a new pool")
		return this.store.createRecord ( 'pool' );
	}
})