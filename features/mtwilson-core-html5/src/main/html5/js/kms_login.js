function UserProfile(data) {
    this.username = ko.observable(data.username);
    this.authorizationToken = ko.observable(data.authorizationToken);
    this.authenticated = ko.observable(false);
    this.error = ko.observable(data.error);
}

function LoginRequest(data) {
    this.username = ko.observable(data.username);
    this.password = ko.observable(data.password);
    this.remember_me = ko.observable(data.remember_me);
    this.error = ko.observable(data.error);
}

function LoginViewModel() {
    var self = this;
    self.loginRequest = new LoginRequest({});
    self.userProfile = new UserProfile({});
    self.options = {
        "postLoginActivatePage": "dashboard.html",  // dashboard should be the first view we show after login; dashboard can then load the "run once" or "are there notifications" code.
        "postLogoutRedirect": "index.html" //  where to send user after logout is done
    };

    // operations
    self.login = function(loginFormElement) {
        console.log("Endpoint: %s", endpoint);
        //            console.log("Search keys 1: %O", ko.toJSON(searchCriteriaItem)); //   results in error: InvalidStateError: Failed to read the 'selectionDirection' property from 'HTMLInputElement': The input element's type ('hidden') does not support selection
        console.log("Login request to %s: %O", endpoint+"/login", ko.toJSON(self.loginRequest)); // attempting to serialize loginRequestItem produces errors, probably because it represents the entire form
        
        // this can be moved into a separate feature so that if an application wants to allow http login, it can simply omit this feature
        // Prevent insecure non-TLS login requests 
        var uri = new URI();
        console.log("Login request is via %s,", uri.protocol());
        if( uri.protocol() !== "https" ) {
            var httpsUri = new URI();
            httpsUri.protocol("https");
            self.loginRequest.error("<a href='"+httpsUri.toString()+"' style='color: red;'>Login via secure site</a>");
            return false;
        }
        //
//                    console.log("Login request form: %O", loginFormElement); // we could use jquery validation with the form element
        $.ajax({
            type: "POST",
            url: endpoint + "/login",
//                        accept: "application/json",
            contentType: "application/json",
            headers: {'Accept': 'application/json'},
            data: ko.toJSON(self.loginRequest), //$("#loginForm").serialize(), 
            success: function(data, status, xhr) {
                console.log("Login results: %O", data);
                document.getElementById("loginButton").disabled = true;
                /*
                 * Example:
                 * {"authorization_token":"G4zpaAK426bZNqMTGGGbWVMiYJnd04Iy5DK75J1iVb4="}
                 */
                
                var authorizationToken = data.authorization_token;
                
                self.userProfile.username(self.loginRequest.username);
                self.userProfile.authorizationToken(authorizationToken);
                self.userProfile.authenticated(true);
                
                // send the authorization token automatically with every ajax request
                $(document).ajaxSend(function(event,jqxhr,settings){
                    console.log("ajaxSend: url: %s", settings.url);
                    // check if url starts with /v1  (for example /v1/users)
                    //if( settings.url.lastIndexOf('/v1',0) === 0 ) {
                        console.log("ajaxSend: accept header: %O", settings.accept);
                        console.log("ajaxSend: headers object: %O", settings.headers);
                        console.log("ajaxSend: AJAX request to /v1, setting authorization token: "+authorizationToken);
                        jqxhr.setRequestHeader("Authorization", "Token "+authorizationToken);
                    //}
                });
                $(document).ajaxError(function(jqxhr, status, errorMessage){
                    console.log("Triggered ajaxError handler.");
                    console.log("Resource request failed with status: %O", status); // "error"
                    console.log("Resource request failed failed with message: %O", errorMessage); 
                    console.log("Resource request failed: %O", jqxhr);
                    //document.location.href = self.options.postLogoutRedirect;
                    if( status.status === 401 ) {
                        console.log("Request unauthorized; logging out");
                        self.logout();
                    }
                    else {
                        console.log("Server response: %s", status.statusText);
                    }
                });
                
                
                // load the navbar and the dashboard, and activate the post-login primary view
                
//                var nextView = self.options.postLoginActivatePage; // "dashboard.html";
                
                $(document).trigger({
                    type: "mtwilson-core-html5:login:success",
                    message: {"username": self.userProfile.username() },
                    time: new Date()
                });
                        
            },
            error: function(xhr, status, errorMessage) {
                document.getElementById("loginButton").disabled = false; 
                console.log("Login failed with status: %s", status); // "error"
                console.log("Login failed with message: %s", errorMessage); // "Unauthorized" which is same as xhr.statusText (the http status message)
                console.log("Login failed: %O", xhr);
                // xhr.responseText has the error html page from the server response
                // xhr.status has the http status code like 401
                if (xhr.status == 401) {
                    self.loginRequest.error("Unauthorized");
//                                self.loginRequest({"error":"Unauthorized","username":"","password":"","remember_me":""}); // instead of passing the error message from the server, we need to internationalize this
                }
            }
        });
        
    self.logout = function(logoutFormElement) {
        self.userProfile.authenticated(false);
        self.userProfile.authorizationToken(null);
        console.log("Logout, authenticated=%O, authorizationToken=%O", self.userProfile.authenticated(), self.userProfile.authorizationToken());
        // instead of trying to clear all data, just reload the page now to the index
        window.location = self.options.postLogoutRedirect; // "index.html";
    };
        
    };

}
