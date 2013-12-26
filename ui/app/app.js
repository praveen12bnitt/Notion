App = Ember.Application.create({
	LOG_TRANSITIONS: true
})


// Routes go here
App.Router.map(function() {
	  // A 'resource' defines a group of routes that work together
    this.resource("pools", function() {	
      this.resource("studies", {path: 'studies/:poolKey'})
      this.resource("lookup", {path: 'lookup/:poolKey'})
      this.resource("pool", { path: 'pool/:poolKey'})
      this.route("new")
    })
    this.route('about');
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
    this.editor.setShowFoldWidgets(false)
    this.editor.setTheme("ace/theme/monokai");
    this.editor.getSession().setMode("ace/mode/javascript");
    if ( this.get('mode')) {
      this.editor.getSession().setMode ( this.get('mode'))
    }
    if ( this.get('theme')) {
      this.editor.getSession().setMode ( this.get('theme'))
    }

    var self = this;
    if (this.preset) {
      this.set('content', this.preset);
      this.preset = null;
    }

    // On longer files, this causes unacceptable bugs
    // this.editor.on('change', function(){
    //     Ember.run.once(self, self.notifyPropertyChange, 'content');
    // });
    if ( !this.get('longText') ) {
      this.editor.on('change', function(){
        Ember.run.debounce(self, self.notifyPropertyChange, 'content', 250 );
      });
    }

    this.editor.commands.addCommand({
      name: 'save',
      bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
      exec: function(editor) {
        console.log("saveScript")
        self.notifyPropertyChange ( 'content')
        self.sendAction ( 'saveScript', self.script )
      }
    })

    // Try
     this.editor.commands.addCommand({
      name: 'exec',
      bindKey: {win: 'Ctrl-Return', mac: 'Command-Return'},
      exec: function(editor) {
        console.log ( "tryScript from component", self.script)
        self.notifyPropertyChange ( 'content')
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
      console.log ( "getting value")
      return this.editor.getSession().getValue();
    } else {
      this.editor.getSession().setValue(val);
      return val;
    }
  }.property()
});


/** Extend the jtable */
(function ($) {

    //extension members
    $.extend(true, $.hik.jtable.prototype, {

        showEditForm: function(val) {
            var $row = null;
            if (typeof(val) == 'number') {
                $row = this.getRowByKey(val);
                if ($row == null)
                    throw "Invalid key.";
            } else
                $row = val;

            if (!$row.hasClass('jtable-data-row'))
                throw "This is not a valid jtable data row";

            this._showEditForm($row);
        }

    });

})(jQuery);


