/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.kms.login.token;

import com.intel.mtwilson.shiro.EncryptedTokenContent;
import com.intel.dcsg.cpg.authz.token.TokenFactory;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.iso8601.Iso8601Date;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.jaxrs2.mediatype.DataMediaType;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.shiro.authc.password.LoginPasswordId;
import com.intel.mtwilson.shiro.UserId;
import com.intel.mtwilson.shiro.Username;
import com.intel.mtwilson.shiro.UsernameWithPermissions;
import com.intel.mtwilson.shiro.authc.token.MemoryTokenRealm;
import com.intel.mtwilson.shiro.authc.token.MemoryTokenRealm.MemoryTokenDatabase;
import com.intel.mtwilson.shiro.authc.token.TokenCredential;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * The token generated by this class contains the following information: UserId
 * Username LoginPasswordId
 *
 * These correspond to the principals that are set by the JdbcPasswordRealm
 * against which we are authenticating the user.
 *
 * We explicitly serialize these three values and then reconstruct the
 * principals from them as necessary because the authorization token should not
 * allow arbitrary (blind) reconstruction of principals, or else an attacker who
 * compromises the authorization token key would be able to construct any set of
 * principals and pass them to the server in an attack token and elevate
 * privileges. By explicitly storing the LoginPasswordId, UserId, and Username
 * we limit any hacking of the token to the same privileges that would have been
 * available if the attacker had stolen the user's password.
 *
 * @author jbuhacoff
 */
@V2
@Path("/login")
public class PasswordLogin {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PasswordLogin.class);
    
//    private TokenFactory factory;
//    @RequiresGuest  // causes it to fail when someone is already logged in, which is not convenient because then user has to close browser and "forget" credentials if they want to log in again (for example if they reloaded entry point)
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
//    public void submitLoginForm(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @FormParam("username") String username, @FormParam("password") String password) {
    public void submitLoginForm(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @BeanParam PasswordLoginRequest passwordLoginRequest) throws GeneralSecurityException {
//        log.debug("submitLoginForm username {} password {}", username, password);
        log.debug("submitLoginForm beanparam username {} password {}", passwordLoginRequest.getUsername(), passwordLoginRequest.getPassword());
        log.debug("request from {}", request.getRemoteHost());

        PasswordLoginResponse passwordLoginResponse = loginRequest(request, response, passwordLoginRequest);
        log.debug("Successfully processed login request with auth token {}.", passwordLoginResponse.getAuthorizationToken());
    }

//    @RequiresGuest
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    public PasswordLoginResponse loginRequest(@Context final HttpServletRequest request, @Context final HttpServletResponse response, PasswordLoginRequest loginForm) throws GeneralSecurityException {
        log.debug("loginRequest username {} password {}", loginForm.getUsername(), loginForm.getPassword());
        log.debug("request from {}", request.getRemoteHost());

        // load configuration
        Configuration configuration;
        try {
            configuration = ConfigurationFactory.getConfiguration();
        }
        catch(IOException e) {
            log.warn("Cannot load configuration, using defaults", e);
            configuration = new PropertiesConfiguration();
        }
        
        boolean requireTls = Boolean.valueOf(configuration.get(LoginTokenUtils.LOGIN_REQUIRES_TLS, "true"));
        if( requireTls && !request.isSecure()) {
            log.info("Denying non-TLS login request");
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        
        // authenticate the user with JdbcPasswordRealm and PasswordCredentialsMatcher (configured in shiro.ini)
        Subject currentUser = SecurityUtils.getSubject();
//        if( !currentUser.isAuthenticated() ) { // shouldn't need this because we have @RequiresGuest annotation...
        log.debug("authenticating...");
        // for this junit test we're using mtwilson.api.username and mtwilson.api.password properties from  mtwilson.properties on the local system, c:/mtwilson/configuration/mtwilson.properties is default location on windows 
        UsernamePasswordToken loginToken = new UsernamePasswordToken(loginForm.getUsername(), loginForm.getPassword());
//            UsernamePasswordToken token = new UsernamePasswordToken("root", "root"); // guest doesn't need a password
        loginToken.setRememberMe(false); // we could pass in a parameter with the form but we don't need this
        currentUser.login(loginToken); // throws UnknownAccountException , IncorrectCredentialsException , LockedAccountException , other specific exceptions, and AuthenticationException 

        if (!currentUser.isAuthenticated()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        log.info("logged in as {}", currentUser.getPrincipal());
        PrincipalCollection principals = currentUser.getPrincipals();

        Collection<UsernameWithPermissions> usernames = principals.byType(UsernameWithPermissions.class);
        log.debug("Found {} UsernameWithPermissions principals", usernames.size());
//        Collection<UserId> userIds = principals.byType(UserId.class);
//        Collection<LoginPasswordId> loginPasswordIds = principals.byType(LoginPasswordId.class);

        UsernameWithPermissions usernameWithPermissions = LoginTokenUtils.getFirstElementFromCollection(usernames);
//        UserId userId = getFirstElementFromCollection(userIds);
//        LoginPasswordId loginPasswordId = getFirstElementFromCollection(loginPasswordIds);
        if ( usernameWithPermissions == null /* || userId == null || loginPasswordId == null */ ) {
            log.error("One of the required parameters is missing. Login request cannot be processed");
            throw new IllegalStateException();
        }
        
        String tokenValue = RandomUtil.randomBase64String(32); // new random token value
        Date notBefore = new Date(); // token is valid starting right now
        Date notAfter = LoginTokenUtils.getExpirationDate(notBefore, configuration);
        Integer used = 0; // new token
        Integer usedMax = null; // for logins we don't set a usage limit, just an expiration date
        TokenCredential tokenCredential = new TokenCredential(tokenValue, notBefore, notAfter, used, usedMax);
        
        MemoryTokenDatabase database = MemoryTokenRealm.getDatabase();
        database.add(tokenCredential, usernameWithPermissions);

        // include token in response headers
        Iso8601Date authorizationDate = new Iso8601Date(new Date());
        response.addHeader("Authorization-Token", tokenCredential.getValue());
        response.addHeader("Authorization-Date", authorizationDate.toString());

        // and in response body
        PasswordLoginResponse passwordLoginResponse = new PasswordLoginResponse();
        passwordLoginResponse.setAuthorizationToken(tokenCredential.getValue());
        passwordLoginResponse.setAuthorizationDate(authorizationDate);
        passwordLoginResponse.setNotAfter(new Iso8601Date(tokenCredential.getNotAfter()));
        return passwordLoginResponse;
    }
    
}
