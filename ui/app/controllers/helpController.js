App.HelpRoute = Ember.Route.extend({
	ignore_me_model: function(params) {
		var topic = params.topic
		if ( !topic ) {
			topic = 'index'
		}
		var self = this
		console.log ( "fetching in Route topic ", "help/" + topic + ".md")
		var url = "help/" + topic + ".md"
		return $.ajax ({
			url: url,
			success: function ( data, textStatus, xhr ) {
				console.log ( "Got data back", data)
				// self.set ( 'model', data)
			},
			error: function ( data, textStatus, xhr ) {
				$.pnotify({
					title: "Error fetching help",
					text: "Failed to get help: " + textStatus,
					type: 'error'
				})
			}
		})
	}
})

App.HelpController = Ember.Controller.extend({
	actions: {
		render: function (topic) {
			var self = this
			console.log ( "rendering topic ", "help/" + topic + ".md")
			var url = "help/" + topic + ".md"
			$.ajax ({
				url: url,
				success: function ( data, textStatus, xhr ) {
					self.set ( 'topic', data)
				},
				error: function ( data, textStatus, xhr ) {
					$.pnotify({
						title: "Error fetching help",
						text: "Failed to get help: " + textStatus,
						type: 'error'
					})
				}
			})
		}
	},
	init: function() {
		this._super()
		this.send ( 'render', 'index')
	},
	topic: "",
})
