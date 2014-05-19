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
    refresh = require('gulp-livereload'),
    livereload = require('gulp-livereload'),
    uglify = require('gulp-uglify'),
    lr = require('tiny-lr'),
    streamqueue = require('streamqueue'),
    handlebars = require('gulp-ember-handlebars'),
    styl = require('gulp-styl'),
    stylus = require('gulp-stylus'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    server = lr();

gulp.task("default", ['watch'], function() {
})

gulp.task("build", ['ace', 'assets', 'vendor', 'app', 'style', 'bootstrap'], function() {
})


gulp.task("watch", ['lr-server', 'build'], function() {
  console.log("\nStarting webserver and watching files\n")
  gulp.watch ( ['app/*.js', 'app/partials/**', 'app/*.html'], ['app'])
})

// Handlebars / ember / all the rest
gulp.task('app', function() {

  // Just for backbone
  gulp.src('app/*.js')
  .pipe(gulp.dest('public/js'));

  gulp.src('app/*.html')
  .pipe(gulp.dest('public/'));

  // Copy partials
  gulp.src('app/partials/**')
  .pipe(gulp.dest('public/partials'))
  .pipe(refresh(server))

})

// CSS using Styl
gulp.task('style', function() {
  gulp.src([
    'app/styles/*.css',
    'bower_components/font-awesome/css/font-awesome*.css'
    ])
//  .pipe(styl({compress : true }))
//  .pipe(stylus)
  .pipe(gulp.dest('public/css'))
})


// Vended source
gulp.task('vendor', function() {
  gulp.src([
    'vendor/scripts/moment.js',
    'vendor/scripts/showdown.js',
    'bower_components/jquery/jquery.js',
    'bower_components/angular-ui-ace/ui-ace.js',
    'bower_components/angular-ui-bootstrap-bower/ui-bootstrap-tpls.js',
    'bower_components/vex/js/vex.dialog.js',
    'bower_components/vex/js/vex.js',
    'bower_components/angular-ui-router/release/angular-ui-router.js',
    'bower_components/angular-ui-select/dist/select.js',
    'bower_components/angularAMD/angularAMD.js',
    'bower_components/angularAMD/ngload.js',
    'bower_components/requirejs/require.js',
    'bower_components/backbone/backbone.js',
    'bower_components/angular/angular.js',
    'bower_components/angular-route/angular-route.js',
    'bower_components/underscore/underscore.js',
    'bower_components/handlebars/handlebars.js',
    'vendor/scripts/console-polyfill.js',
    ])
  .pipe(uglify({outSourceMap: true}))
  .pipe(gulp.dest('public/js'))

  gulp.src(['bower_components/dropzone/downloads/dropzone-amd-module.js'])
  .pipe(rename('dropzone.js'))
  .pipe(uglify({outSourceMap: true}))
  .pipe(gulp.dest('public/js'))

})

gulp.task('ace', function() {
  gulp.src('bower_components/ace-builds/src-noconflict/**')
  .pipe(gulp.dest("public/js/ace"))
})

gulp.task('bootstrap', function() {
  gulp.src('bower_components/bootstrap/dist/**')
  .pipe(gulp.dest("public/"))
})

// Assets
gulp.task('assets', function() {
  gulp.src('app/assets/**')
  .pipe(gulp.dest("public/"))

})


gulp.task('lr-server', function() {
  server.listen(35729, function(err) {
    if (err) return console.log(err);
  });
});
