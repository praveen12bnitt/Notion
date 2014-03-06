

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
  // deps: ['./foundation', './xtk', 'dat.gui'],
  baseURL: 'js',
  // Some packages do not provide require info, so we 'shim' them here
  shim: {
    'angular': { exports: 'angular'},
    'angular-route': ['angular'],
    'angular-ui-router' : ['angular'],
    // The angularAMD and ngload let us load a page and add angular apps later
    'angularAMD':['angular'],
    'ngload':['angularAMD']
  }
})

// For Grater to work, the model, angular and angularAMD packages are required
require(['angular', 'angularAMD', "Backbone", 'angular-ui-router'], function(angular, angularAMD, Backbone) {
  PoolModel = Backbone.Model.extend({
    idAttribute: "poolKey"
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

  notionApp = angular.module('notionApp', ['ui.router']);

  notionApp.config(function($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise('/pools')
    $stateProvider
    .state('pools', {
      url: "/pools",
      templateUrl: 'partials/pools.html',
      controller: 'PoolsController'
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

  notionApp.controller ( 'PoolsController', function ($scope,$timeout) {
    $scope.poolCollection = new PoolCollection();
    p = $scope.poolCollection;
    (function tick() {
      $scope.poolCollection.fetch({remove: true});
      $timeout(tick, 2000)
    })();
  });



  // Here is where the fun happens. angularAMD contains support for initializing an angular
  // app after the page load.
  angularAMD.bootstrap(notionApp);


  console.log ("Build notion app")
})