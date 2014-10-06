var browserSync = require('browser-sync');
var gulp        = require('gulp');

gulp.task('browserSync', ['build'], function() {
  browserSync.init(['public/**', 'public/js/*.js'], {
    proxy: "localhost:11118"
  });
});
