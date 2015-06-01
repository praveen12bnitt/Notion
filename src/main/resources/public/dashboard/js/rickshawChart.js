var globalChart;

(function () {
  freeboard.addStyle('.chart-widget', "width:100%; height:100%;");
  freeboard.addStyle('.rickshaw-x-axis', "position:relative; left:0px; height:40px");
  var chartCounter = 1;
  var rickshawPlotWidget = function (settings) {

    var self = this;
    var currentSettings = settings;
    var htmlElement;
    var data = [ {
      data: [ { x: 0, y: 120 }, { x: 1, y: 890 }, { x: 2, y: 38 }, { x: 3, y: 70 }, { x: 4, y: 32 } ],
      color: "#c05020"
    }, {
      data: [ { x: 0, y: 80 }, { x: 1, y: 200 }, { x: 2, y: 100 }, { x: 3, y: 520 }, { x: 4, y: 133 } ],
      color: "#30c020"
    }];
    var options;
    var chartHeight = 300;
    var chartWidth = 300;
    var chartID = "rickshaw-" + chartCounter++;
    var chart;

    //add the chart div to the dom
    var chartWrapper = $('<div class="chart-widget"></div>');
    var chartHTML = '<canvas id="' + chartID + '" ></canvas>';
    var tmp = '<div id="' + chartID + '" ></div>';
    var chartDiv = $(tmp);
    tmp = '<div class="rickshaw-x-axis" id="' + chartID+'-x'+ '"></div>';
    var xAxis = $(tmp)

    //seems to be called once (or after settings change)
    this.render = function (element) {
      console.log('render');
      chartWrapper.append(chartDiv).append(xAxis);
      $(element).append(chartWrapper)
      chart = new Rickshaw.Graph({
        element: document.getElementById(chartID),
        height: currentSettings.height,
        width: 300,
        series: data
      });
      chart.render();


      // var yAxis = new Rickshaw.Graph.Axis.Y( {
      //   graph: chart,
      //   ticksTreatment: 'glow',
      //   element: document.getElementById(chartID+"-x")
      // } );
      // render it 
      // yAxis.render();

      globalChart = chart;
      console.log(chartDiv);
    }

    this.onSettingsChanged = function (newSettings) {
      currentSettings = newSettings;
    }

    //seems to be called after render whenever a calculated value changes
    this.onCalculatedValueChanged = function (settingName, newValue) {
      console.log('onCalculatedValueChanged for ' + settingName, newValue);

      if (settingName == 'data') {
        // Remove the elements of the data
        data.splice (0, data.length-1);
        newValue.forEach ( function(element){
          data.push(element);
        });
      }

      if (settingName == 'options') {
        options = newValue;
      }
      //render the chart
      console.log("ReRender", data)

      try {
        console.log("Resized ChartWrapper! ", $(chartWrapper).width(), $(chartWrapper).height())
        chart.configure({
          width: $(chartWrapper).width(),
          height: currentSettings.chartHeight
        });
        chart.update();
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
    "type_name": "rickshawPlotWidget",
    "display_name": "Rickshaw Graphics Plot",
    "fill_size": true,
    "external_scripts": [
    "https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.3/d3.min.js",
    "//cdnjs.cloudflare.com/ajax/libs/rickshaw/1.5.1/rickshaw.min.js",
     ],
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
  newInstanceCallback(new rickshawPlotWidget(settings));
}
});

}());
