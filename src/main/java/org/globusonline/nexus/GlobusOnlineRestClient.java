package org.globusonline.nexus;
/*
Copyright 2012 Johns Hopkins University Institute for Computational Medicine
Based upon the GlobusOnline Nexus Client written in Python by Mattias Lidman  
available at https://github.com/globusonline/python-nexus-client

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
/**
* @author Chris Jurado
* 
*/
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URLEncoder;

import java.util.*;

import org.apache.log4j.Logger;
import org.json.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GlobusOnlineRestClient {
	
	private String GO_HOST, oauthSecret, community;
	private JSONObject currentUser;
	Cookie[] sessionCookies;
	
	static org.apache.log4j.Logger logger = Logger.getLogger(GlobusOnlineRestClient.class);
	
	public GlobusOnlineRestClient(){
		testInit();
		init("", "", "");
	}
	
	private void testInit(){
		Properties props = new Properties();
		
	     try {
	            String fileName = "/resources/nexus.config";            
	            InputStream stream = GlobusOnlineRestClient.class.getResourceAsStream(fileName);

	            props.load(stream);

	            GO_HOST = (props.getProperty("globus.url", "missing"));
	            community = (props.getProperty("globus.default.community", "missing"));
	            
	    		if(GO_HOST.equals("missing")){
	    			logger.error("Host URL Configuration missing.");
	    			System.out.println("Missing config item");
	    			return;
	    		}
	            
	        } catch (FileNotFoundException e) {
	        	logger.error("authenticator.config not found.");
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	}


    public void init(String username, String password, String oauthSecret){
//      Initial login supported either using username+password or
//      username+oauth_secret. The client also supports unauthenticated calls.
    	
        if(!GO_HOST.startsWith("http")){
//		Default to https
        	GO_HOST = "https://" + GO_HOST;
        }
        this.oauthSecret = oauthSecret;
        this.sessionCookies = null;
        this.currentUser = null;
        if(!username.isEmpty()){
            if(!oauthSecret.isEmpty()){
                usernameOauthSecretLogin(username, oauthSecret);
            }
            else {
            	usernamePasswordLogin(username, password);
            }
        }
    }
    
	// Create a trust manager that does not validate certificate chains
	static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
    	public X509Certificate[] getAcceptedIssuers() { return null;}
    	public void checkClientTrusted(X509Certificate[] certs, String authType) { return; }
    	public void checkServerTrusted(X509Certificate[] certs, String authType) { return; }
	}};
    
