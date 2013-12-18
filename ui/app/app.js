App = Ember.Application.create({
	LOG_TRANSITIONS: true
})


// Routes go here
App.Router.map(function() {
	// A 'resource' defines a group of routes that work together
	this.resource("pools", function() {	
		this.resource("pool", { path: ':poolKey'})
		this.route("new")
	})

	this.route('about');
	this.route("bill");
})

// Connecting to Slicer
App.ApplicationAdapter = DS.RESTAdapter.extend({
  namespace: 'rest',
  pathForType: function(type) {
  	console.log("pathForType: "+ type)
  	return type;
  },
  findHasMany: function ( store, record, url) {
  	console.log ( "Calling to get " + record + " from " + url )
  	return this._super ( store, record, url )
  }
});




App.AceEditorComponent = Ember.Component.extend({
  script: null,
  didInsertElement: function(){
    console.log("didInsertElement", this.$())
    console.log(this.$().find('.script-editor')[0])
    this.editor = ace.edit(this.$().find('.script-editor')[0]);
    this.editor.setTheme("ace/theme/monokai");
    this.editor.getSession().setMode("ace/mode/javascript");

    var self = this;
    this.editor.on('change', function(){
      Ember.run.once(self, self.notifyPropertyChange, 'content');
    });
    if (this.preset) {
      this.set('content', this.preset);
      this.preset = null;
    }

    this.editor.commands.addCommand({
      name: 'save',
      bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
      exec: function(editor) {
        console.log("saveScript")
        self.sendAction ( 'saveScript', self.script )
      }
    })

    // Try
     this.editor.commands.addCommand({
      name: 'exec',
      bindKey: {win: 'Ctrl-Return', mac: 'Command-Return'},
      exec: function(editor) {
        console.log ( "tryScript from componont", self.script)
        self.sendAction ( 'tryScript', self.script )
      }
    })

  },
  content: function(key, val){
    if (!this.editor) {
      this.preset = val;
      return val;
    }
    if (arguments.length == 1) {
      return this.editor.getSession().getValue();
    } else {
      this.editor.getSession().setValue(val);
      return val;
    }
  }.property()
});