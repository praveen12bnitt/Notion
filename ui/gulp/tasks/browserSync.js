var browserSync = require('browser-sync');
var gulp        = require('gulp');

gulp.task('browserSync', ['build'], function() {
  var config = {
    files: ['public/**', 'public/js/*.js'],
    proxy: "localhost:11118"
  };
  browserSync ( config);
});
