

App.PoolNewController = Ember.ObjectController.extend({
	actions: {
		addPool: function(pool) {
			pool.save()
		}
	}
})