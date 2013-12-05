App.PoolsRoute = Ember.Route.extend({
	model: function() {
		console.log ( "Grabbing data!")
		// var nodes = this.store.find ( 'pool' )
		var nodes = App.Pool.find();
		console.log ( nodes )
		// var nodes = [{id:'1', name:'foo'}]
		// return nodes
		return nodes
	},
	actions: {
		open: function() {
			console.log ( 'PoolsRoute -- open')
			var pool = App.Pool.create({'name' : 'new'});
			console.log ( "pool name: " + pool.get('name'))
			this.controllerFor('newPool').set ( 'model', pool )
			console.log ( this.controllerFor('newPool').get( 'model' ).get('name') )
			this.render ( 'newPool', { into: 'pools', outlet: 'modal'});
		},
		close: function() {
			console.log ( 'closing modal dialog ')
			return this.disconnectOutlet ( {
				outlet: 'modal',
				parentView: 'pools'
			})
			// this.render ( 'empty', { into: 'pools', outlet: 'modal'})
		},
		reload: function() {
			console.log ( "reloading")
			// Remember that find returns a promise, so do something when the promise is finished (.then)
			var c = this.controller
			App.Pool.find().then ( function ( response ) {
				c.set ( 'model', response )
			})
		}
	}
});

App.NewPoolController = Ember.ObjectController.extend({
	actions: {
		save: function() {
			console.log ( 'saving our new pool ' + this.get('model').get('name'))
			this.get('model').save()

		}
	}
})
App.NewPoolView = Ember.View.extend({
	didInsertElement: function() {
    this.$('.modal, .modal-backdrop').addClass('in');
  },
  layoutName: 'modal_layout',
  actions: {
  	close: function() {
  		console.log ( "NewPoolView -- close")
  		var view = this;
        // use one of: transitionend webkitTransitionEnd oTransitionEnd MSTransitionEnd
        // events so the handler is only fired once in your browser
        this.$('.modal').one("transitionend", function(ev) {
        	view.controller.send('close');
        	view.controller.send('reload');
        });
    
        this.$('.modal, .modal-backdrop').removeClass('in');
	}
  }
})

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
		console.log ( "PoolRoute Model for ", params.poolKey)
		var pool = App.Pool.findById(params.poolKey);
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