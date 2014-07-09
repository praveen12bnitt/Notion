/*
install:
npm install --save-dev gulp gulp-uglify gulp-concat gulp-notify gulp-cache gulp-livereload tiny-lr gulp-util express gulp-browserify
npm install --save-dev streamqueue
npm install --save-dev gulp-ember-handlebars
npm install --save-dev gulp-styl
npm install --save-dev gulp-uglify gulp-rename
*/



var gulp = require('gulp'),
    concat = require('gulp-concat'),
    notify = require('gulp-notify'),
    cache = require('gulp-cache'),
    livereload = require('gulp-livereload'),
    uglify = require('gulp-uglify'),
    streamqueue = require('streamqueue'),
    styl = require('gulp-styl'),
    stylus = require('gulp-stylus'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    connectlr = require('connect-livereload'),
    express = require('express'),
    exec = require('child_process').exec;



// These are the assets we need to build
assets = {
  app: ['app/**'],
  partials: ['app/partials/**'],
  js: ['app/js/**'],
  docs: ['../Documentation/**/*.rst']
}



//
// refreshBrowser = function() {
//   // Hmmp, some magic here, just hit the lr server with *.html
//   server.changed({ body: { files: ['*.html'] } });
// }

gulp.task("default", ['watch']);
gulp.task("watch", [], function() {
  console.log("\nStarting webserver and watching files\n")
  livereload.listen();
  // gulp.watch ( assets.app, ['app'])
  gulp.watch ( assets.partials, ['partials'])
  gulp.watch ( assets.js, ['js'])

  // .on('change', livereload.changed)
  gulp.watch ( assets.docs, ['docs'])
})

gulp.task("build", ['app'], function() {
})

gulp.task('docs', function() {
  exec('make docs', function (err, stdout, stderr) {
    console.log("bumping the Docs server")
    refreshBrowser();
  });
})

gulp.task('partials', [], function() {
gulp.src(assets.partials)
.pipe(gulp.dest('public/partials'))
.pipe(livereload());
});

gulp.task('js', [], function() {
gulp.src(assets.js)
.pipe(gulp.dest('public/js'))
.pipe(livereload());
});


// All the rest
gulp.task('app', ['vendor'], function() {
  // Just for backbone
gulp.src('app/**')
.pipe(gulp.dest('public/'))
})

// Vended source
gulp.task('vendor', function() {
  gulp.src([
    'bower_components/jquery/dist/jquery.js',
    'bower_components/toastr/toastr.js',
    'bower_components/angular-ui-ace/ui-ace.js',
    'bower_components/angular-ui-bootstrap-bower/ui-bootstrap-tpls.js',
    'bower_components/angular-ui-router/release/angular-ui-router.js',
    'bower_components/angular-ui-select/dist/select.js',
    'bower_components/backbone/backbone.js',
    'bower_components/notifyjs/dist/notify-combined.js',
    'bower_components/angular/angular.js',
    'bower_components/angular-route/angular-route.js',
    'bower_components/underscore/underscore.js',
    'bower_components/angular-bindonce/bindonce.js',
    'bower_components/w11k-dropdownToggle/dist/w11k-dropdownToggle.js',
    'bower_components/w11k-select/dist/w11k-select.js',
    'bower_components/w11k-select/dist/w11k-select.tpl.js'
    ])
  .pipe(uglify())
  .pipe(gulp.dest('public/js'))

  gulp.src('bower_components/ace-builds/src-noconflict/**')
  .pipe(gulp.dest("public/js/ace"))

  gulp.src('bower_components/bootstrap/dist/**')
  .pipe(gulp.dest("public/"))

  gulp.src([
    'bower_components/font-awesome/css/font-awesome*.css',
    'bower_components/w11k-select/dist/w11k-select.css',
    'bower_components/toastr/toastr.css'
    ])
  .pipe(gulp.dest('public/css'));

  gulp.src('bower_components/font-awesome/fonts/**').pipe(gulp.dest('public/fonts'));

})


gulp.task('docs-server', function() {

  // Start an Express server for the docs
  var app = express();
  app.use ( connectlr() );
  app.use(express.static('../Documentation/_build/html'));
  console.log ( "\nStarting documentation server on\n\n\thttp://localhost:8400\n")
  app.listen(8400);

});
