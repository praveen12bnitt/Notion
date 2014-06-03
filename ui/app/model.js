ConnectorModel = Backbone.Model.extend({
  idAttribute: "connectorKey"
});

ConnectorCollection = Backbone.Collection.extend({
  model: ConnectorModel,
  url: '/rest/connector',
  parse: function(response) {
    var m = [];
    for(var i = 0; i < response.connector.length; i++) {
      m.push(new ConnectorModel(response.connector[i]))
    }
    this.set ( m )
    return this.models;
  }
});

PoolModel = Backbone.Model.extend({
  idAttribute: "poolKey",
  // urlRoot: '/rest/pool',
defaults: {
  'name' : null,
  'anonymize' : false,
  'applicationEntityTitle' : null,
  'description' : "This is a new Pool"
}
});

PoolCollection = Backbone.Collection.extend({
model: PoolModel,
url: '/rest/pool',
parse: function(response) {
  var m = [];
  for(var i = 0; i < response.pool.length; i++) {
    m.push(new PoolModel(response.pool[i]))
  }
  this.set ( m )
  return this.models;
}
});

DeviceModel = Backbone.Model.extend({
idAttribute: 'deviceKey',
format: function() {
  return this.get('applicationEntityTitle') + "@" + this.get('hostName') + ":" + this.get('port')
}
});
DeviceCollection = Backbone.Collection.extend({
model: DeviceModel,
url: function () { return this.urlRoot; },
parse: function(response) {
  var m = [];
  for(var i = 0; i < response.device.length; i++) {
    m.push(new DeviceModel(response.device[i]))
  }
  this.set ( m )
  return this.models;
}
});

CTPModel = Backbone.Model.extend();
QueryModel = Backbone.Model.extend({
idAttribute: 'queryKey',
url: function () {
  // return '/rest/pool/' + this.get('poolKey') + '/query/' + this.get('queryKey')
  return this.urlRoot;
},
parse: function(response) {
  // Sort the items
  response.items.sort ( function(a,b){
    return a.queryItemKey - b.queryItemKey
  });
  for ( var i = 0; i < response.items.length; i++ ) {
    response.items[i].items.sort ( function(a,b){
      return a.queryResultKey - b.queryResultKey;
    })
  }
  return response;
}
});

ScriptModel = Backbone.Model.extend({
});

ScriptCollection = Backbone.Collection.extend({
model: ScriptModel,
url: function () { return this.urlRoot; },
parse: function(response) {
  var m = [];
  for(var i = 0; i < response.script.length; i++) {
    m.push(new ScriptModel(response.script[i]))
  }
  this.set ( m )
  return this.models;
}

});
