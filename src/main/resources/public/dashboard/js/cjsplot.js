(function () {
  freeboard.addStyle('.chart-widget', "width:100%; height:100%;");
  var chartCounter = 1;
  var cjsPlotWidget = function (settings) {

    var self = this;
    var currentSettings = settings;
    var htmlElement;
    var data;
    var options;
    var chartHeight = 300;
    var chartWidth = 300;
    var chartID = "mq-" + chartCounter++;
    var chart;

    //add the chart div to the dom
    var chartWrapper = $('<div class="chart-widget"></div>');
    var chartHTML = '<canvas id="' + chartID + '" ></canvas>';
    var chartDiv = $(chartHTML);

    //seems to be called once (or after settings change)
    this.render = function (element) {
      console.log('render');
      $(element).append(chartWrapper).append(chartDiv)
      // $(valueElement).append(chartDiv);
      console.log(chartDiv);
    }

    this.onSettingsChanged = function (newSettings) {
      currentSettings = newSettings;
    }

    //seems to be called after render whenever a calculated value changes
    this.onCalculatedValueChanged = function (settingName, newValue) {
      console.log('onCalculatedValueChanged for ' + settingName, newValue);

      if (settingName == 'data') {
        data = newValue;
      }

      if (settingName == 'options') {
        options = newValue;
      }
      //render the chart
      console.log("ReRender", data)

      try {


        if ( chart !== undefined ) {
          chart.destroy();
        }

        var ctx = document.getElementById(chartID).getContext("2d");
        console.log("Resized ChartWrapper! ", $(chartWrapper).width(), $(chartWrapper).height())
        document.getElementById(chartID).width = $(chartWrapper).width();
        document.getElementById(chartID).height = currentSettings.chartHeight;

        console.log("Chart: ", ctx);
        if ( currentSettings.chartType === "bar" ) {
          chart = new Chart(ctx).Bar(data, currentSettings.options);

        }
        if ( currentSettings.chartType == "line") {
        chart = new Chart(ctx).Line(data, currentSettings.options);
      }
      } catch(e) {
        console.log("Caught error in Chart.js: ", e)
      }
      console.log("Finished Rendering")

      // $.jqplot(currentSettings.id, data, options);
    }

    this.onDispose = function () {
    }

    this.getHeight = function () {
      return 5;
    }

    this.onSettingsChanged(settings);
  };  // End of mgPlotWidget

  freeboard.loadWidgetPlugin({
    "type_name": "cjsPlotWidget",
    "display_name": "Chart.js Graphics Plot",
    "fill_size": true,
    "external_scripts": [
    "//cdnjs.cloudflare.com/ajax/libs/Chart.js/1.0.1/Chart.min.js"    ],
    "settings": [

{
  "name": "data",
  "display_name": "Chart Data",
  "type": "calculated",
  "description": "The data to plot"
},
{
  "name": "chartHeight",
  "display_name": "Chart Height (px)",
  "type": "number",
  "default_value": 300,
  "description": "chart height in pixels"
},
{
  "name": "chartType",
  "display_name": "Chart Type",
  "type": "option",
  "options" : [
{ "name" : "Bar", "value": "bar"},
{ "name" : "Line", "value": "line"},
]

}
],
newInstance: function (settings, newInstanceCallback) {
  newInstanceCallback(new cjsPlotWidget(settings));
}
});

}());
