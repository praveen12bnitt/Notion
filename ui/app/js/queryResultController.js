notionApp.controller ( 'QueryResultController', function($scope,$timeout,$stateParams, $state, $modal, $http) {


  $scope.pool = $scope.$parent.pool;
  console.log ( "QueryResultController for pool: " + $stateParams.poolKey + " result key: " + $stateParams.queryKey );
  console.log ( "Pool is: ", $scope.pool);
  $scope.model = $scope.pool.toJSON();
  $scope.pools = $scope.$parent.poolCollection.toJSON();
  $scope.item = {};
  $scope.ordering = "PatientID";
  $scope.sort = {
    column: 'studyDate',
    descending: false
  };

  $scope.autodownload = false;

  console.log("Getting query!");
  $scope.query = new QueryModel();
  $scope.query.set('poolKey', $scope.pool.get('poolKey'));
  $scope.query.set('queryKey', $stateParams.queryKey );
  $scope.query.urlRoot = 'rest/pool/' + $stateParams.poolKey + '/query/' + $stateParams.queryKey;
  $scope.query.fetch({
    success: function( data ) {
      $scope.query = data;
      $timeout(function(){
        $scope.$apply();
        console.log ( "Got Query: ", $scope.query );
        queryTick();
      },0);
    }
  });



  // modes are:
  // 'setup' - not fetching, configuring the query
  // 'query-pending' - letting the query run
  // 'query-done' - query is done
  // 'fetch-pending' - fetching
  // 'fetch-done' - done fetching
  $scope.mode = 'pending';

  $scope.refresh = function(){
    console.log ( $scope.query );
    $scope.query.fetch({
      success: function() { console.log("Fetched query"); $scope.$apply(); }
    });
  };

  var queryTick = function(){
    // Actually fetch only when mode is '*-pending'
    if ( $scope.query && $scope.mode.match('pending') ) {
      // console.log("queryTick");
      $scope.query.fetch().done(function() {
        // console.log ("queryTick completed")
        if ($scope.query.get('status').match("query completed")) {
          $scope.mode = 'query-done';
        }
        if ($scope.query.get('status').match("fetch completed")) {
          $scope.mode = 'fetch-done';
          if ( $scope.autodownload )  {
            // Start the autodownload
            $scope.download();
          }
        }
      });
    }
    $timeout(queryTick, 2000);
  };
  $scope.reset = function() {
    $state.go ( "^.query", { queryKey: $scope.query.get('QueryKey')});
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
          $scope.sort.column = 'status';
          $scope.sort.descending = true;
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

$scope.download = function() {
  // Have the browser download the completed fetch
  var url = "/rest/pool/" + $scope.pool.get('poolKey') + "/query/" + $scope.query.get('queryKey') + "/zip";
  downloadFile ( url );
};


$scope.doQuery = function() {
  // Start the query by doing an empty PUT on the query url
  $.ajax({
    url: $scope.query.urlRoot + "/query",
    type: 'PUT',
    data: {},
    success: successCallback
  });
  $scope.mode = 'query-pending';
  queryTick();
};

var successCallback = function(data) {
  console.log("Create query");
  $scope.mode = 'query-pending';
  queryTick();
};

$scope.selectAll = function() {
  $.each($scope.query.get('items'), function(index,value) {
    $scope.fetchAll(value);
  });
};

$scope.fetchAll = function(item){
  $.each(item.items, function(index,value){
    item.items[index].doFetch = true;
  });
};

$scope.toggleFetch = function(item) {
  item.doFetch = !item.doFetch;
};

$scope.getOrdering = function() {
  return $scope.sort.column;
};
$scope.setOrderBy = function(o) {
  if ( o === $scope.sort.column ) {
    $scope.sort.descending = !$scope.sort.descending;
  }
  $scope.sort.column = o;
};
$scope.sortingBy = function(o) {
  return $scope.sort.column == o;
};



});
