var browserSync = require('browser-sync');
var gulp        = require('gulp');

gulp.task('browserSync', ['build'], function() {
  browserSync.init(['build/**'], {
    proxy: "localhost:11118"
  });
});
