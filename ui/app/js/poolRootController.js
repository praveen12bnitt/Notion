
notionApp.controller ( 'PoolRootController', function($scope,$timeout,$stateParams, $state, $modal, $http) {
  $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey);
  if ( $scope.pool === undefined ) {
    $state.go("^.^.index");
  } else {
    $scope.$state = $state;
  }

});
