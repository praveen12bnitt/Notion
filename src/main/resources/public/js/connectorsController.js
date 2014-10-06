
notionApp.controller ( 'ConnectorsController', function($scope,$timeout,$state,$modal,authorization) {
  console.log("is logged in: ", authorization.isLoggedIn() );
  $scope.poolCollection = new PoolCollection();
  // Make the first one syncrhonous
  $scope.poolCollection.fetch({remove:true, async:false});
  $scope.connectorCollection = new ConnectorCollection();
  $scope.connectorCollection.fetch({async:false});
  $scope.pools = $scope.poolCollection.toJSON();
  $scope.deviceCache = {};

  $scope.getPoolInfo = function(poolKey) {
    var pool = $scope.poolCollection.get(poolKey);
    return pool.get('name') + " / " + pool.get("applicationEntityTitle");
  };

  $scope.getDeviceInfo = function (poolKey, deviceKey) {
    var device;
    if ( deviceKey in $scope.deviceCache ) {
      device = $scope.deviceCache[deviceKey];
    } else {
      device = new DeviceModel();
      device.urlRoot = '/rest/pool/' + poolKey + '/device/' + deviceKey;
      device.fetch({async:false});
      console.log("Got devcie ", device);
      $scope.deviceCache[deviceKey] = device;
    }
    return device.get("applicationEntityTitle") + "@" + device.get("hostName") + ":" + device.get("port");
  };

  $scope.deleteConnector = function(connector) {
    $modal.open ({
      templateUrl: 'partials/modal.html',
      controller: function($scope, $modalInstance) {
        $scope.title = "Delete connector?";
        $scope.message = "Delete the connector: " + connector.get('name');
        $scope.ok = function(){
          connector.destroy({
            success: function(model, response) {
              console.log("Dismissing modal");
              $modalInstance.dismiss();
              $scope.$apply();
            },
            error: function(model, response) {
              alert ( "Failed to delete Connector: " + response.message );
            }
          });
        };
        $scope.cancel = function() { $modalInstance.dismiss(); };
      }
    });
  };

  // Devices
  $scope.editConnector = function(connector) {
    console.log("Edit Connector");
    var newConnector = !connector;
    if ( !connector ) {
      connector = new ConnectorModel();
    }
    $scope.connector = connector;
    $scope.model = connector.toJSON();
    $modal.open ( {
      templateUrl: 'partials/connector.edit.html',
      scope: $scope,
      controller: function($scope, $modalInstance) {
        $scope.devices = [];
        if ( newConnector ) {
          $scope.title = "Create a new connector";
        } else {
          $scope.title = "Edit the connector";
        }
        $scope.queryPoolChanged = function() {
          console.log("get devices");
          // Grab the devices
          var deviceCollection = new DeviceCollection();
          deviceCollection.urlRoot = '/rest/pool/' + $scope.model.queryPoolKey + '/device';
          deviceCollection.fetch({async:false});
          $scope.devices = deviceCollection.toJSON();
        };
        $scope.getDevices = function(poolKey) {
          return $scope.devices;
        };
        if ( !newConnector ) {
          $scope.queryPoolChanged();
        }
        $scope.save = function(){
          connector.set ( $scope.model );
          $scope.connectorCollection.add(connector);
          console.log("saving here!!!!", $scope, $scope.model, connector );
          $scope.connector.save();
          $modalInstance.close();
        };
        $scope.cancel = function() { $modalInstance.dismiss(); };
      }
    });
  };




});
