
notionApp = angular.module('notionApp', ['ui.router', 'ui.bootstrap', 'ui.ace']);

notionApp.config(function($stateProvider, $urlRouterProvider) {
  $urlRouterProvider.otherwise('/login');
  $stateProvider
  .state('root', {
    abstract: true,
    url: '',
    templateUrl: 'partials/root.html',
    controller: 'RootController'
  });
  $stateProvider
  .state('root.login', {
    url: '/login',
    templateUrl: 'partials/login.html',
    controller: 'LoginController'
  });
  $stateProvider
  .state('root.register', {
    url: '/register',
    templateUrl: 'partials/register.html',
    controller: 'RegisterController'
  });

});

notionApp.controller("RootController", function($scope, $state) {
});

notionApp.controller ( "LoginController", function ( $scope, $state, $timeout,$location,$http,$window) {
  $scope.allowRegistration = false;
  $http.get('/rest/user/').success(function(data){
    console.log("user", data);
    $scope.allowRegistration = data.allowRegistration;
  });
  $scope.login = function() {
    $http.post('/rest/user/login', $scope.user ).success(function(result) {
      console.log("logged in");
      $location.url("notion.html");
      $window.location.href = "./";
    }).error( function(result) {
      console.log("login failed");
      $scope.error = result.message;
    });
  };


});

notionApp.controller ( "RegisterController", function ( $scope, $state, $http, $location, $window) {
  $scope.register = function() {
    console.log ( "Register with ", $scope.user);
    $http(
      {
        url:"/rest/user/register",
        method: "POST",
        data: $.param($scope.user),
        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
      }).success(function(data, status, headers, config) {
        console.log(data);
        $location.url("notion.html");
        $window.location.href = "./";
      }).error(function(data, status, headers, config) {
        $scope.error = data.message;
      });
    };
  }
);


console.log ("Built notion app");
