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


require(['angular', 'angularAMD', "backbone", 'angular-ui-router', 'ui-bootstrap-tpls', 'ui-ace', 'ace/ace'], function(angular, angularAMD, Backbone ) {

notionApp = angular.module('notionApp', ['ui.router', 'ui.bootstrap', 'ui.ace']);

notionApp.config(function($stateProvider, $urlRouterProvider) {
  $urlRouterProvider.otherwise('/login')
  $stateProvider
  .state('root', {
    abstract: true,
    url: '',
    templateUrl: 'partials/root.html',
    controller: 'RootController'
  })
  $stateProvider
    .state('root.login', {
      url: '/login',
      templateUrl: 'partials/login.html',
      controller: 'LoginController'
    })
  $stateProvider
    .state('root.register', {
      url: '/register',
      templateUrl: 'partials/register.html',
      controller: 'RegisterController'
    })

});

    notionApp.controller("RootController", function($scope, $state) {
    })

    notionApp.controller ( "LoginController", function ( $scope, $state, $timeout,$location,$http,$window) {
	$scope.login = function() {
	    $http.post('/rest/user/login', $scope.user ).success(function(result) {
		console.log("logged in")
		$location.url("index.html");
		$window.location.href = "./";
	    }).error( function(result) {
		console.log("login failed");
		$scope.error = result.message
	    });
	}
	    
	
    });

    notionApp.controller ( "RegisterController", function ( $scope, $state) {
});

// Here is where the fun happens. angularAMD contains support for initializing an angular
// app after the page load.
angularAMD.bootstrap(notionApp);


console.log ("Build notion app")
})
