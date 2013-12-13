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
			var pool = App.Pool.create();
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
			// Remember that find returns a promise, so do something when the promise is finished ()
			var c = this.controller
			App.Pool.find().then ( function ( response ) {
				c.set ( 'model', response )
			})
		}
	}
});



App.NewObjectController = Ember.ObjectController.extend({
	actions: {
		save: function() {
			this.get('model').save()
		}
	}
})

App.NewObjectView = Ember.View.extend({
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

App.NewPoolController = App.NewObjectController.extend({})
App.NewPoolView = App.NewObjectView.extend({})

App.NewDeviceController = App.NewObjectController.extend({})
App.NewDeviceView = App.NewObjectView.extend({})


App.PoolRoute = Ember.Route.extend({
	pool: null,
	model: function(params) {
		console.log ( "PoolRoute Model for ", params.poolKey)
		var pool = App.Pool.findById(params.poolKey);
		console.log ( pool )
		this.set("pool", pool)
		return pool
	},
	actions: {
		edit: function() {
			var pool =  this.controller.get('model')
			console.log ( "Editing", pool )
			this.controllerFor('newPool').set ( 'model', pool )
			this.render ( 'newPool', { into: 'pool', outlet: 'modal'});
		},

		newDevice: function() {
			console.log ( 'PoolRoute -- newDevice')
			var pool =  this.controller.get('model')
			var device = App.Device.create({'hostName' : 'unknown', 'applicationEntityTitle': 'unknown', 'port': 0, 'poolKey' : pool.get('poolKey')});
			this.controllerFor('newDevice').set ( 'model', device )
			this.render ( 'newDevice', { into: 'pool', outlet: 'modal'});
		},
		close: function() {
			console.log ( 'closing modal dialog ')
			return this.disconnectOutlet ( {
				outlet: 'modal',
				parentView: 'pool'
			})
			// this.render ( 'empty', { into: 'pools', outlet: 'modal'})
		},
		reload: function() {
			console.log ( "reloading")
			// Remember that find returns a promise, so do something when the promise is finished ()
			var c = this.controller
			App.Pool.find().then ( function ( response ) {
				c.set ( 'model', response )
			})
		}
	}
})


App.PoolsNewRoute = Ember.Route.extend({
	model: function() {
		console.log("Adding a new pool")
		return this.store.createRecord ( 'pool' );
	}
})