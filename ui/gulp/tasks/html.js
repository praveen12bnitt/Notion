var gulp = require ('gulp');


gulp.task ( 'html', function() {
  gulp.src('app/*.html')
  .pipe ( gulp.dest( './public/') );
});
