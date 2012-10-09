/*
Copyright 2012 Johns Hopkins University Institute for Computational Medicine
Copyright 2012 University of Chicago

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
* 
* @author Josh Bryan
* 
*/

package org.globusonline.nexus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


import org.apache.log4j.BasicConfigurator;
import org.globusonline.nexus.exception.InvalidCredentialsException;
import org.globusonline.nexus.exception.NexusClientException;
import org.globusonline.nexus.exception.ValueErrorException;
import org.json.JSONException;
import org.json.JSONObject;

public class GoauthClient extends BaseNexusRestClient {

	public static void main(String[] args) {
		BasicConfigurator.configure();
		if (args.length != 4) {
			System.out
					.println("Usage: java "
							+ GoauthClient.class.getName()
							+ "<api host> <globusonline host> <client_id> <client_password>");
			System.exit(1);
		}

		// Create the client
		GoauthClient cli = new GoauthClient(args[0], args[1], args[2], args[3]);
		cli.setIgnoreCertErrors(true);

		try {

			// Generate login url
			System.out.println("Log in at: "
					+ cli.getLoginUrl("https://www.example.org"));

			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);

			// Enter code and exchange for access token
			System.out.println("Enter the code retrieved from redirect:");
			String code = in.readLine();

			JSONObject accessTokenJSON = cli
					.exchangeAuthCodeForAccessToken(code);
			String accessToken = accessTokenJSON.getString("access_token");

			System.out.println();
			System.out.println("Your access token is " + accessToken);
			System.out.println("It is valid for "
					+ accessTokenJSON.getInt("expires_in") + "seconds.");
			
			// We can validate the token by using this call:
			JSONObject tokenInfo = cli.validateAccessToken(accessToken);
			System.out.println("Token is valid.");
			
			// Now that we have exchanged the access token, we can do cool things 
			// like get user info
			JSONObject userInfo = cli.getUser(tokenInfo.getString("user_name"));
			System.out.println("Your email is: " + userInfo.getString("email"));
			
			// We can also get client only credentials
			accessTokenJSON = cli.getClientOnlyAccessToken();
			accessToken = accessTokenJSON.getString("access_token");
			System.out.println("Client only access token: " + accessToken);
			cli.validateAccessToken(accessToken);
			
			// We can also create a client that just uses a token directly without
			// logging in first.  This is useful if an access token will be received
			// from elsewhere.
			cli = new GoauthClient();
			cli.setIgnoreCertErrors(true);
			cli.setAccessToken(accessToken);
			cli.setNexusApiHost(args[0]);
			cli.setGlobusOnlineHost(args[1]);
			JSONObject user = cli.getCurrentUser();
			System.out.println("Clients email is: " + user.getString("email"));

		} catch (NexusClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	String globusOnlineHost = "https://www.globusonline.org";
	
	BasicAuthenticator goauthClientAuthenticator;

	public GoauthClient() {
		super();
	}

	/**
	 * @param clientId
	 * @param clientPassword
	 */
	public GoauthClient(String clientId, String clientPassword) {
		super();
		this.goauthClientAuthenticator = new BasicAuthenticator(clientId, clientPassword);
	}
	
	/**
	 * @param nexusApiUrl
	 * @param globusOnlineUrl
	 * @param clientId
	 * @param clientPassword
	 */
	public GoauthClient(String nexusApiUrl, String globusOnlineUrl,
			String clientId, String clientPassword) {
		super();
		this.nexusApiHost = nexusApiUrl;
		this.globusOnlineHost = globusOnlineUrl;
		this.goauthClientAuthenticator = new BasicAuthenticator(clientId, clientPassword);
	}
	
	/**
	 * @return the currentUser
	 * @throws NexusClientException 
	 */
	public JSONObject getCurrentUser() throws NexusClientException {
		JSONObject user = super.getCurrentUser();
		if (user == null) {
			try {
				GoauthAuthenticator auth = (GoauthAuthenticator)getAuthenticator();
				JSONObject tokenInfo = validateAccessToken(auth.getAccessToken());
				String username = tokenInfo.getString("user_name");
				user = getUser(username);
			} catch (ClassCastException e) {
				logger.error("Could not cast authenticator", e);
				throw new InvalidCredentialsException();
			} catch (JSONException e) {
				logger.error("Could not retrieve user name.", e);
				throw new ValueErrorException();
			}
		}
		return user;
	}
	
	/**
	 * Takes an authorization code and exchanges it for an access token
	 * 
	 * @param code
	 * @return
	 * @throws NexusClientException
	 */
	public JSONObject exchangeAuthCodeForAccessToken(String code)
			throws NexusClientException {
		try {
			String path = "/goauth/token?grant_type=authorization_code" + "&code="
					+ URLEncoder.encode(code, "UTF-8");
			JSONObject response = issueRestRequest(path, goauthClientAuthenticator);
			setAccessToken(response.getString("access_token"));
			return response;
		} catch (JSONException e) {
			logger.error("Error getting access_token", e);
			throw new ValueErrorException();
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported Encoding", e);
			throw new ValueErrorException();
		}
	}
	
	/**
	 * Issue an access token for a request conforming to the Client Credentials
	 * Grant flow described in section 4.4 of the OAuth 2.0 specification. This
	 * is allowing a client to create an access token for their own resources.
	 * 
	 * @return
	 * @throws NexusClientException
	 */
	public JSONObject getClientOnlyAccessToken() throws NexusClientException {
		String path = "/goauth/token?grant_type=client_credentials";
		JSONObject response = issueRestRequest(path, goauthClientAuthenticator);
		try {
			setAccessToken(response.getString("access_token"));
		} catch (JSONException e) {
			logger.error("Error getting access_token", e);
			throw new ValueErrorException();
		}
		return response;
	}

	public String getGlobusOnlineHost() {
		return globusOnlineHost;
	}

	public String getLoginUrl(String redirectUrl)
			throws NexusClientException {
		return getLoginUrl(redirectUrl, null);
	}

	public String getLoginUrl(String redirectUrl, String state)
			throws NexusClientException {
		try{
			String loginUrl = "https://" + this.globusOnlineHost
					+ "/OAuth?response_type=code" + "&client_id="
					+ URLEncoder.encode(this.goauthClientAuthenticator.getClientId(), "UTF-8") + "&redirect_uri="
					+ URLEncoder.encode(redirectUrl, "UTF-8");
	
			if (state != null) {
				loginUrl += "&state=" + URLEncoder.encode(state, "UTF-8");
			}
			return loginUrl;
		} catch (UnsupportedEncodingException e){
			logger.error("Unsupported Encoding", e);
			throw new ValueErrorException();
		}
	}

	public void setAccessToken(String token) {
		NexusAuthenticator auth = new GoauthAuthenticator(token);
		setAuthenticator(auth);
		//clear the current user
		setCurrentUser(null);
	}

	public void setGlobusOnlineHost(String globusOnlineHost) {
		this.globusOnlineHost = globusOnlineHost;
	}



	/**
	 * Validate a token and return a json object representing the fields
	 * associated with this access token.
	 * 
	 * @param token
	 * @return
	 * @throws NexusClientException
	 */
	public JSONObject validateAccessToken(String token)
			throws NexusClientException {
		try {
			String path = "/goauth/validate?token="
					+ URLEncoder.encode(token, "UTF-8");
			return issueRestRequest(path);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			logger.error("Unsupported Encoding", e);
			throw new ValueErrorException();
		}
	}
}
