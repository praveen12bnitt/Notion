App.PoolRoute = Ember.Route.extend({
	model: function() {
		console.log ( "Grabbing data!")
		var nodes = this.store.find ( 'pool' )
		// console.log ( nodes )
		// var nodes = [{id:'1', name:'foo'}]
		return nodes
	}
});


App.PoolNewRoute = Ember.Route.extend({
	model: function() {
		return this.store.createRecord ( 'pool' );
	}
})