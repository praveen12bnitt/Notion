ConnectorModel = Backbone.Model.extend({
  idAttribute: "connectorKey"
});

ConnectorCollection = Backbone.Collection.extend({
  model: ConnectorModel,
  url: '/rest/connector',
  parse: function(response) {
    var m = [];
    for(var i = 0; i < response.connector.length; i++) {
      m.push(new ConnectorModel(response.connector[i]));
    }
    this.set ( m );
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
  url: function() { console.log ( "PoolCollection.url "); return '/rest/pool'; },
  parse: function(response) {
    var m = [];
    for(var i = 0; i < response.pool.length; i++) {
      m.push(new PoolModel(response.pool[i]));
    }
    this.set ( m );
    return this.models;
  }
});

DeviceModel = Backbone.Model.extend({
  idAttribute: 'deviceKey',
  format: function() {
    return this.get('applicationEntityTitle') + "@" + this.get('hostName') + ":" + this.get('port');
  }
});
DeviceCollection = Backbone.Collection.extend({
  model: DeviceModel,
  url: function () { return this.urlRoot; },
  parse: function(response) {
    var m = [];
    for(var i = 0; i < response.device.length; i++) {
      m.push(new DeviceModel(response.device[i]));
    }
    this.set ( m );
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
  urlRoot: function() { return 'rest/pool' + this.get('poolKey') + '/query/'; },
  parse: function(response) {
    // Sort the items
    response.items.sort ( function(a,b){
      return a.queryItemKey - b.queryItemKey;
    });
    for ( var i = 0; i < response.items.length; i++ ) {
      response.items[i].items.sort ( function(a,b) {
        return a.queryResultKey - b.queryResultKey;
      });
    }
    return response;
  }
});
QueryCollection = Backbone.Collection.extend({
  model: QueryModel,
  url: function () { console.log("QueryCollection url"); return this.urlRoot; },
  parse: function(response) {
    var m = [];
    for(var i = 0; i < response.queries.length; i++) {
      m.push(new QueryModel(response.queries[i]));
    }
    this.set ( m );
    return this.models;
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
      m.push(new ScriptModel(response.script[i]));
    }
    this.set ( m );
    return this.models;
  }

});


// Groups
GroupModel = Backbone.Model.extend({
  idAttribute: 'groupKey',
  urlRoot: function() { return 'rest/authorization/group/'; }
});

GroupCollection = Backbone.Collection.extend({
  model: GroupModel,
  url: 'rest/authorization/group',
  urlRoot: 'rest/authorization/group',
  parse: function(response) {
    console.log("Got response: ", response);
    var m = [];
    for(var i = 0; i < response.group.length; i++) {
      m.push(new GroupModel(response.group[i]));
    }
    this.set ( m );
    return this.models;
  }
});


GroupRoleModel = Backbone.Model.extend({
  idAttribute: 'groupRoleKey',
  urlRoot: function() {
    return 'rest/pool/' + this.get('poolKey') + "/grouprole/";
  }
});
GroupRoleCollection = Backbone.Collection.extend({
  model: GroupRoleModel,
  url: function () { return this.urlRoot; },
  parse: function(response) {
    console.log("Got response: ", response);
    var m = [];
    for(var i = 0; i < response.groupRole.length; i++) {
      m.push(new this.model(response.groupRole[i]));
    }
    this.set ( m );
    return this.models;
  }
});


UserModel = Backbone.Model.extend({
  idAttribute: 'userKey',
  urlRoot: function() {
    return 'rest/authorization/users';
  }
});
UserCollection = Backbone.Collection.extend({
  model: UserModel,
  url: 'rest/authorization/users',
  parse: function(response) {
    console.log("Got response: ", response);
    var m = [];
    for(var i = 0; i < response.users.length; i++) {
      m.push(new this.model(response.users[i]));
    }
    this.set ( m );
    return this.models;
  }
});
