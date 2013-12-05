

// App.Pool = DS.Model.extend({
// 	name: DS.attr('string'),
// 	description: DS.attr('string'),
// 	applicationEntityTitle: DS.attr('string'),
// 	devices: DS.hasMany('device')
// });

// App.PoolSerializer = DS.RESTSerializer.extend({
// 	serializeIntoHash: function(hash, type, record, options) {
// 		console.log ( "serializeIntoHash")
// 		var j = this.serialize(record, options);
// 		console.log ( j )
// 		$.each ( j, function (key, value) {
// 			console.log ( "\t" + key + ": " + value)
// 			hash[key] = value
// 		})
// 	}


// })

App.Serializable = Ember.Mixin.create({
    serialize: function ()
    {
        var result = {};
        for (var key in $.extend(true, {}, this))
        {
            // Skip these
            if (key === 'isInstance' ||
            key === 'isDestroyed' ||
            key === 'isDestroying' ||
            key === 'concatenatedProperties' ||
            typeof this[key] === 'function')
            {
                continue;
            }
            result[key] = this[key];
        }
        return result;
    }
});


App.Device = Ember.Object.extend({
	hostName: null,
	applicationEntityTitle: null,
	port: null,
	deviceKey: null
})

App.Pool = Ember.Object.extend (App.Serializable, {
	poolKey: null,
	name: null,
	applicationEntityTitle: null,
	description: null,
	devices: function() {
		console.log ( "getting devices from REST server")
		var p = this
		$.getJSON("/rest/pool/" + this.get("poolKey") + "/device", function(data){
			var deviceList = Ember.A();
			$.each ( data.device, function ( i, p ) {
				var pool = App.Device.create ( p );
				deviceList.addObject ( pool )
			})
			console.log ( "DeviceList from server on " )
			console.log ( p)
			p.set ( "devices", deviceList )
		})
		return [];
	}.property(),
	save: function () {
		console.log ( "Saving this pool back to the server " )
		console.log ( this.serialize() )
		$.ajax ({
			contentType : 'application/json',
			type: "POST",
			url: "/rest/pool",
			data: JSON.stringify(this.serialize()),
			success: function ( data ) {
				console.log ( "saved!" )
			}
		})
	}
})

// Class methods
App.Pool.reopenClass({
	find: function() {
		// Return a promise
		return Ember.RSVP.Promise ( function ( resolve ) {
			$.getJSON("/rest/pool", function(data){
				var pools = Ember.A();
				$.each ( data.pool, function ( i, p ) {
					var pool = App.Pool.create ( p );
					pools.addObject ( pool )
				})
				resolve ( pools )
			})
		})
	},
	findById: function(poolKey) {
		return Ember.RSVP.Promise ( function ( resolve ) {
			$.getJSON("/rest/pool/" + poolKey, function(data){
				var pool = App.Pool.create ( data );
				resolve ( pool )
			})
		})		
	}
})
