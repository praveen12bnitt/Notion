


// Helper for shortening strings
String.prototype.trunc = String.prototype.trunc ||
function(n){
  return this.length>n ? this.substr(0,n-1)+'...' : this;
};

String.prototype.startsWith = String.prototype.startsWith ||
function (str){
  return this.indexOf(str) === 0;
};


String.prototype.contains = String.prototype.contains ||
function (str){
  return this.indexOf(str) != -1;
};


// Notifications

notionApp = angular.module('notionApp', ['ui.router', 'ui.bootstrap', 'ui.ace', 'w11k.select', 'w11k.select.template']);

notionApp.config(function($stateProvider, $urlRouterProvider) {
  $urlRouterProvider.otherwise('/pools/index');
  $urlRouterProvider.when('', '/pools/index');

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
.state('root.pools.authorization', {
  url: "/:poolKey/authorization",
  templateUrl: 'partials/pool.authorization.html',
  controller: 'AuthorizationController',
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
  .state('root.groups', {
    url: "/groups",
    templateUrl: 'partials/groups.html',
    controller: 'GroupController',
    data: {
      access: ['admin']
    }
  })
  .state('root.users', {
    url: "/users",
    templateUrl: 'partials/users.html',
    controller: 'UserController',
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



// Extra directive
notionApp.directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if(event.which === 13) {
                scope.$apply(function (){
                    scope.$eval(attrs.ngEnter);
                });

                event.preventDefault();
            }
        });
    };
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
        });



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
              var r = u.username !== null;
              // console.log("isLoggedIn:", u, r);
              return u.username !== null;
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
            };
          });

          notionApp.run(['$rootScope', '$state', 'authorization', function( $rootScope, $state, authorization ) {
            console.log ( "run from notionApp", authorization.isLoggedIn() );

            $rootScope.$on("$stateChangeStart", function(e, toState, toParams, fromState, fromParams) {
              return;
            });
          }]);


          notionApp.controller("RootController", function($scope, $state, authorization,$timeout,$http,$modal,$window) {
            $scope.permission = {};
            var check = {
              'admin' : 'admin:edit'
            };
            $http.post('/rest/user/permission', {permission: check} ).success(function(result) {
              $scope.permission = result;
              console.log("Permissions", result);
            }).error();

            var heartbeat = function() {
              authorization.checkLogin( function() {
                if ( authorization.isLoggedIn() ) {
                  console.log ( "Logged in, all is well!" );
                  // $scope.user = authorization.user
                  $scope.user = $.extend(true, {}, authorization.user);

                  $timeout(heartbeat,60000);
                } else {
                  console.log ( "Not logged in, something is amiss" );
                  $window.location.href = "login.html";
                }
              });
            };
            heartbeat();
            console.log("Creating RootController");
            $scope.name = "RootController";
            $scope.loggedIn = true;


            $scope.settings = function(){
              console.log ( "Configuration of settings");
              $modal.open({
                templateUrl: 'partials/settings.html',
                scope: $scope,
                controller: function($scope,$modalInstance) {
                  console.log("Settings controller");
                  $scope.updateUser = $.extend(true,{},$scope.user);
                  $scope.save = function() {
                    $http.put("/rest/user/update", $scope.updateUser)
                    .success(function() {
                      $.extend(true, $scope.user, $scope.updateUser);
                      $modalInstance.dismiss();
                    }
                  );
                };
                $scope.close = function() { $modalInstance.dismiss(); };
              }
            });
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
          };
        });


        notionApp.controller ( 'PoolsController', function($scope,$timeout,$state,$modal, authorization, $http) {
          console.log("Creating PoolsController");
          $scope.name = "PoolsController";
          $scope.poolCollection = new PoolCollection();
          // Make the first one syncrhonous
          $scope.poolCollection.fetch({remove:true, async:false});

          $scope.permission = {};

          $scope.fetchPermissions = function() {

            for ( var i = 0; i < $scope.poolCollection.length; i++ ) {
              var poolKey = $scope.poolCollection.at(i).get('poolKey');
              var check = {
                'admin' : 'pool:admin:' + poolKey,
                'coordinator' : 'pool:coordinator:' + poolKey,
                'edit' : 'pool:edit:' + poolKey,
                'query' : 'pool:query:' + poolKey,
                'download' : 'pool:download:' + poolKey
              };
              $http.post('/rest/user/permission', {permission: check}, { key: poolKey } ).success(function(result, status, headers, config) {
                var poolKey = config.key;
                $scope.permission[poolKey] = result;
                console.log(config.key + "Permissions for " + poolKey, result);
              }).error();
            }
          };
          $scope.fetchPermissions();

          p = $scope.poolCollection;
          $scope.newPoolKey = false;

          $scope.refresh = function() {
            $scope.poolCollection.fetch({remove:true, success: function() {
              $scope.fetchPermissions();
            }});
          };


          $scope.newPool = function() {
            $modal.open ({
              templateUrl: 'partials/pool.edit.html',
              scope: $scope,
              controller: function($scope,$modalInstance) {
                $scope.pool = new PoolModel();
                $scope.model = $scope.pool.toJSON();
                $scope.title = "Create a new pool";
                $scope.hideAETitle = false;
                $scope.save = function() {
                  $scope.pool.set ( $scope.model );
                  $scope.poolCollection.add( $scope.pool);
                  $scope.pool.save ( $scope.model ).done ( function(data) {
                    toastr.success ("Saved new pool");
                    $scope.refresh();
                  })
                  .fail ( function ( xhr, status, error ) {
                    toastr.error ( "Failed to save pool: " + status );
                  });

                  $modalInstance.close();
                };
                $scope.cancel = function() { $modalInstance.dismiss(); };
              }
            });
          };

        });

        notionApp.controller ( 'PoolController', function($scope,$timeout,$stateParams, $state, $modal, $http) {
          $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey);
          console.log ( "PoolController for ", $stateParams.poolKey );
          console.log ( "Pool is: ", $scope.pool);
          $scope.model = $scope.pool.toJSON();

          // Grab the devices
          $scope.deviceCollection = new DeviceCollection();
          $scope.deviceCollection.urlRoot = '/rest/pool/' + $stateParams.poolKey + '/device';
          $scope.deviceCollection.fetch({async:false});
          console.log( $scope.deviceCollection );
          pool = $scope.pool;
          devices = $scope.deviceCollection;

          $scope.edit = function() {
            $modal.open ( {
              templateUrl: 'partials/pool.edit.html',
              scope: $scope,
              controller: function($scope, $modalInstance) {
                $scope.title = "Edit " + $scope.pool.get('name');
                $scope.hideAETitle = true;
                $scope.save = function() {
                  $scope.pool.save ( $scope.model );
                  $modalInstance.close();
                };
                $scope.cancel = function() { $modalInstance.dismiss(); };
              }
            });
          };

          // Devices
          $scope.editDevice = function(device) {
            console.log("EditDevice");
            var newDevice = !device;
            if ( !device ) {
              console.log("Create new device");
              device = new DeviceModel();
            }
            $scope.device = device;
            $scope.deviceModel = device.toJSON();
            $modal.open ( {
              templateUrl: 'partials/device.edit.html',
              scope: $scope,
              controller: function($scope, $modalInstance) {
                if ( newDevice ) {
                  $scope.title = "Create a new device";
                } else {
                  $scope.title = "Edit the device";
                }
                $scope.save = function(){
                  device.set ( $scope.deviceModel );
                  $scope.deviceCollection.add(device);
                  device.save();
                  $modalInstance.close();
                };
                $scope.cancel = function() { $modalInstance.dismiss(); };
              }
            });
          };

          $scope.deleteDevice = function(device) {
            $scope.device = device;
            $scope.deviceModel = device.toJSON();
            $modal.open ({
              templateUrl: 'partials/modal.html',
              controller: function($scope, $modalInstance) {
                $scope.title = "Delete device?";
                $scope.message = "Delete the device: " + device.format();
                $scope.ok = function(){
                  device.destroy({
                    success: function(model, response) {
                      console.log("Dismissing modal");
                      $modalInstance.dismiss();
                      $scope.$apply();
                    },
                    error: function(model, response) {
                      alert ( "Failed to delete Device: " + response.message );
                    }
                  });
                };
                $scope.cancel = function() { $modalInstance.dismiss(); };
              }
            });
          };

          // Test a DICOM triplet against all the defined devices
          $scope.testResult = { query: false, retrieve: false, store: false };
          $scope.testDeviceMatch = function() {

            var re = /([^@]*)@([^:]*):(\d*)/;
            var t = re.exec ( $scope.testDeviceFull);
            console.log(t);
            if ( t === null || t.length != 4 ) {
              toastr.error ("Could not parse DICOM information: " + $scope.testDeviceFull + "<br> Should be AET@Hostname:port");
              return;
            }
            var testDevice = { applicationEntityTitle: t[1], hostName: t[2], port: t[3]};
            console.log("checking " + $scope.testDevice + " to server");
            $http.post ( '/rest/pool/' + $scope.pool.get('poolKey') + '/device/match', testDevice)
            .success(function(data,status){
              console.log("Got test data back", data);
              $scope.testResult = data;
             });
          };

          // CTP Configuration
          $scope.ctp = new CTPModel();
          $scope.ctp.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/ctp';
          $scope.ctp.fetch({async:false});
          $scope.ctpScript = $scope.ctp.get("script");
          $scope.saveCTP = function() {
            $scope.ctp.set('script', $scope.ctpScript);
            $scope.ctp.sync("update", $scope.ctp).done ( function(data) {
              toastr.success ( "Saved CTP script" );
            })
            .fail ( function ( xhr, status, error ) {
              toastr.error ( "Failed to save script: " + status );
            });
          };

          // Scripts
          $scope.scriptModel = new ScriptModel();
          $scope.scriptModel.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/script';
          $scope.scriptModel.fetch({async:false});
          $scope.script = $scope.scriptModel.get("script");
          console.log("Got script: ", $scope.scriptModel);

          $scope.saveScript = function(){
            console.log ( "Saving script ", $scope.script);
            $scope.scriptModel.set("script", $scope.script);
            $scope.scriptModel.sync('update', $scope.scriptModel).done ( function(data) {
              toastr.success ( "Saved Anonymization script" );
            })
            .fail ( function ( xhr, status, error ) {
              toastr.error ( "Failed to save script: " + status );
            });
          };
          $scope.aceLoaded = function(editor) {
            console.log ( "Ace loaded" );
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

          $scope.tryScript = function() {
            // Cal the rest API and give it a go
            console.log ( "Trying script on the server");
            $scope.scriptModel.set("script", $scope.script);
            var url = "/rest/pool/" + $scope.pool.get('poolKey') + '/script/try';
            $.ajax ({
              contentType: 'application/json',
              type: 'PUT',
              url: url,
              data: JSON.stringify($scope.scriptModel),
              success: function ( data ) {
                console.log("Tried script, got back ", data);
                $scope.$apply ( function() {
                  $scope.tryResult = data.result;
                  toastr.success("Script result: " + data.result);
                });
              }
            });
          };

        });


        console.log ("Build notion app");
