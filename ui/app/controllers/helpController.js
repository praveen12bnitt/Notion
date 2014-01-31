//// help
App.HelpRoute = Ember.Route.extend({
	model: function(params) {
		var topic = params.topic

		var self = this
		console.log ( "fetching in Route topic ", "help/" + topic + ".md")
		var url = "help/" + topic + '.md'
		return $.ajax ({
			url: url,
			error: function ( data, textStatus, xhr ) {
				$.pnotify({
					title: "Error fetching help",
					text: "Failed to get help: " + textStatus,
					type: 'error'
				})
			}
		}).pipe( function(data) {
			return {topic: topic, content: data}
		})
	},    /* This is the key to making it all work.  Puts together the URL */
    serialize: function (model) {
        console.log("HelpRoute.serialize", model)
        if ( !model ) {
        	return 'index'
        }
        return model.topic
    },
    setupController: function(controller, model) {
    	console.log ( 'HelpRoute.setupController ', model)
    	controller.set('model', model);
    },
})
