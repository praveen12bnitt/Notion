
        notionApp.controller ( 'PoolRootController', function($scope,$timeout,$stateParams, $state, $modal, $http) {
          $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey);
          $scope.$state = $state;
          console.log ( "State is: ", $state)


          console.log ( "pool prior state is: ", $scope.pool.get("lastState"));
          if ( $scope.pool.get("lastState")) {
            // $state.go ( $scope.pool.get("lastState"));
          }
          // $scope.$on('$stateChangeSuccess', function( event, toState, toParams, fromState, fromParams) {
          //   console.log("transitioned state!!!", toState);
          //   $scope.pool.set("lastState", toState.name);
          // });


        });
