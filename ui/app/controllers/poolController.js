

App.PoolsNewController = Ember.ObjectController.extend({
	actions: {
		addPool: function(pool) {
			console.log("ADding a new pool")
			pool.save()
		},
		cancel: function(pool) {
			pool.deleteRecord();
			pool.save();
			this.transitionToRoute ( 'pools')
		}
	}
})