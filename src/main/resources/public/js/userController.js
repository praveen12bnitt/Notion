console.log("in UserController")
notionApp.controller ( 'UserController', function($scope,$timeout,$stateParams, $state, $modal, $http, $timeout) {
  $scope.name = "UserController";
  $scope.userCollection = new UserCollection();
  $scope.userCollection.fetch({
    success: function() { $scope.$apply() }
  });
  $scope.toggleAdmin = function ( user ) {
    user.set ( 'isAdmin', !user.get('isAdmin'))
    user.save()
    .done ( function(data) {
      toastr.success ("Saved " + user.get('username') + " " + user.get("email"))
    })
    .fail ( function ( xhr, status, error ) {
      toastr.error ( "Failed to save: " + status );
    });
  }
});
