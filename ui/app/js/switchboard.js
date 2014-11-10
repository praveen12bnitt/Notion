// The purpose of this file is to check if the user is logged in and re-direct appropriately.

$.getJSON('/rest/user/')
  .done(function(data,textStatus,jqXHR){
    console.log ( "got data: ", data);
    // Are we logged in?
    if ( data.isAuthenticated ) {
      alert ( "Redirecting to notion.html");
      window.location = "notion.html";
    } else {
      alert ( "Redirecting to login.html");
      window.location = "login.html";
    }
  })
  .fail (function(jqXHR, textStatus,errorThrown) {
    window.location.href = "error.html";
  });
