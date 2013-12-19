

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


App.Device = Ember.Object.extend(App.Serializable, {
	hostName: null,
	applicationEntityTitle: null,
	port: null,
	deviceKey: -1,
	poolKey: -1,
	save: function ( ) {
		console.log ( "Saving this device back to the server " )
		console.log ( this.serialize() )
		$.ajax ({
			contentType : 'application/json',
			type: "POST",
			url: "/rest/pool/" + this.get('poolKey') + "/device" ,
			data: JSON.stringify(this.serialize()),
			success: function ( data ) {
				console.log ( "saved!" )
			}
		})
	}
})

App.Script = Ember.Object.extend(App.Serializable, {
	tag: null,
	poolKey: -1,
	script: null,
	scriptKey: null,
	display: false,
	scriptEditorId: function() {
		return "editor-" + this.get("scriptKey")
	}.property('scriptKey'),
	tryScript: function() {
		console.log ( "Trying script: " + this.script + " on the server")
		var self = this
		var url = "/rest/pool/" + this.get('poolKey') + '/script/try'
		$.ajax ({
			contentType: 'application/json',
			type: 'PUT',
			url: url,
			data: JSON.stringify(this.serialize()),
			success: function ( data ) {
				console.log("Tried script, got back ", data)
				self.set('tryResult', data.result)
			}
		})
	},
	save: function() {
		console.log ( "Saving script for " + this.tag + " back to server", this.serialize())
		var url = "/rest/pool/" + this.get('poolKey') + "/script"
		var action = "POST"
		// If we are editing
		if ( this.scriptKey ) {
			url = url + "/" + this.get('scriptKey')
			action = "PUT"
		}
		$.ajax ({
			contentType : 'application/json',
			type: action,
			url: url,
			data: JSON.stringify(this.serialize()),
			success: function ( data ) {
				console.log ( "saved!" )
			}
		})		
	}
})


App.Pool = Ember.Object.extend (App.Serializable, {
	poolKey: null,
	name: null,
	applicationEntityTitle: null,
	description: null,
	anonymize: false,
	editCTP: false,
	devices: function() {
		return this.loadDevices()
	}.property(),
	scripts: function() {
		return this.loadScripts();
	}.property(),
	loadDevices: function() {
		console.log ( "getting devices from REST server")
		var p = this
		var poolKey = this.get('poolKey')
		$.getJSON("/rest/pool/" + this.get("poolKey") + "/device", function(data){
			var deviceList = Ember.A();
			$.each ( data.device, function ( i, p ) {
				var device = App.Device.create ( p );
				device.set('poolKey', poolKey)
				deviceList.addObject ( device )
			})
			console.log ( "DeviceList from server on " )
			console.log ( p)
			p.set ( "devices", deviceList )
		})
		return [];
	},
	loadScripts: function() {
		console.log ( "Fetching scripts")
		var that = this
		var poolKey = this.get('poolKey')
		$.getJSON("/rest/pool/" + this.get("poolKey") + "/script", function(data){
			var scriptList = Ember.A();
			$.each ( data.script, function(i,p) {
				var script = App.Script.create(p)
				script.set('poolKey', poolKey)
				scriptList.addObject(script)
			})
			that.set ( "scripts", scriptList )
		})
		return [];
	},
	ctpConfig: function () {
		console.log ( "Fetching CTP Config")
		var that = this
		var poolKey = this.get('poolKey')
		$.getJSON("/rest/pool/" + this.get("poolKey") + "/ctp", function(data){
			that.set ( 'ctpConfig', data.script )
		})
		return null;
	}.property(),
	saveCTP: function() {
		console.log ("Saving CTP config")
		var self = this
		$.ajax({
			url: "/rest/pool/" + this.get("poolKey") + "/ctp",
			contentType : 'application/json',
			type: "PUT",
			data: JSON.stringify ( { 'script': self.get('ctpConfig')} )
		})
	},
	save: function () {
		console.log ( "Saving this pool back to the server " )
		console.log ( this.serialize() )
		if ( this.poolKey ) {
			// We want to update
			$.ajax({
				contentType : 'application/json',
				type: "PUT",
				url: "/rest/pool/" + this.poolKey,
				data: JSON.stringify(this.serialize()),
				success: function ( data ) {
					console.log ( "saved!" )
				}
			})
		} else {
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
		console.log("Fetching pool by key " + poolKey)
		return Ember.RSVP.Promise ( function ( resolve ) {
			$.getJSON("/rest/pool/" + poolKey, function(data){
				console.log("Fetched pool data", data)
				var pool = App.Pool.create ( data );
				console.log("Constructed a pool object", pool)
				resolve ( pool )
			})
		})		
	}
})
