
App.LookupRoute = Ember.Route.extend({
	pool: null,
	model: function(params) {
		console.log ( "Lookup Route PoolRoute Model for ", params.poolKey)
		var pool = App.Pool.findById(params.poolKey);
		console.log ( pool )
		this.set("pool", pool)
		return pool
	},
	actions: {},
})

App.LookupController = Ember.ObjectController.extend({
    actions: {
        loadCSV: function() {
            var self = this
            var csv = self.get('csv')
            console.log("Sending CSV data", csv)
            if ( !csv ) { 
                $.pnotify({
                    title: "No CSV",
                    text: "CSV data is not loaded",
                    type: "error"
                })
                return 
            }
            $.ajax ({
                contentType : 'application/json',
                type: "PUT",
                url: "/rest/pool/" + this.get('poolKey') + "/lookup/csv" ,
                data: JSON.stringify(csv),
                success: function ( data ) {
                    $.pnotify({
                        title: 'Saved CSV',
                        text: data.message,
                        type: 'success',
                    })
                    self.set("csv", null)
                },
                error: function() {
                    $.pnotify({
                        title: 'Error saving CSV',
                        text: 'Server could not parse CSV',
                        type: 'error'
                    })
                }
            })
        }
    },
    hasFileUpload: function() {
        /* Check for the various File API support. */
        if (window.File && window.FileReader && window.FileList && window.Blob) {
            return true;
        } else {
            return false;
        }
    }.property(),
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
        var self = this
		console.log("didInsertElement", this.$())
		var pool = this.controller.get('model')
        this.createLookupTable()
        var table = this.get('table')

        // File upload
        console.log ( "Creating fileupload")
        self.$('.upload').fileupload({
            add: function(e,data) {
                console.log('Add data', data, self.$(".upload").files)
                $.each(data.files, function(idx, file){
                    if ( file.type.indexOf("text") < 0 ) {
                        $.pnotify({
                            title: "Incorrect filetype",
                            text: "File must be text and a CSV",
                            type: "error"
                        })
                        return
                    }
                    var reader = new FileReader();

                    reader.onerror = function () {
                        console.log(reader.error)
                        $.pnotify({
                            title: "CSV Parsing Failed",
                            type: "error",
                            text: "Could not parse the file " + file + " <p>Error was: " + errreader.error
                        })
                        alert ( "Could not parse this CSV file")
                    };
                    reader.onload = function(event)
                    {
                        var text = event.target.result;
                        var results = $.parse(text, {
                            delimiter: ',',
                            header: false,
                            dynamicTyping: true
                        });

                        if ( results.errors.length > 0 ) {
                            $.pnotify({
                                title: "Parsing failed",
                                type: "success",
                                text: "Had " + results.errors.length + " errors (see console for details)"
                            })
                            console.error ( "Failed to parse", results )
                        } else {
                            $.pnotify({
                                title: "Parsing completed",
                                type: "success",
                                text: "Finished parsing file"
                            })
                            var headerIndex = {}
                            for ( var i = 0; i < results.results[0].length; i++) {
                                headerIndex[i] = results.results[0][i]
                            }
                            var csv = {
                                headers: results.results.shift(),
                                rows: results.results,
                                headerIndex: headerIndex,
                                typeColumn: null,
                                keyColumn: null,
                                valueColumn: null,
                            }
                            /* Find the headers */
                            $.each(csv.headers, function(idx, header) {
                                if ( header.toLowerCase().match("type|t|tag")) {
                                    csv.typeColumn = { index: idx, name: header }
                                }
                                if ( header.toLowerCase().match("key|k|name")) {
                                    csv.keyColumn = { index: idx, name: header }
                                }
                                if ( header.toLowerCase().match("value|v")) {
                                    csv.valueColumn = { index: idx, name: header }
                                }
                            })
                            csv.valid = csv.valueColumn && csv.keyColumn && csv.typeColumn
                            console.log ( "Reformated CSV results", csv)
                            self.controller.set("csv", csv)                            
                        }

                    };

                    reader.readAsText(file);
                })
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
	},
})