//    # GROUP OPERATIONS
	
	public JSONObject getGroupList(String depth){
		
		return getGroupList(null, depth);
	}
	
    public JSONObject getGroupList(UUID rootId, String depth){	
    	
    	String url = "";
    	
    	if(depth.equals("")){
    		depth = "1";
    	}  	
   	
    	url = "/groups/list?depth=" + depth;
    	
    	if(rootId != null){
    		url = url + "&root=" + rootId; 
    	}

        return issueRestRequest(url);
    }
    
    //gid is actually a UUID, but since only JSON objects are involved, it is treated
    //as a String.
    public JSONObject getGroupSummary(UUID gid){
        String url = "/groups/" + gid;
        return issueRestRequest(url);
    }
    
    public JSONObject getGroupMembers(UUID gid){
        String url = "/groups/" + gid + "/members";
        return issueRestRequest(url);
    }
    
    public JSONObject getGroupMember(UUID gid, String username){
        String url = "/groups/" + gid + "/members/" + username;
        return issueRestRequest(url);
    }
    
    public JSONObject getGroupPolicies(UUID gid){
    	String url = "/groups/" + gid + "/policies";
        return issueRestRequest(url);
    }
    
    public JSONObject getGroupEmailTemplates(UUID gid){
//        # Returned document does not include the message of each template. 
//        # Use get_group_email_template for that.
    	String url = "/groups/" + gid + "/email_templates";
    	return issueRestRequest(url);
    }
    
    public JSONObject getGroupEmailTemplate(UUID gid, UUID templateId){
//        # Get a single email template, including message. The template_id can
//        # be gotten by using get_group_email_templates.
    	String url = "/groups/" + gid + "/email_templates/" + templateId;
    	return issueRestRequest(url);
    }
    
    public JSONObject getRenderedGroupEmailTemplate(UUID gid, UUID templateId){
    	String url = "/groups/" + gid + "/email_templates/" + templateId;
    	JSONObject params = new JSONObject();
        
        try {
			params.put("mode", "view");
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}

        return issueRestRequest(url, "", "", "", params, false);
    }
    
    public JSONObject postGroup(String name, String description, UUID parent){
    	return postGroup(name, description, parent, true);
    }
    
    public JSONObject postGroup(String name, String description, UUID parentId, boolean isActive){
//        # Create a new group.
        if(description.isEmpty()){
            description = "A group called \"" + name + "\"";
        }

        JSONObject params = new JSONObject();
        try {
			params.put("name", name);
			params.put("description", description);
			params.put("is_active", isActive);

			if(parentId != null){
				params.put("parent", parentId);
			}
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
        
        String url = "/groups/";
        
        return issueRestRequest(url, "POST", "", "", params, false);
    }
    
    public JSONObject putGroupSummary(String gid, String name, String description, String isActive){
//        # Edit group. Group name, description, and whether the group is active or not
//        # are the only things that can be set using this method.
    	
        String url = "/groups/" + gid;
        JSONObject params = new JSONObject();
        
        try{
        
        	if(!name.isEmpty()){
        		params.put("name", name);
        	}
        	if(!description.isEmpty()){
        		params.put("description", description);
        	}
        	if(isActive.equals("True")){
        		params.put("is_active", isActive);
        	}
        
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
        
        return issueRestRequest(url, "PUT", "", "", params, false);
    }
    
    public JSONObject putGroupPolicies(UUID gid, JSONObject policies){
//        # PUT policies in dict policies. Utility function build_policy_dictionary()
//        # may be used to simplify building the document.
        String url = "/groups/" + gid + "/policies";
        
        return issueRestRequest(url, "PUT", "", "", policies, false);
    }
    
    public JSONObject setSinglePolicy(UUID gid, JSONObject policy, String newPolicyOption){
//      # Wrapper function for easily setting a single policy. For a given policy,
//      # all policy options specified in new_policy_options are set to true,
//      # all others to false. new_policy_options may be a string for single-value
  	
  	JSONArray newPolicyOptionsArray = new JSONArray();
  	try {
		newPolicyOptionsArray.put(0, newPolicyOption);
	} catch (JSONException e) {
		logger.error("JSON Exception.");
		e.printStackTrace();
	}

    return setSinglePolicy(gid, policy, newPolicyOptionsArray);
  }
    
    public JSONObject setSinglePolicy(UUID gid, JSONObject policy, JSONArray newPolicyOptions){
//        # Wrapper function for easily setting a single policy. For a given policy,
//        # all policy options specified in new_policy_options are set to true,
//        # all others to false. new_policy_options may be a string for single-value
//        # policies and must be a list for multi-value policies.

        JSONObject policies = getGroupPolicies(gid);
        JSONArray existingPolicyOptions;
		try {
			existingPolicyOptions = policies.getJSONArray("policy");


			for(int i = 0; i < existingPolicyOptions.length(); i++){
				for(int j = 0; j < newPolicyOptions.length(); j++){
					if(existingPolicyOptions.getJSONObject(i).get("value").equals(newPolicyOptions.getJSONObject(j).get("value"))){
						existingPolicyOptions.getJSONObject(i).put("value", "True");
        		}
        		else{
        			existingPolicyOptions.getJSONObject(i).put("value", "False");
        		}	
        	}
        }
        
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}

        return putGroupPolicies(gid, policies);
    }
        		
	public JSONObject postGroupEmailTemplates(String gid, JSONObject params){
//  	# Create one or more new email templates.
		String url = "/groups/" + gid + "/email_templates";
		return issueRestRequest(url, "POST", "", "", params, false); 
	}
	
	public JSONObject putGroupEmailTemplate(String gid, String templateId, JSONObject params){
//	        # Update an email template.
		String url = "/groups/" + gid + "/email_templates" + templateId;
		return issueRestRequest(url, "PUT", "", "", params, false);
	}
	
//    # GROUP MEMBERSHIP OPERATIONS
	
	public JSONObject postMembership(String gid, String username, String email){

		JSONArray usernames = new JSONArray();
		JSONArray emails = new JSONArray();
		
		usernames.put(username);
		emails.put(email);
		
		return postMembership(gid, usernames, emails);
	}


	public JSONObject postMembership(String gid, JSONArray usernames, JSONArray emails){
//        # POSTing a membership corresponds to inviting a user identified by a 
//        # username or an email address to a group, or requesting to join a group
//        # (if the actor is among the listed usernames). 
		
		String url = "/groups/" + gid + "/members";
		
		JSONObject params = new JSONObject();
		try {
			params.put("users", usernames);
			params.put("emails", emails);
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
		

        return issueRestRequest(url, "POST", "", "", params, false);
	}
	
	public JSONObject putGroupMembership(String gid, String username, String email, String role, 
			String status, String statusReason, String lastChanged, String userDetails){
//	        # PUT is used for accepting invitations and making other changes to a membership.
//	        # The document is validated against the following schema:
//	        # https://raw.github.com/globusonline/goschemas/integration/member.json
//	        # membership_id == invite_id for purposes of accepting an invitation.

		String url = "/groups/" + gid + "/members" + username;
	    return putGroupMembership(url, username, email, role, status, statusReason, userDetails);
	}
	
	public JSONObject putGroupMembershipById(String inviteId, String username, String email, String role, String status,     
            String statusReason, String lastChanged, String userDetails){
//        # put_group_membership_by_id() is used for tying an email invite to a GO user, 
//        # use put_group_membership() otherwise.

        String url = "/memberships/" + inviteId;
        return putGroupMembership(url, username, email, role, status, statusReason, userDetails);
	}
	
	public JSONObject putGroupMembershipRole(UUID gid, String username, String newRole){
		
			JSONObject member = getGroupMember(gid, username);
			try {
				member.put("role", newRole);


	        return putGroupMembership(
	            gid.toString(),
	            username,
	            (String)member.get("email"),
	            (String)member.get("role"),
	            (String)member.get("status"),
	            (String)member.get("statusReason"),
	            null,
	            null);
	        
			} catch (JSONException e) {
				logger.error("JSON Exception.");
				e.printStackTrace();
				return null;
			}
			
	}
	
	public JSONObject claimInvitation(String inviteId){
		
//        # claim_invitation ties an email invite to a GO user, and must be done
//        # before the invite can be accepted.
		
		String url = "/memberships/" + inviteId;
		JSONObject params = new JSONObject();
    	Date date = new Date();
    	Timestamp time = new Timestamp(date.getTime());
		
		try{
			
			JSONObject user = currentUser;
			JSONObject membership = issueRestRequest(url);
			membership.put("username", (String)user.get("username"));
			membership.put("email", (String)user.get("email"));
			params.put("username", (String)user.get("username"));
			params.put("status", (String)membership.get("status"));
			params.put("status_reason", (String)membership.get("status_reason"));
			params.put("role", (String)membership.get("role"));
			params.put("email", (String)membership.get("email"));
			params.put("last_changed", time);
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
			return null;
		}

        return issueRestRequest(url, "PUT", "", "", params, false);
	}
	
	public JSONObject acceptInvitation(UUID gid, String username, String statusReason){
        return putMembershipStatusWrapper(
            gid,
            username,
            "pending",
            "invited",
            "Only invited users can accept an invitation.",
            "");
	}

	public JSONObject rejectInvitation(UUID gid, String username, String statusReason){
        return putMembershipStatusWrapper(
            gid,
            username,
            "rejected",
            "invited",
            "Only an invited user can reject an invitation.",
            "");
	}
	
	public JSONObject rejectPending(UUID gid, String username, String statusReason){
	        return putMembershipStatusWrapper(
	            gid,
	            username,
	            "rejected",
	            "pending",
	            "Only possible to reject membership for pending users.",
	            "");
	}
	
	public JSONObject approveJoin(UUID gid, String username, String statusReason){
		return putMembershipStatusWrapper(
            gid,
            username,
            "active",
            "pending",
            "Only invited users can accept an invitation.",
            "");
	}
	
	public JSONObject suspendGroupMember(UUID gid, String username, String newStatusReason){
		return putMembershipStatusWrapper(
            gid,
            username,
            "suspended",
            "active",
            "Only active members can be suspended.",
            newStatusReason);
	}
	
	public JSONObject unsuspendGroupMember(UUID gid, String username, String newStatusReason){
		return putMembershipStatusWrapper(
            gid,
            username,
            "active",
            "suspended",
            "Only suspended members can be unsuspended.",
            newStatusReason);
	}
	
//    # USER OPERATIONS
	
	public JSONObject getUser(String username){
		
		return getUser(username, null, null, false);
		
	}
	
	public JSONObject getUser(String username, JSONArray fields, JSONArray customFields, boolean useSessionCookies){
//        # If no fields are explicitly set the following will be returned by Graph:
//        # ['fullname', 'email', 'username', 'email_validated', 'system_admin', 'opt_in']
//        # No custom fields are returned by default.
        String url = "";
        boolean includeParams = false;
        JSONObject queryParams = new JSONObject();
        
        try{
        
        	if(fields != null){
        		queryParams.put("fields", queryParams);
        		includeParams = true;
        	}
        	if(customFields != null){
        		queryParams.put("custom_fields", customFields);
        		includeParams = true;
        	}
        
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
			return null;
		}
        
        url = "/users/" + username;
        		
        if(includeParams){
        	url = url + '?' + urlEncode(queryParams);
        }
        
        return issueRestRequest(url, "", "", "", null, useSessionCookies);
	}
        		
	public JSONObject getUserSecret(String username, boolean useSessionCookies){
//    	# Gets the secret used for OAuth authentication.
		JSONArray fieldsArray = new JSONArray();
		
        return getUser(username, fieldsArray, null, useSessionCookies);
	}
	
	public JSONObject postUser(String username, String fullname, String email, String password, JSONObject kwargs){
//	        # Create a new user.
		String acceptTerms = "True";
		String optIn = "True";
		JSONObject params = new JSONObject();
		
		try{
	        
			if(kwargs.has("accept_terms")){
				acceptTerms = (String) kwargs.get("accept_terms");
			}
			if(kwargs.has("opt_in")){
				optIn = (String) kwargs.get("opt_in");
			}		

			params.put("username", username);
			params.put("fullname", fullname);
			params.put("email", email);
			params.put("password", password);
			params.put("accept_terms", acceptTerms);
			params.put("optIn", optIn);
			
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
			return null;
		}

        return issueRestRequest("/users", "POST", "", "", params, false);
	}
	
	public JSONObject putUser(String username, JSONObject kwargs){
//	        # Edit existing user.
		
		try {
			kwargs.put("username", username);
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
        String path = "/users/" + username;
        
        JSONObject content = issueRestRequest(path, "PUT", "", "", kwargs, false);
        return issueRestRequest(path, "PUT", "", "", kwargs, false);
	}
	
	public JSONObject putUserCustomFields(String username, JSONObject kwargs){
        JSONObject content = getUser(username);
  
        try {
			content.put("custom_fields", kwargs);
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
        content.remove("username");
        return putUser(username, content);
	}	
	
	public JSONObject getUserPolicies(String username){
        String url = "/users/" + username + "/policies";
        return issueRestRequest(url);
	}
	
	public JSONObject putUserPolicies(String username, JSONObject policies){
	        String url = "/users/" + username + "/policies";
	        return issueRestRequest(url, "PUT", "", "", policies, false);
	}
	
    public JSONObject putUserMembershipVisibility(String username, String newVisibility){
        JSONObject policies = getUserPolicies(username);
        JSONObject visibilityPolicy;
		try {
			visibilityPolicy = (JSONObject) policies.getJSONObject("user_membership_visibility").get("value");
        
			Iterator<?> keys = visibilityPolicy.keys();
        
			while(keys.hasNext()){
				((JSONObject)visibilityPolicy.get((String)keys.next())).put("value", newVisibility);
			}
			policies.getJSONObject("user_membership_visibility").put("value", visibilityPolicy);
        
			} catch (JSONException e) {
				logger.error("JSON Exception.");
				e.printStackTrace();
			}
 
        	return putUserPolicies(username, policies);  		
    }
    
    public JSONObject simpleCreateUser(String username, String acceptTerms, String optIn){
//        # Wrapper function that only needs a username to create a user. If you
//        # want full control, use post_user instead.

    	String fullname, email, password;
    	
    	if(acceptTerms.equals("")){
    		acceptTerms = "True";
    	}
    	
    	if(optIn.equals("")){
    		optIn = "True";
    	}   	

        fullname = username + " " + (username + "son");
        email = username + "@" + username + "son.com";
        password = "test";
        
        JSONObject kwargs = new JSONObject();
        try {
			kwargs.put("accept_terms", acceptTerms);
			kwargs.put("optIn", optIn);
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
                
        return postUser(username, fullname, email, password, kwargs);
    }
    
    public JSONObject deleteUser(String username){
        String path = "/users/" + username; 
        return issueRestRequest(path, "DELETE", "", "", null, false);
    }   

    public JSONObject usernamePasswordLogin(String username, String password){
//        # After successful username/password authentication the user's OAuth secret
//        # is retrieved and used in all subsequent calls until the user is logged out.
//        # If no username is provided, authentication will be attempted using the default
//        # password used by the simple_create_user() method.
        
    	String path = "/authenticate";
    	JSONObject params = new JSONObject();
    	JSONObject content;
    	
        if(password.isEmpty() || password == null){
        	logger.error("Password missing.");
        	return null;
        }  
        
        try{
        	params.put("username", username);
        	params.put("password", password);
        	content = issueRestRequest(path, "POST", "", "", params, true);
        	
        } catch (JSONException e){
        	logger.error("JSON Exception.");
        	e.printStackTrace();
        	return null;
        }

//        # Also get user secret so that subsequent calls can be made using OAuth:
        
//        JSONObject secretContent = getUserSecret(username, true);
//
//        try {
//			oauthSecret = secretContent.getString("secret");
//		} catch (JSONException e) {
//			logger.error("JSON Exception.");
//			e.printStackTrace();
//		}
//        
//        currentUser = getUser(username);
//        sessionCookies = null;
        
        return content;
    }
    
    public JSONObject usernameOauthSecretLogin(String username, String oauthSecret){
//        # login_username_oauth_secret() tries to retrieve username's user object
//        # using the provided oauth_secret. If succesful, the username and 
//        # oauth_secret will be used for all subsequent calls until user is logged
//        # out. The result of the get_user() call is returned.
    	
    	JSONObject content = getUser(username);
    	
        String oldOauthSecret = oauthSecret;
        JSONObject oldCurrentUser = currentUser;
        this.oauthSecret = oauthSecret;
        currentUser = content;

        return content;
    }
    
    public JSONObject logout(){
    	
        JSONObject content = issueRestRequest("/logout");
        currentUser = null;
        sessionCookies = null;
        oauthSecret = null;
        return content;
    }
    
    public JSONObject postEmailValidation(String validationCode){
        String url = "/validation";
        JSONObject params = new JSONObject();
        try {
			params.put("validation_code", validationCode);
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
        
        return issueRestRequest(url, "POST", "", "", params, false);
    }
    
//    # UTILITY FUNCTIONS

    public JSONObject buildPolicyDictionary(JSONObject kwargs){
//        # Each kwargs must be a dictionary named after a policy, containing policy 
//        # options and values. For example:
//        #    approval = { 'admin': True, 'auto_if_admin': False, 'auto': False, }
//        # go_rest_client_tests.py contains an example setting all policies available 
//        # as of this writing.
    	
        JSONObject policies = new JSONObject();
        
        Iterator<?> keys = kwargs.keys();
        
        while(keys.hasNext()){     
            
			try {
	        	String policy = (String) keys.next();
	            JSONObject policyOptions = new JSONObject();
	            JSONObject policyOptionsSource = new JSONObject();
				policyOptionsSource = kwargs.getJSONObject(policy);
				policyOptionsSource = kwargs.getJSONObject(policy);
  
				Iterator<?> subKeys = policyOptionsSource.keys();
				while(subKeys.hasNext()){
            	
					String optionKey = (String) subKeys.next();
					JSONObject newOption = new JSONObject();          	

					newOption.put("value", policyOptionsSource.get(optionKey));
					policyOptions.put(optionKey, newOption);
				}
        
				JSONObject jsonPolicy = new JSONObject();
				jsonPolicy.put("value", policyOptions);
				policies.put(policy, jsonPolicy);  
				
			} catch (JSONException e) {
				logger.error("JSON Exception.");
				e.printStackTrace();
			}
              
        }
  
        return policies;
    }
 
    
    private JSONObject issueRestRequest(String path){
 	
    	JSONObject params = null;   	
    	return issueRestRequest(path, "", "", "", params, false);
    }
       
    private JSONObject issueRestRequest(String path, String httpMethod, String contentType, String accept, 
    		JSONObject params, boolean useSessionCookies){
    	
    	JSONObject json = null;
    	
    	if(httpMethod.isEmpty()){
    		httpMethod = "GET";
    	}
    	if(contentType.isEmpty()){
    		contentType = "application/json";
    	}
    	if(accept.isEmpty()){
    		accept = "application/json";
    	}

    	try{
    		SSLContext sc = SSLContext.getInstance("SSL");
    		sc.init(null, trustAllCerts, new SecureRandom());
    		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
    		URL url = new URL(GO_HOST + path); 
    		
    		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(); 
    		connection.setDoOutput(true); 
    		connection.setInstanceFollowRedirects(false); 
    		connection.setRequestMethod(httpMethod); 
    		connection.setRequestProperty("Content-Type", contentType); 
    		connection.setRequestProperty("Accept", accept); 
    		connection.setRequestProperty("X-Go-Community-Context", community);
    		
    		System.out.println("ConnectionURL: " + connection.getURL());		       		

        	String body = "";
        
        	if(params != null){
        		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());      
        	
        		if(contentType.equals("application/x-www-form-urlencoded")){
//        			body = urllib.urlencode(params)
        		}
        		else {
        			body = params.toString();
        		}
        		out.write(body);
        		System.out.println("Body:" + body);
        		out.close();  
        	}
        	
    		if(connection.getResponseCode() == 203){
    			logger.error("Access is denied.  Invalid credentials.");
    			throw new Exception();	
    		}
        	if(connection.getResponseCode() == 204){
        		logger.error("Authentciation URL invalid.");
        		throw new Exception();	
        	}
        	if(connection.getResponseCode() == 500){
        		logger.error("Internal Server Error.");
//        		throw new Exception();
        	}
        	if(connection.getResponseCode() != 200){
        		System.out.println("Response code is: " + connection.getResponseCode());
//        		throw new Exception();	
        	}else{

        		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));		
        		String decodedString = in.readLine();
        
        		json = new JSONObject(decodedString);
        	}
        
        	if(useSessionCookies){
        		if(sessionCookies != null){
        			sessionCookies = (Cookie[]) json.get("Cookie");
        		}
        	}
        	else if(currentUser != null && oauthSecret != null){
            JSONObject authHeaders = getAuthHeaders(httpMethod, url.toString());
////        # Merge dicts. In case of a conflict items in headers take precedence.
////                  headers = dict(auth_headers.items() + headers.items())
        	}
        
        		body = null;
        
//        if((String)json.getString("set-cookie") != null){
//        	this.session_cookies = json.getString("set-cookie");
//        }
//        if(!((String)json.getString("content-type")).equals("")){
////        	return response, json.loads(content)
//        }
        } catch (Exception e){
    		e.printStackTrace();
    	}

        return json;
    }
    
    private JSONObject getAuthHeaders(String method, String url){
    	
    	JSONObject oauthParams = new JSONObject();
    	JSONObject authHeaders = new JSONObject();
    	Date date = new Date();
    	Timestamp time = new Timestamp(date.getTime());
    	
    	try {
			oauthParams.put("oauth_version", "1.0");
	    	oauthParams.put("oauth_nonce", generateNonce());
	    	oauthParams.put("oauth_timestamp", Integer.valueOf(time.toString()));
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}

//        OAuthRequest oauthRequest = new OAuthRequest(method, url, oauthParams);
//        JSONObject consumer = Consumer(currentUser, oauthSecret);
//        oauthRequest.sign_request(SignatureMethod_HMAC_SHA1(), consumer, null);
//        auth_headers = oauthRequest.to_header();
//        auth_headers = auth_headers['Authorization'].encode('utf-8');
        
        return authHeaders;
    }
    
    private JSONObject putGroupMembership(String url, String username, String email, String role,
    		String status, String statusReason, String userDetails){
    	
    	JSONObject params = new JSONObject();
    	try {
			params.put("username", username);
			params.put("status", status);
			params.put("status_reason", status);
			params.put("role", role);
			params.put("email", email);

//        # last_changed needs to be set or validation will fail, but the value 
//        # will get overwritten by Graph anyway.
			params.put("last_changed", "2007-03-01T13:00:00");

			if(userDetails != null){
				params.put("user", userDetails);
			}
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
        return issueRestRequest(url, "PUT", "", "", params, false);
    }
    
    public JSONObject putMembershipStatusWrapper(UUID gid, String username, String newStatus, String expectedCurrent, 
            String transitionErrorMessage, String newStatusReason){
    	
        JSONObject member = getGroupMember(gid, username);
        String email = "";
        String role = "";
        String status = "";
        String statusReason = "";
        
        try{
        if(!member.getString("status").equals(expectedCurrent)){
//            raise StateTransitionError(member['status'], new_status,
//                transition_error_message)
        }
        member.put("status", newStatus);
        member.put("statusReason", newStatusReason);
        email = member.getString("email");
        role = member.getString("role");
        status = member.getString("status");
        statusReason = member.getString("status_reason");
        
		} catch (JSONException e) {
			logger.error("JSON Exception.");
			e.printStackTrace();
		}
		
        return putGroupMembership(
            gid.toString(),
            username,
            email,
            role,
            status,
            statusReason,
            "",
            "");    
    }

    private String urlEncode(JSONObject parameters){
    	
    	Iterator<?> keys = parameters.keys();
    	String queryString = "";
    	
    	while(keys.hasNext()){
    		String key = (String)keys.next();
    		try {
    			String value = parameters.getString(key);
				queryString = queryString + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8") + "&";
			} catch (UnsupportedEncodingException e) {
				logger.error("Unsupported Encoding Exception.");
				e.printStackTrace();
			} catch (JSONException e) {
				logger.error("JSON Exception.");
				e.printStackTrace();
			}
    	}    	
    	
    	queryString = queryString.substring(0, queryString.length() - 2);
    	
    	return queryString;
    }
    
    private long generateNonce(){
    	SecureRandom sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
	    	byte[] bytes = new byte[1024/8];
	        sr.nextBytes(bytes);
	        int seedByteCount = 10;
	        byte[] seed = sr.generateSeed(seedByteCount);
	        sr = SecureRandom.getInstance("SHA1PRNG");
	        sr.setSeed(seed);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return sr.nextLong();
    }
}
