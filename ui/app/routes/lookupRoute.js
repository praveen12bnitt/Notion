
App.LookupRoute = Ember.Route.extend({
	pool: null,
	model: function(params) {
		console.log ( "Lookup Route PoolRoute Model for ", params.poolKey)
		var pool = App.Pool.findById(params.poolKey);
		console.log ( pool )
		this.set("pool", pool)
		return pool
	},
	actions: {}
})

App.LookupView = Ember.View.extend(Ember.TargetActionSupport, {
	layoutName: 'lookup',
    actions: {
    },
    createLookupTable: function() {
        console.log ( "==== Starting up jtable ====")
        var pool = this.controller.get('model')
        var self = this
        // $('#PersonTableContainer').jtable({
        var table = this.$('.lookup-table').jtable({
            title: 'Lookup for ' + pool.get('name'),
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
                    text: 'New PatientName',
                    click: function() {
                        var entry = self.get('table').jtable('addRecord', {
                            record: {
                                Type: "PatientName",
                                Name: "<replace>",
                                Value: "<replace>"
                            },
                            success: function(a) {
                                console.log("New PatientName: ", a, a.Record.LookupKey)
                                var key = a.Record.LookupKey
                                Ember.run.later ( self, function() {
                                    var row = self.get('table').jtable('getRowByKey', key)
                                    console.log ( "found row: ", row)
                                    self.get('table').jtable('showEditForm', row)
                                }, 700)
                            }
                        });
                    }
                },
                {
                    text: 'New AccessionNumber',
                    click: function() {
                        var entry = self.get('table').jtable('addRecord', {
                            record: {
                                Type: "AccessionNumber",
                                Name: "<replace>",
                                Value: "<replace>"
                            },
                            success: function(a) {
                                console.log("New AccessionNumber: ", a, a.Record.LookupKey)
                                var key = a.Record.LookupKey
                                Ember.run.later ( self, function() {
                                    var row = self.get('table').jtable('getRowByKey', key)
                                    console.log ( "found row: ", row)
                                    self.get('table').jtable('showEditForm', row)
                                }, 700)

                            }
                        });
                    }
                },
                {
                    text: 'New PatientID',
                    click: function() {
                        var entry = self.get('table').jtable('addRecord', {
                            record: {
                                Type: "PatientID",
                                Name: "<replace>",
                                Value: "<replace>"
                            },
                            success: function(a) {
                                console.log("New PatientID: ", a, a.Record.LookupKey)
                                var key = a.Record.LookupKey
                                Ember.run.later ( self, function() {
                                    var row = self.get('table').jtable('getRowByKey', key)
                                    console.log ( "found row: ", row)
                                    self.get('table').jtable('showEditForm', row)
                                }, 700)

                            }
                        });
                    }
                }]
            },
            actions: {
                listAction: '/rest/pool/' + pool.get('poolKey') + "/lookup",
                deleteAction: '/rest/pool/' + pool.get('poolKey') + "/lookup/delete",
                createAction: '/rest/pool/' + pool.get('poolKey') + "/lookup/create",
                updateAction: '/rest/pool/' + pool.get('poolKey') + "/lookup/update"
            },
            ajaxSettings: {
                type: 'POST',
                dataType: 'json'
            },
            fields: {
                LookupKey: {
                    key: true,
                    list: false
                },
                Type: {
                    title: 'Lookup Type',
                    width: '20%',
                    edit: false
                },
                Name: {
                    title: 'Key',
                    width: '40%'
                },
                Value: {
                    title: "Value",
                    width: '40%'
                }
            }
        });
        this.set('table', table)
        this.get('table').jtable ('load')
    },
	didInsertElement: function(){
		console.log("didInsertElement", this.$())
		var pool = this.controller.get('model')
        this.createLookupTable()
        var table = this.get('table')

        // File upload
        console.log ( "Creating fileupload")
        $('#fileupload').fileupload({
            drop: function(e,data) {
                alert ( data )
            },
            add: function(e,data) {
                alert ( data )
            }
        })

        // Make our binding
        this.get('controller').addObserver('model', this, function() {
            Ember.run.once(this, function() {
                console.log("Lookup callback", table)
                if ( table ) {
                    try {
                        table.jtable('destroy')
                    } catch ( err ) {
                        console.error ( "caught error", err)
                    }
                }             
               // Create the new one...
                this.createLookupTable()
                this.get('table').jtable ('load')
            })
        })


	}
})

