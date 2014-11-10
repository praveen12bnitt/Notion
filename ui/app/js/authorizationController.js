console.log("In authorizationController.js");

notionApp.controller ( 'AuthorizationController', function($scope,$timeout,$stateParams, $state, $modal, $http) {
  $scope.name = "AuthorizationController";

  $scope.groupRoleCollection = new GroupRoleCollection();
  $scope.groupRoleCollection.urlRoot = "/rest/pool/" + $stateParams.poolKey + "/grouprole";
  $scope.groupRoleCollection.fetch().done( function(d) { $scope.$apply(); });
  $scope.groupCollection = new GroupCollection();
  $scope.groupCollection.fetch().done ( function(d) { $scope.$apply(); });


  $scope.editGroupRole = function(groupRole) {
    console.log("Edit groupRole");
    var newGroupRole = !groupRole;
    if ( !groupRole ) {
      console.log("Create new groupRole");
      groupRole = new GroupRoleModel();
      groupRole.set("poolKey", $stateParams.poolKey);
    }
    $scope.groupRole = groupRole;
    $scope.model = groupRole.toJSON();
    $modal.open ( {
      templateUrl: 'partials/grouprole.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        if ( newGroupRole ) {
          $scope.title = "Create a new Authorization";
        } else {
          $scope.title = "Edit the Authorization";
        }
        $scope.save = function(){
          if ( !$scope.model.groupKey ) {
            alert ( "please set a group");
            return;
          }
          groupRole.set ( $scope.model );
          $scope.groupRoleCollection.add(groupRole);
          groupRole.save();
          $modalInstance.close();
        };
        $scope.cancel = function() { $modalInstance.dismiss(); };
      }
    });
  };


  $scope.deleteGroupRole = function(group) {
    $modal.open ({
      templateUrl: 'partials/modal.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        $scope.title = "Delete group authorization?";
        $scope.message = "Delete the group authorization for: " + $scope.groupCollection.get(group.get('groupKey')).get('name');
        $scope.ok = function(){
          $scope.groupRoleCollection.remove(group);
          group.destroy({
            success: function(model, response) {
              console.log("Dismissing modal");
              $modalInstance.dismiss();
            },
            error: function(model, response) {
              alert ( "Failed to delete group authorization: " + response.message );
            }
          });
        };
        $scope.cancel = function() { $modalInstance.dismiss(); };
      }
    });
  };


});
