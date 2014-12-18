
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
                  // console.log ( "Logged in, all is well!" );
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
