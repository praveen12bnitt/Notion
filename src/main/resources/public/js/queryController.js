
notionApp.controller ( 'QueryController', function($scope,$timeout,$stateParams, $state, $modal) {
  $scope.pool = $scope.$parent.poolCollection.get($stateParams.poolKey)
  console.log ( "QueryController for ", $stateParams.poolKey )
  console.log ( "Pool is: ", $scope.pool)
  $scope.model = $scope.pool.toJSON();
  $scope.pools = $scope.$parent.poolCollection.toJSON();

  // modes are:
  // 'setup' - not fetching, configuring the query
  // 'query-pending' - letting the query run
  // 'query-done' - query is done
  // 'fetch-pending' - fetching
  // 'fetch-done' - done fetching
  $scope.mode = 'setup'

  $scope.connectorCollection = new ConnectorCollection();
  $scope.connectorCollection.fetch({async:false});
  $scope.connectors = $scope.connectorCollection.toJSON();

  $scope.refresh = function(){
    console.log ( $scope.query )
    $scope.query.fetch({'async':false});
  };

  $scope.fetchAll = function(item){
    $.each(item.items, function(index,value){
      item.items[index].doFetch = true
    })
  };

  $scope.toggleFetch = function(item) {
    item.doFetch = !item.doFetch
  }

  $scope.submit = function() {
    console.log ( $('#queryFile')[0].files[0])
    var formData = new FormData();
    console.log ( formData )
    formData.append('file', $('#queryFile')[0].files[0] )
    formData.append('connectorKey', $scope.connectorKey)
    console.log ( formData )

    $.ajax({
      url: '/rest/pool/' + $scope.pool.get('poolKey') + '/query',
      type: 'POST',
      data: formData,
      processData: false,
      contentType: false,
      success: function(data) {
        $scope.$apply ( function(){
          console.log("Create query")
          $scope.query = new QueryModel(data);
          $scope.query.urlRoot = '/rest/pool/' + $scope.pool.get('poolKey') + '/query/' + $scope.query.get('queryKey');
          $scope.mode = 'query-pending'
          queryTick();
        })
      },
      error: function(xhr, status, error) {
        alert ( "Query failed: " + xhr.responseText )
      }
    });
  };

  var queryTick = function(){
    // Actually fetch only when mode is '*-pending'
    if ( $scope.query && $scope.mode.match('pending') ) {
      // console.log("queryTick")
      $scope.query.fetch().done(function() {
        // console.log ("queryTick completed")
if ($scope.query.get('status').match("Query Completed")) {
  $scope.mode = 'query-done'
}
if ($scope.query.get('status').match("Fetch Completed")) {
  $scope.mode = 'fetch-done'
}
      });
    }
    $timeout(queryTick, 2000)
  };
  $scope.reset = function() {
    $scope.query = null;
    $scope.mode = 'setup';
  }

  $scope.fetch = function(){
    $scope.query.save()
    .done( function(model,response, options) {
      console.log("Saved query", $scope.query)
      $scope.mode = 'fetch-pending'
      $.ajax({
        url: $scope.query.urlRoot + "/fetch",
        type: 'PUT',
        data: {},
        success: function(data){
          console.log("started ticker")
          $scope.refresh();
        }
      });
    })
    .fail( function(model,response,options) {
      $scope.error = "Failed to start fetch!"
      console.log("fetch failed", response)
    }
  );

};

});
