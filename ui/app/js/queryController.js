
notionApp.controller ( 'QueryController', function($scope,$timeout,$stateParams, $state, $modal, $http) {
  $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey);
  console.log ( "QueryController for ", $stateParams.poolKey );
  console.log ( "Pool is: ", $scope.pool);
  $scope.model = $scope.pool.toJSON();
  $scope.pools = $scope.$parent.poolCollection.toJSON();
  $scope.item = {};

  // modes are:
  // 'setup' - not fetching, configuring the query
  // 'query-pending' - letting the query run
  // 'query-done' - query is done
  // 'fetch-pending' - fetching
  // 'fetch-done' - done fetching
  $scope.mode = 'setup';
  $scope.query = null;

  $scope.connectorCollection = new ConnectorCollection();
  $scope.connectorCollection.fetch({async:false});
  $scope.connectors = $scope.connectorCollection.toJSON();
  $scope.connectorKey = $scope.connectors[0].connectorKey;

  console.log("Getting prior queries!");
  $scope.queries = new QueryCollection();
  $scope.queries.urlRoot = 'rest/pool/' + $scope.pool.get('poolKey') + "/query";
  $scope.queries.fetch({
    success: function() { $timeout(function(){$scope.$apply();},0); }
  });

  $scope.refresh = function(){
    console.log ( $scope.query );
    $scope.query.fetch({
      success: function() { console.log("Fetched query"); $scope.$apply(); }
    });
  };

  $scope.selectQuery = function(id) {
    $scope.query = $scope.queries.get(id);
    $scope.query.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/query/' + $scope.query.get('queryKey');
    $scope.mode = 'query-pending';
  };

  $scope.fetchAll = function(item){
    $.each(item.items, function(index,value){
      item.items[index].doFetch = true;
    });
  };

  $scope.toggleFetch = function(item) {
    item.doFetch = !item.doFetch;
  };

  var successCallback = function(data) {
    console.log("Create query");
    $scope.query = new QueryModel(data);
    $scope.query.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/query/' + $scope.query.get('queryKey');
    $scope.mode = 'query-pending';
    queryTick();
  };

  $scope.submitIndividual = function() {
    var data = {
      items: [$scope.item],
      connectorKey: $scope.connectorKey
    };
    $http.put("/rest/pool/" + $scope.pool.get('poolKey') + "/query/simple", data).
    success(successCallback).
    error(function(data){
      alert ( "Could not construct query:" + data.message);
    });
  };

  $scope.submit = function() {
    console.log ( $('#queryFile')[0].files[0]);
    var formData = new FormData();
    console.log ( formData );
    formData.append('file', $('#queryFile')[0].files[0] );
    formData.append('connectorKey', $scope.connectorKey);
    console.log ( formData );

    $.ajax({
      url: '/rest/pool/' + $scope.pool.get('poolKey') + '/query',
      type: 'POST',
      data: formData,
      processData: false,
      contentType: false,
      success: successCallback,
      error: function(xhr, status, error) {
        alert ( "Could not construct query: " + xhr.responseText );
      }
    });
  };

  var queryTick = function(){
    // Actually fetch only when mode is '*-pending'
    if ( $scope.query && $scope.mode.match('pending') ) {
      // console.log("queryTick")
      $scope.query.fetch().done(function() {
        // console.log ("queryTick completed")
        if ($scope.query.get('status').match("query completed")) {
          $scope.mode = 'query-done';
        }
        if ($scope.query.get('status').match("fetch completed")) {
          $scope.mode = 'fetch-done';
        }
      });
    }
    $timeout(queryTick, 2000);
  };
  $scope.reset = function() {
    $scope.query = null;
    $scope.mode = 'setup';
    $scope.queries.fetch({
      success: function() { $timeout(function(){$scope.$apply();},0); }
    });
  };

  $scope.fetch = function(){
    $scope.query.save()
    .done( function(model,response, options) {
      console.log("Saved query", $scope.query);
      $scope.mode = 'fetch-pending';
      $.ajax({
        url: $scope.query.urlRoot + "/fetch",
        type: 'PUT',
        data: {},
        success: function(data){
          console.log("started ticker");
          $scope.refresh();
        }
      });
    })
    .fail( function(model,response,options) {
      $scope.error = "Failed to start fetch!";
      console.log("fetch failed", response);
    }
  );
};

$scope.doQuery = function(){
  console.log("doQuery! " + $scope.query.urlRoot);
  $.ajax({
    url: $scope.query.urlRoot + "/query",
    type: 'PUT',
    data: {},
    success: successCallback
  });
};



});
