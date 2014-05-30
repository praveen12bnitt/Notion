

// App.Pool = DS.Model.extend({
//   name: DS.attr('string'),
//   description: DS.attr('string'),
//   applicationEntityTitle: DS.attr('string'),
//   devices: DS.hasMany('device')
// });

// App.PoolSerializer = DS.RESTSerializer.extend({
//   serializeIntoHash: function(hash, type, record, options) {
//     console.log ( "serializeIntoHash")
//     var j = this.serialize(record, options);
//     console.log ( j )
//     $.each ( j, function (key, value) {
//       console.log ( "\t" + key + ": " + value)
//       hash[key] = value
//     })
//   }


// })

// Configuration for require.js
// foundation, xtk and dat.gui are loaded by default
require.config({
  // deps: ['./vex.dialog', "./vex"],
  baseURL: 'js',
  // Some packages do not provide require info, so we 'shim' them here
  shim: {
    'angular': { exports: 'angular'},
    'angular-route': ['angular'],
    'angular-ui-router' : ['angular'],
    'ui-ace' : ['angular'],
    // The angularAMD and ngload let us load a page and add angular apps later
    'angularAMD':['angular'],
    'ngload':['angularAMD'],
    'ui-bootstrap-tpls':['angular']
  }
})

// For Grater to work, the model, angular and angularAMD packages are required
require(['angular', 'angularAMD', "backbone", 'angular-ui-router', 'ui-bootstrap-tpls', 'ui-ace', 'ace/ace'], function(angular, angularAMD, Backbone ) {

  // Helper for shortening strings
  String.prototype.trunc = String.prototype.trunc ||
  function(n){
    return this.length>n ? this.substr(0,n-1)+'...' : this;
  };

  String.prototype.startsWith = String.prototype.startsWith ||
  function (str){
    return this.indexOf(str) == 0;
  };

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
  idAttribute: 'scriptKey'
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

notionApp = angular.module('notionApp', ['ui.router', 'ui.bootstrap', 'ui.ace']);

notionApp.config(function($stateProvider, $urlRouterProvider) {
  $urlRouterProvider.otherwise('/pools/index')
    $urlRouterProvider.when('', '/pools/index')

  $stateProvider
  .state('root', {
    abstract: true,
    url: '',
    templateUrl: 'partials/root.html',
    controller: 'RootController'
  })
  .state('root.pools', {
    abstract: true,
    url: '/pools',
    templateUrl: 'partials/pools.html',
    controller: 'PoolsController',
    data: {
      access: ['admin', 'user']
    }
  })
  .state('root.pools.index', {
    url: "/index",
    templateUrl: 'partials/pools.index.html',
    data: {
      access: ['admin', 'user']
    }
  })
  .state('root.pools.pool', {
    url: "/:poolKey",
    templateUrl: 'partials/pool.detail.html',
    controller: 'PoolController',
    data: {
      access: ['admin', 'user']
    }
  })
  .state('root.pools.studies', {
    url: "/:poolKey/studies",
    templateUrl: 'partials/pool.studies.html',
    controller: 'StudyController',
    data: {
      access: ['admin', 'user']
    }
  })
  .state('root.pools.query', {
    url: "/:poolKey/query",
    templateUrl: 'partials/pool.query.html',
    controller: 'QueryController',
    data: {
      access: ['admin', 'user']
    }
  })
  .state('root.connectors', {
    url: "/connectors",
    templateUrl: 'partials/connectors.html',
    controller: 'ConnectorsController',
    data: {
      access: ['admin']
    }
  })
	.state('root.loggedout', {
	    templateUrl: 'partials/loggedout.html',
	});

});


notionApp.config(function ($httpProvider) {
  $httpProvider.interceptors.push([
    '$injector',
    function ($injector) {
      return $injector.get('AuthInterceptor');
    }
  ]);
});

notionApp.constant('EVENTS', {
  loginSuccess: 'auth-login-success',
  loginFailed: 'auth-login-failed',
  logoutSuccess: 'auth-logout-success',
  sessionTimeout: 'auth-session-timeout',
  notAuthenticated: 'auth-not-authenticated',
  notAuthorized: 'auth-not-authorized'
});

notionApp.factory('AuthInterceptor', function ($rootScope, $q,EVENTS) {
  return {
    responseError: function (response) {
      if (response.status === 401) {
        $rootScope.$broadcast(EVENTS.notAuthenticated,
                              response);
      }
      if (response.status === 403) {
        $rootScope.$broadcast(EVENTS.notAuthorized,
                              response);
      }
      if (response.status === 419 || response.status === 440) {
        $rootScope.$broadcast(EVENTS.sessionTimeout,
                              response);
      }
      return $q.reject(response);
    }
  };
})



// authorization works by checking to see if the user can access certain routes.
// The check is done by role.
notionApp.factory ( 'authorization', function($http) {
  var u = { username: null, roles: [] };
  function changeUser(newUser) {
    // console.log("Changing user to ", newUser, 'from ', u)
    angular.extend(u,newUser.user);
  }
  return {
    isLoggedIn: function() {
      if ( u === undefined ) {
        return false;
      }
	     var r = u.username != null;
	// console.log("isLoggedIn:", u, r);
      return u.username != null;
    },
    logout: function(success, error) {
      $http.post("/rest/user/logout")
      .success( function() {
        changeUser({username: null, roles:[]});
        success();
      }).error(error);
    },
    checkLogin: function(success,error) {
      $http.get("/rest/user/").success(function(result){
	  // console.log("checkLogin: ", result );
          changeUser(result);
	  if ( success ) { success(); }
      }).error(error);
    },
    login: function(credentials, success, error) {
      $http.post('/rest/user/login', credentials ).success(function(result) {
        u = result;
        changeUser(u);
        success();
      }).error(error);
    },
    isPermitted: function(expectedRoles) {
      // console.log("isPermitted: ", expectedRoles, " for user ", u)
      if ( u === undefined  ) {
        // console.log("undefined or no roles")
        return false; }
      for ( var i = 0; i < expectedRoles.length; i++ ) {
        // console.log("jquery:", $.inArray(String(expectedRoles[i]), u.roles))
        // console.log("jquery #2:", $.inArray('admin', u.roles))
        // console.log("looking at ", expectedRoles[i], u.roles.indexOf(expectedRoles[i]) )
        for ( var j = 0; j < u.roles.length; j++ ) {
          if ( String(u.roles[j]) === String(expectedRoles[i])) {
            return true;
          }
        }

      }
      return false;
    },
    user: u
  }
})

notionApp.run(['$rootScope', '$state', 'authorization', function( $rootScope, $state, authorization ) {
  console.log ( "run from notionApp", authorization.isLoggedIn() )

  $rootScope.$on("$stateChangeStart", function(e, toState, toParams, fromState, fromParams) {
      return;
    if ( !toState.data ) {
      console.log("No authorization data...", toState)
      return;
    }
    if ( !authorization.isPermitted(toState.data.access)) {
      e.preventDefault();
      console.log ("not logged in")
      // return $state.transitionTo('root.login')
    } else {
      console.log ("User", authorization, " is permitted to ", toState)
    }
  });
}]);


notionApp.controller("RootController", function($scope, $state, authorization,$timeout,$http,$modal,$window) {

	var heartbeat = function() {
    authorization.checkLogin( function() {
      if ( authorization.isLoggedIn() ) {
        console.log ( "Logged in, all is well!" );
        // $scope.user = authorization.user
        $scope.user = $.extend(true, {}, authorization.user)

        $timeout(heartbeat,60000);
      } else {
        console.log ( "Not logged in, something is amiss" );
      $window.location.href = "login.html";
      }
    });
  };
  heartbeat();
  console.log("Creating RootController")
  $scope.name = "RootController"
  $scope.loggedIn = true;


    $scope.settings = function(){
      $modal.open({
        templateUrl: 'partials/settings.html',
        scope: $scope,
        controller: function($scope,$modalInstance) {
          $scope.updateUser = $.extend(true,{},$scope.user)
          $scope.save = function() {
            $http.put("/rest/user/update", $scope.updateUser)
            .success(function() {
              $.extend(true, $scope.user, $scope.updateUser);
              $modalInstance.dismiss();
            }
              );
          };
          $scope.close = function() { $modalInstance.dismiss() }
        }
      })
    };

  $scope.logout = function() {

    console.log("Starting logout");
    $http.post("/rest/user/logout")
    .success( function() {
      console.log("Logout completed" );
      $window.location.href = "login.html";
    }).error(function() {
		    // $window.location.href = "login.html";
      });
  }
})


notionApp.controller ( 'ConnectorsController', function($scope,$timeout,$state,$modal,authorization) {
  console.log("is logged in: ", authorization.isLoggedIn() )
  $scope.poolCollection = new PoolCollection();
  // Make the first one syncrhonous
  $scope.poolCollection.fetch({remove:true, async:false})
  $scope.connectorCollection = new ConnectorCollection();
  $scope.connectorCollection.fetch({async:false});
  $scope.pools = $scope.poolCollection.toJSON()
  $scope.deviceCache = {}

  $scope.getPoolInfo = function(poolKey) {
    var pool = $scope.poolCollection.get(poolKey);
    return pool.get('name') + " / " + pool.get("applicationEntityTitle");
  }

  $scope.getDeviceInfo = function (poolKey, deviceKey) {
    var device;
    if ( deviceKey in $scope.deviceCache ) {
      device = $scope.deviceCache[deviceKey]
    } else {
      device = new DeviceModel();
      device.urlRoot = '/rest/pool/' + poolKey + '/device/' + deviceKey;
      device.fetch({async:false})
      console.log("Got devcie ", device)
      $scope.deviceCache[deviceKey] = device;
    }
    return device.get("applicationEntityTitle") + "@" + device.get("hostName") + ":" + device.get("port")
  }

  $scope.deleteConnector = function(connector) {
    $modal.open ({
      templateUrl: 'partials/modal.html',
      controller: function($scope, $modalInstance) {
        $scope.title = "Delete connector?"
        $scope.message = "Delete the connector: " + connector.get('name')
        $scope.ok = function(){
          connector.destroy({
            success: function(model, response) {
              console.log("Dismissing modal")
              $modalInstance.dismiss();
              $scope.$apply();
            },
            error: function(model, response) {
              alert ( "Failed to delete Connector: " + response.message )
            }
          })
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };

  // Devices
  $scope.editConnector = function(connector) {
    console.log("Edit Connector")
    var newConnector = !connector
    if ( !connector ) {
      connector = new ConnectorModel()
    }
    $scope.connector = connector
    $scope.model = connector.toJSON()
    $modal.open ( {
      templateUrl: 'partials/connector.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        $scope.devices = [];
        if ( newConnector ) {
          $scope.title = "Create a new connector"
        } else {
          $scope.title = "Edit the connector"
        }
        $scope.queryPoolChanged = function() {
          console.log("get devices")
          // Grab the devices
          var deviceCollection = new DeviceCollection();
          deviceCollection.urlRoot = '/rest/pool/' + $scope.model.queryPoolKey + '/device';
          deviceCollection.fetch({async:false})
          $scope.devices = deviceCollection.toJSON()
        }
        $scope.getDevices = function(poolKey) {
          return $scope.devices;
        }
        if ( !newConnector ) {
          $scope.queryPoolChanged();
        }
        $scope.save = function(){
          connector.set ( $scope.model )
          $scope.connectorCollection.add(connector);
          console.log("saving here!!!!", $scope, $scope.model, connector )
          $scope.connector.save();
          $modalInstance.close();
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };




});

notionApp.controller ( 'PoolsController', function($scope,$timeout,$state,$modal, authorization) {
  console.log("Creating PoolsController")
  $scope.name = "PoolsController"
  $scope.poolCollection = new PoolCollection();
  // Make the first one syncrhonous
  $scope.poolCollection.fetch({remove:true, async:false})
  // $scope.user = authorization.user;

  p = $scope.poolCollection;
  $scope.newPoolKey = false;

  $scope.refresh = function() {
    $scope.poolCollection.fetch({remove:true, success: function() {
      $scope.$apply();
    }});
  };


  $scope.newPool = function() {
    $modal.open ({
      templateUrl: 'partials/pool.edit.html',
      scope: $scope,
      controller: function($scope,$modalInstance) {
        $scope.pool = new PoolModel();
        $scope.model = $scope.pool.toJSON()
        $scope.title = "Create a new pool"
        $scope.hideAETitle = false
        $scope.save = function() {
          $scope.pool.set ( $scope.model )
          $scope.poolCollection.add( $scope.pool)
          $scope.pool.save ( $scope.model )
          $modalInstance.close()
          $scope.poolCollection.fetch({remove:true, success: function() {
            $scope.$apply();
          }})
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };

});

notionApp.controller ( 'PoolController', function($scope,$timeout,$stateParams, $state, $modal) {
  $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey)
  console.log ( "PoolController for ", $stateParams.poolKey )
  console.log ( "Pool is: ", $scope.pool)
  $scope.model = $scope.pool.toJSON();

  // Grab the devices
  $scope.deviceCollection = new DeviceCollection();
  $scope.deviceCollection.urlRoot = '/rest/pool/' + $stateParams.poolKey + '/device';
  $scope.deviceCollection.fetch({async:false})
  console.log( $scope.deviceCollection )
  pool = $scope.pool
  devices = $scope.deviceCollection

  $scope.edit = function() {
    $modal.open ( {
      templateUrl: 'partials/pool.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        $scope.title = "Edit " + $scope.pool.get('name')
        $scope.hideAETitle = true
        $scope.save = function() {
          $scope.pool.save ( $scope.model )
          $modalInstance.close()
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };

  // Devices
  $scope.editDevice = function(device) {
    console.log("EditDevice")
    var newDevice = !device
    if ( !device ) {
      console.log("Create new device")
      device = new DeviceModel()
    }
    $scope.device = device
    $scope.deviceModel = device.toJSON()
    $modal.open ( {
      templateUrl: 'partials/device.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        if ( newDevice ) {
          $scope.title = "Create a new device"
        } else {
          $scope.title = "Edit the device"
        }
        $scope.save = function(){
          device.set ( $scope.deviceModel )
          $scope.deviceCollection.add(device)
          device.save();
          $modalInstance.close();
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };


  $scope.deleteDevice = function(device) {
    $scope.device = device
    $scope.deviceModel = device.toJSON()
    $modal.open ({
      templateUrl: 'partials/modal.html',
      controller: function($scope, $modalInstance) {
        $scope.title = "Delete device?"
        $scope.message = "Delete the device: " + device.format()
        $scope.ok = function(){
          device.destroy({
            success: function(model, response) {
              console.log("Dismissing modal")
              $modalInstance.dismiss();
              $scope.$apply();
            },
            error: function(model, response) {
              alert ( "Failed to delete Device: " + response.message )
            }
          })
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };


  // CTP Configuration
  $scope.ctp = new CTPModel();
  $scope.ctp.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/ctp';
  $scope.ctp.fetch({async:false})
  $scope.ctpScript = $scope.ctp.get("script")
  $scope.saveCTP = function() {
    $scope.ctp.set('script', $scope.ctpScript);
    $scope.ctp.sync("update", $scope.ctp)
  }

  // Scripts
  $scope.scriptCollection = new ScriptCollection();
  $scope.scriptCollection.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/script';
  $scope.scriptCollection.fetch({async:false})

  $scope.deleteScript = function(script) {
    $scope.script = script
    $scope.scriptModel = script.toJSON()
    $modal.open ({
      templateUrl: 'partials/modal.html',
      controller: function($scope, $modalInstance) {
        $scope.title = "Delete script?"
        $scope.message = "Delete the script for " + script.get('tag')
        $scope.ok = function(){
          script.destroy({
            success: function(model, response) {
              console.log("Dismissing modal")
              $modalInstance.dismiss();
              $scope.$apply();
            },
            error: function(model, response) {
              alert ( "Failed to delete script: " + response.message )
            }
          })
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  }


  $scope.editScript = function(script) {
    var newScript = !script
    if ( newScript ) {
      script = new ScriptModel();
    }
    $modal.open ({
      templateUrl: 'partials/script.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        $scope.script = script
        $scope.scriptModel = script.toJSON()
        $scope.tryResult = ""
        if ( newScript ) {
          $scope.title = "Create a new script"
        } else {
          $scope.title = "Edit the script for " + script.get('tag')
          $scope.hideTag = true
        }
        $scope.save = function(){
          script.set ( $scope.scriptModel )
          $scope.scriptCollection.add(script)
          console.log ( "Saving script ", script)
          script.save();
          $modalInstance.close();
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
        $scope.aceLoaded = function(editor) {
          console.log ( "Ace loaded" )
          editor.commands.addCommand({
            name: 'save',
            bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
            exec: $scope.save
          });

          // Try
          editor.commands.addCommand({
            name: 'exec',
            bindKey: {win: 'Ctrl-Return', mac: 'Command-Return'},
            exec: $scope.try
          });
        };

        $scope.try = function() {
          // Cal the rest API and give it a go
          console.log ( "Trying script on the server")
          var url = "/rest/pool/" + $scope.pool.get('poolKey') + '/script/try'
          $.ajax ({
            contentType: 'application/json',
            type: 'PUT',
            url: url,
            data: JSON.stringify($scope.scriptModel),
            success: function ( data ) {
              console.log("Tried script, got back ", data)
              $scope.$apply ( function() {
                $scope.tryResult = data.result
              })
            }
          });
        }
      }
    })
  }

});

notionApp.controller ( 'StudyController', function($scope,$http,$timeout,$stateParams, $state, $modal) {
  $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey)
  $scope.numberOfItems = 1;
  $scope.pageSize = 50;

  $scope.reload = function(){
    console.log("Page is " + $scope.currentPage)
    var start = 0;
    if ( $scope.currentPage ) {
      start = $scope.pageSize * ($scope.currentPage - 1 );
    }
    $http.post('/rest/pool/' + $scope.pool.get('poolKey') + '/studies',
    {
      jtStartIndex: start,
      jtPageSize: $scope.pageSize,
      PatientID : $scope.PatientID,
      PatientName : $scope.PatientName,
      AccessionNumber : $scope.AccessionNumber
    }
  )
  .success(function(data,status,headers) {
    console.log("got ", data)
    $scope.studies = data
    $scope.numberOfItems = data.TotalRecordCount
  });
};

$scope.$watch('currentPage', $scope.reload);
$scope.clear = function() {
  $scope.PatientID = "";
  $scope.PatientName = "";
  $scope.AccessionNumber = "";
  $scope.reload()

}
$scope.deleteStudy = function(study) {
  $scope.study = study
  $modal.open ({
    templateUrl: 'partials/modal.html',
    scope: $scope,
    controller: function($scope, $modalInstance) {
      $scope.title = "Delete study?"
      $scope.message = "Delete study " + study.StudyDescription + " for " + study.PatientName + " / " + study.PatientID + " / " + study.AccessionNumber
      $scope.ok = function(){
        $http.delete("/rest/pool/" + $scope.pool.get("poolKey") + "/studies/" + study.StudyKey)
        .success(function() {
          $scope.reload()
          $modalInstance.dismiss()
        })
      };
      $scope.cancel = function() { $modalInstance.dismiss() };
    }
  });
};



});


notionApp.controller ( 'QueryController', function($scope,$timeout,$stateParams, $state, $modal) {
  $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey)
  console.log ( "QueryController for ", $stateParams.poolKey )
  console.log ( "Pool is: ", $scope.pool)
  $scope.model = $scope.pool.toJSON();
  $scope.pools = $scope.$parent.poolCollection.toJSON()

  $scope.connectorCollection = new ConnectorCollection();
  $scope.connectorCollection.fetch({async:false});
  $scope.connectors = $scope.connectorCollection.toJSON();

  $scope.refresh = function(){
    console.log ( $scope.query )
    $scope.query.fetch({'async':false});
  };

  $scope.fetchAll = function(item){
    $.each(item.items, function(index,value){
      item.items[index].doFetch = true
    })
  };

  $scope.toggleFetch = function(item) {
    item.doFetch = !item.doFetch
  }

  $scope.submit = function() {
    console.log ( $('#queryFile')[0].files[0])
    var formData = new FormData();
    console.log ( formData )
    formData.append('file', $('#queryFile')[0].files[0] )
    formData.append('connectorKey', $scope.connectorKey)
    console.log ( formData )


    $.ajax({
      url: '/rest/pool/' + $scope.pool.get('poolKey') + '/query',
      type: 'POST',
      data: formData,
      processData: false,
      contentType: false,
      success: function(data) {
        $scope.$apply ( function(){
          console.log("Create query")
          $scope.query = new QueryModel(data);
          $scope.query.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/query/' + $scope.query.get('queryKey');
          queryTick();
        })
      },
      error: function(xhr, status, error) {
        alert ( "Query failed: " + xhr.responseText )
      }
    });
  };

  var queryTick = function(){
    if ( $scope.query ) {
      // console.log("queryTick")
      $scope.query.fetch().done(function() {
        // console.log ("queryTick completed")
        if ($scope.query.get('status').match("Pending")) {
          $timeout(queryTick, 2000)
        }
      });
      // console.log("query done")
      // });
    }
  };
  $scope.reset = function() {
    $scope.query = null;
  }
  $scope.fetch = function(){
    $scope.query.save({async:false});
    console.log("Saved query", $scope.query)
    $.ajax({
      url: $scope.query.urlRoot + "/fetch",
      type: 'PUT',
      data: {},
      success: function(data){
        queryTick();
        console.log("started ticker")
        $scope.refresh();
      }
    });
  };

});

// Here is where the fun happens. angularAMD contains support for initializing an angular
// app after the page load.
angularAMD.bootstrap(notionApp);


console.log ("Build notion app")
})
