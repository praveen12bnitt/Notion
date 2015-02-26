console.log("Loading googleChart")
var googleChartIdCounter = 0;
freeboard.addStyle('.google-chart', "width:100%; height:300px;");

var googlePlotWidget = function (settings) {

  var self = this;
  var currentSettings = settings;
  var chartId = "googleChart-" + googleChartIdCounter++;
  var htmlElement;
  var data;
  var options;
  var chart;

  this.render = function (element) {
    console.log("Rendering a Google Chart");

    //add the chart div to the dom
    var chartDiv = '<div id="' + chartId + '" class="google-chart"></div>';
    htmlElement = $(chartDiv);
    $(element).append(htmlElement);
    try {
      if ( currentSettings.chartType == "column") {
        chart = new google.visualization.ColumnChart(document.getElementById(chartId));
      }
      if ( currentSettings.chartType == "gauge") {
        chart = new google.visualization.Gauge(document.getElementById(chartId));
      }
      if ( currentSettings.chartType == "line") {
        chart = new google.visualization.LineChart(document.getElementById(chartId));
      }
    } catch ( e) { console.log ("Caught error creating Google Chart: ", e)}
    console.log ("Build a " + currentSettings.chartType + " chart");
  }

  this.onSettingsChanged = function (newSettings) {
    currentSettings = newSettings;
  }

  //seems to be called after render whenever a calculated value changes
  this.onCalculatedValueChanged = function (settingName, newValue) {

    if (settingName == 'data') {
      data = newValue;
    }

    if (settingName == 'options') {
      options = newValue;
    }

    chart.draw(data,options);
  }

  this.onDispose = function () {
  }

  this.getHeight = function () {
    console.log("GoogleChart returning height");
    return 5;
  }

  this.onSettingsChanged(settings);
};

freeboard.loadWidgetPlugin({
  "type_name": "googlePlotWidget",
  "display_name": "Google Chart",
  "fill_size": true,
  "settings": [
    {
      "name": "chartType",
      "display_name": "Chart Type",
      "type": "option",
      "description": "See https://google-developers.appspot.com/chart/interactive/docs/gallery for examples",
      "options" : [
        { "name" : "Column", "value": "column"},
        { "name" : "Gauge", "value": "gauge"},
        { "name" : "Line", "value": "line"}
      ]
    },
    {
      "name": "data",
      "display_name": "Chart Data",
      "type": "calculated",
      "description": "The data to plot, must be a Google Chart DataTable"
    },
    {
      "name": "options",
      "display_name": "Chart Options",
      "type": "calculated",
      "description": "JavaScript object containing options for chart"
    },
    {
      "name": "height",
      "display_name": "Height Blocks",
      "type": "number",
      "default_value": 5,
      "description": "A height block is around 60 pixels"
    }
  ],
  newInstance: function (settings, newInstanceCallback) {
    newInstanceCallback(new googlePlotWidget(settings));
  }
});
