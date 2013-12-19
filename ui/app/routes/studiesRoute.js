
App.StudiesRoute = Ember.Route.extend({
	pool: null,
	model: function(params) {
		console.log ( "Studies Route PoolRoute Model for ", params.poolKey)
		var pool = App.Pool.findById(params.poolKey);
		console.log ( pool )
		this.set("pool", pool)
		return pool
	},
	actions: {}
})

App.StudiesView = Ember.View.extend({
	layoutName: 'studies',
	didInsertElement: function(){
		console.log("didInsertElement", this.$())
		var table = this.$().find('.series-table')[0]
		console.log(table)
		var pool = this.controller.get('model')
		/**
		this.$().find('.series-table').dataTable({
			"bProcessing": true,
			"bServerSide": true,
			"sAjaxSource": "/rest/pool/" + pool.get('poolKey') + "/series"
		})
     	*/
     	console.log ( "==== Starting up jtable ====")
		$('#PersonTableContainer').jtable({
            title: 'Studies',
            actions: {
                listAction: '/rest/pool/' + pool.get('poolKey') + "/series"
            },
            ajaxSettings: {
            	type: 'GET',
            	dataType: 'json'
            },
            fields: {
                StudyKey: {
                    key: true,
                    list: false
                },
                PatientID: {
                    title: 'PatientID',
                    width: '10%'
                },
                PatientName: {
                    title: 'Name',
                    width: '10%'
                },
                AccessionNumber: {
                	title: "Accession Number",
                	width: '20%'
                },
                StudyDescription: {
                	title: 'Description',
                	width: '30%'
                }
            }
        });
		$('#PersonTableContainer').jtable('load');


	}
})

