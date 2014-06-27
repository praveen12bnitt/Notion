
notionApp.controller ( 'GroupController', function($scope,$timeout,$stateParams, $state, $modal, $http, $timeout) {
  $scope.name = "GroupController";
  $scope.users = [];
  $http.get("/rest/authorization/users").
  success(function(data) {
    $scope.users = data.users;
  });
  $scope.groupCollection = new GroupCollection()
  $scope.updateModel = function() {
    console.log("Success from fetch")
    $scope.groups = $scope.groupCollection.toJSON()
    $timeout(function() {
      for ( var i = 0; i < $scope.groupCollection.length; i++ ) {
        $scope.groups[i].userKeys = $scope.groupCollection.models[i].get("userKeys");
      }
    })
  }

  $scope.groupCollection.fetch({
    success: $scope.updateModel
  });

  $scope.saveGroup = function ( group ) {
    var g = new GroupModel();
    g.url = "/rest/authorization/group/" + group.groupKey
    g.set( group )
    g.save();
  }

  $scope.talkToMe = function() {
    console.log("Collection: ", $scope.groupCollection)
    console.log("Model:", $scope.groups)
  }

  $scope.editGroup = function(group) {
    console.log("EditGroup")
    var newGroup = !group
    if ( !group ) {
      console.log("Create new group")
      group = new GroupModel()
    }
    $scope.group = group
    $scope.groupModel = group.toJSON()
    $modal.open ( {
      templateUrl: 'partials/group.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        if ( newGroup ) {
          $scope.title = "Create a new group"
        } else {
          $scope.title = "Edit the group"
        }
        $scope.save = function(){
          group.set ( $scope.groupModel )
          $scope.groupCollection.add(group);
          group.save();
          $scope.updateModel()
          $modalInstance.close();
        };
        $scope.cancel = function() { $modalInstance.dismiss() };
      }
    });
  };


  $scope.staticConfig = {
    disabled: false,
    header: {
      placeholder: 'add users to the group'
    },
    required: true,
    multiple: true
  };

});
