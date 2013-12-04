
App.Device = DS.Model.extend({
	hostName: DS.attr("string"),
	applicationEntityTitle: DS.attr("string"),
	port: DS.attr("int"),
	pool: DS.belongsTo('pool')
})

App.Pool = DS.Model.extend({
	name: DS.attr('string'),
	description: DS.attr('string'),
	applicationEntityTitle: DS.attr('string'),
	devices: DS.hasMany('device')
});

App.PoolSerializer = DS.RESTSerializer.extend({
	serializeIntoHash: function(hash, type, record, options) {
		console.log ( "serializeIntoHash")
		var j = this.serialize(record, options);
		console.log ( j )
		$.each ( j, function (key, value) {
			console.log ( "\t" + key + ": " + value)
			hash[key] = value
		})
	}


})