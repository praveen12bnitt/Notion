
notionApp.controller ( 'PoolController', function($scope,$timeout,$stateParams, $state, $modal, $http, authorization) {
  $scope.pool = $scope.$parent.pool;
  console.log ( "PoolController for ", $stateParams.poolKey );
  console.log ( "Pool is: ", $scope.pool);
  $scope.model = $scope.pool.toJSON();
  $scope.authorization = authorization;
  $scope.okToDelete = false;

  // Grab the devices
  $scope.deviceCollection = new DeviceCollection();
  $scope.deviceCollection.urlRoot = '/rest/pool/' + $stateParams.poolKey + '/device';
  $scope.deviceCollection.fetch({async:false});
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

  $scope.deletePool = function(device) {
    var model = $scope.model;
    $modal.open ({
      templateUrl: 'partials/modal.html',
      controller: function($scope, $modalInstance) {
        $scope.title = "Delete pool " + model.name + "?";
        $scope.message = "This will delete the pool " + model.name + " including all data!  You will no longer be able to send to, receive from, retrieve studies, find the anonymization map or do anything with the images in the pool.  They will be GONE FOREVER.";
        $scope.ok = function(){
          $http.delete ( "/rest/pool/" + model.poolKey )
          .success(function(data,status,headers,config){
              $modalInstance.dismiss();
              $scope.$apply();
          })
          .error(function(data,status,headers,config){
            alert ( "Failed to delete Device: " + data.message );
          });
        };
        $scope.cancel = function() { $modalInstance.dismiss(); };
      }
    });
  };


});
