var gulp = require ('gulp');


gulp.task ( 'html', function() {
  gulp.src('app/**')
  .pipe ( gulp.dest( './public/') );
});
