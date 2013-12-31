
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

App.StudiesView = Ember.View.extend(Ember.TargetActionSupport, {
	layoutName: 'studies',
    actions: {
        move: function() {
            // Collect the series and the move destination
            var request = {}
            var selectedRows = this.get('table').jtable('selectedRows');
            var r = new Array()
            selectedRows.each ( function() {
               var row = $(this).data('record');
               r.push(row.StudyKey)
            })
            var input = this.$('.move-destination')[0]
            request.destinationPoolKey = input.options[input.selectedIndex].value
            var destinationName = input.options[input.selectedIndex].text
            request.studyKeys = r

            // Send it along
            var pool = this.controller.get('model')
            $.pnotify ({
                title: "Moving studies",
                text: "Sending " + request.studyKeys.length + " studies to " + destinationName
            })
            $.ajax ({
                contentType : 'application/json',
                type: "PUT",
                url: "/rest/pool/" + pool.get('poolKey') + "/move" ,
                data: JSON.stringify(request),
                success: function ( data ) {
                    $.pnotify({
                        title: 'Moved studies',
                        text: 'Moved ' + request.studyKeys.length + ' studies to ' + destinationName
                    })
                },
                error: function ( data ) {
                    $.pnotify({
                        title: 'Error moving studies',
                        text: 'Error: ' + data.statusText + ' (' + data.status + ')'
                    })
                }
            })
        }
    },
    createStudiesTable: function() {
        console.log ( "==== Starting up jtable ====")
        var pool = this.controller.get('model')

        // $('#PersonTableContainer').jtable({
        var table = this.$('.study-table').jtable({
            title: 'Studies for ' + pool.get('name'),
            multiselect: true,
            selecting: true,
            paging: true,
            pageSize: 50,
            sorting: true,
            toolbar: {
                items: [{
                    text: 'Reload',
                    click: function() {
                        self.table.jtable('reload')
                    }
                },
                {
                    text: 'Move',
                    click: function() {
                        self.triggerAction({action: 'move', target: self})
                        var selectedRows = self.get('table').jtable('selectedRows');
                        // alert ( 'Would be moving ' + selectedRows.length + ' records')
                    }
                }]
            },
            actions: {
                listAction: '/rest/pool/' + pool.get('poolKey') + "/studies",
                deleteAction: '/rest/pool/' + pool.get('poolKey') + "/studies/delete"
            },
            ajaxSettings: {
                type: 'POST',
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
        this.set('table', table)
        this.get('table').jtable ( 'load')
    },
	didInsertElement: function(){
		console.log("didInsertElement", this.$())
		var pool = this.controller.get('model')
        this.createStudiesTable()

        var self = this
        var input = this.$('.move-destination')
        console.log("move destination", input)
        $.getJSON("/rest/pool", function(data) {
            console.log ( "Got back ", data)
            var options = d3.select(input[0]).selectAll("option").data(data.pool)

            options.enter().append ( "option" ).attr ( 'value', function(d) {
                console.log("option: ", d)
                return d.poolKey
            }).text(function(d){
                return d.name
            })
            console.log("Got options: ", options)
            options.exit().remove()
            input.chosen()
        })


                // Make our binding
        this.controller.addObserver('model', this, function() {
            Ember.run.once(this, function() {
                console.log("Studies callback", this)
                var table = this.get('table')
                if ( table ) {
                    table.jtable('destroy')
                }
                // Create the new one...
                this.createStudiesTable()
                this.get('table').jtable ( 'load')
            })
        })


	}
})

