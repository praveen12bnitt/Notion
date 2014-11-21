
notionApp.controller ( 'StudyController', function($scope,$http,$timeout,$stateParams, $state, $modal) {
  $scope.pool = $scope.$parent.pool;
  $scope.numberOfItems = 1;
  $scope.pageSize = 50;

  $scope.reload = function(){
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
    $scope.studies = data;
    $scope.numberOfItems = data.TotalRecordCount;
  });
};

$scope.$watch('currentPage', $scope.reload);
$scope.clear = function() {
  $scope.PatientID = "";
  $scope.PatientName = "";
  $scope.AccessionNumber = "";
  $scope.reload();
};
$scope.deleteStudy = function(study) {
  $scope.study = study;
  $modal.open ({
    templateUrl: 'partials/modal.html',
    scope: $scope,
    controller: function($scope, $modalInstance) {
      $scope.title = "Delete study?";
      $scope.message = "Delete study " + study.StudyDescription + " for " + study.PatientName + " / " + study.PatientID + " / " + study.AccessionNumber;
      $scope.ok = function(){
        $http.delete("/rest/pool/" + $scope.pool.get("poolKey") + "/studies/" + study.StudyKey)
        .success(function() {
          $scope.reload();
          $modalInstance.dismiss();
        });
      };
      $scope.cancel = function() { $modalInstance.dismiss(); };
    }
  });
};



});
