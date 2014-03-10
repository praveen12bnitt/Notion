

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
require(['angular', 'angularAMD', "Backbone", 'angular-ui-router', 'ui-bootstrap-tpls', 'ui-ace', 'ace/ace' ], function(angular, angularAMD, Backbone ) {

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
    idAttribute: 'deviceKey'
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

  ScriptModel = Backbone.Model.extend({
    idAttribute: 'scriptKey'
  });
  ScriptCollection = Backbone.Collection.extend({
    model: ScriptModel,
    url: function () { return this.urlRoot; },
    parse: function(response) {
      var m = [];
      for(var i = 0; i < response.script.length; i++) {
        m.push(new DeviceModel(response.script[i]))
      }
      this.set ( m )
      return this.models;
    }

  });



  notionApp = angular.module('notionApp', ['ui.router', 'ui.bootstrap', 'ui.ace']);

  notionApp.config(function($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.when('', '/pools/index')
    $urlRouterProvider.otherwise('/pools/index')
    $stateProvider
    .state('pools', {
      abstract: true,
      url: "/pools",
      templateUrl: 'partials/pools.html',
      controller: 'PoolsController'
    })
    .state('pools.index', {
      url: "/index",
      templateUrl: 'partials/pools.index.html',
      controller: 'PoolsController'
    })
    .state('pools.new', {
      url: "/new",
      templateUrl: 'partials/pool.new.html',
      controller: 'NewPoolController'
    })
    .state('pools.pool', {
      url: "/:poolKey",
      templateUrl: 'partials/pool.detail.html',
      controller: 'PoolController'
    })
  });

    // ['$routeProvider',
    // function($routeProvider){
    //   $routeProvider.
    //   when('/', {
    //     templateUrl: 'partials/pools.html',
    //     controller: 'PoolsController'
    //   });
    // }]);

  notionApp.controller ( 'PoolsController', function($scope,$timeout,$state) {
    $scope.poolCollection = new PoolCollection();
    // Make the first one syncrhonous
    $scope.poolCollection.fetch({remove:true, async:false})

    p = $scope.poolCollection;
    $scope.newPoolKey = false;

    $scope.refresh = function() {
      $scope.$apply (  $scope.poolCollection.fetch({remove:true, async:false}) );
    };


    (function tick() {
      $scope.poolCollection.fetch({remove: true});
      if ( $scope.newPoolKey ) {
        $state.transitionTo ( 'pools.pool', { poolKey: $scope.newPoolKey} )
        $scope.newPoolKey = false
      }
      $timeout(tick, 20000)
    })();
  });

  notionApp.controller ( 'PoolController', function($scope,$timeout,$stateParams, $state, $modal) {
    $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey)
    console.log ( "PoolController for ", $stateParams.poolKey )
    console.log ( "Pool is: ", $scope.pool)
    $scope.model = $scope.pool.toJSON();

    // Grab the devices
    $scope.deviceCollection = new DeviceCollection();
    $scope.deviceCollection.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/device';
    console.log( $scope.deviceCollection )
    $scope.deviceCollection.fetch({async:false})
    pool = $scope.pool
    devices = $scope.deviceCollection

    $scope.edit = function() {
      $modal.open ( {
        templateUrl: 'partials/pool.edit.html',
        scope: $scope,
        controller: function($scope, $modalInstance) {
          $scope.save = function() {
            $scope.pool.save ( $scope.model )
            $modalInstance.close()
          };
          $scope.cancel = function() { $modalInstance.dismiss() };
        }
      });
    };

    $scope.editDevice = function(device) {
      console.log("EditDevice")
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

  })

  notionApp.controller ( 'NewPoolController', function($scope, $state) {
    // Create a new pool
    $scope.pool = new PoolModel();
    $scope.model = $scope.pool.toJSON();

    $scope.save = function() {
      $scope.pool.set ( $scope.model )
      $scope.pool.save( $scope.model, {
        error: function(model,xhr,options){
          alert ( "Could not save the pool: " + xhr)
        },
        success: function(model, response, options) {
          $scope.$parent.newPoolKey = model.id
        }
      });
    }
  });


  // Here is where the fun happens. angularAMD contains support for initializing an angular
  // app after the page load.
  angularAMD.bootstrap(notionApp);


  console.log ("Build notion app")
})