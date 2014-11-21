
        notionApp.controller ( 'PoolsController', function($scope,$timeout,$state,$modal, authorization, $http) {
          console.log("Creating PoolsController");
          $scope.name = "PoolsController";
          $scope.poolCollection = new PoolCollection();
          $scope.poolCollection.fetch({async: false} );
          $scope.$state = $state;
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

          $scope.newPoolKey = false;

          $scope.refresh = function() {
            console.log ( "calling refresh on ", $scope.poolCollection)
            $scope.poolCollection.fetch({remove:true, success: function() {
              $scope.fetchPermissions();
              p = $scope.poolCollection;

            }});
          };
          $scope.refresh();

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
