package org.globusonline.nexus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


import org.apache.log4j.BasicConfigurator;
import org.globusonline.nexus.exception.NexusClientException;
import org.globusonline.nexus.exception.ValueErrorException;
import org.json.JSONException;
import org.json.JSONObject;

public class GoauthClient extends BaseNexusRestClient {

	String globusOnlineHost = "https://www.globusonline.org";
	String clientId;
	String clientPassword;
	
	NexusAuthenticator goauthClientAuthenticator;
	
	public String getGlobusOnlineUrl() {
		return globusOnlineHost;
	}

	public void setGlobusOnlineUrl(String globusOnlineHost) {
		this.globusOnlineHost = globusOnlineHost;
	}

	/**
	 * @param nexusApiUrl
	 * @param globusOnlineUrl
	 * @param clientId
	 * @param clientPassword
	 */
	public GoauthClient(String nexusApiUrl, String globusOnlineUrl,
			String clientId, String clientPassword) {
		this.nexusApiHost = nexusApiUrl;
		this.globusOnlineHost = globusOnlineUrl;
		this.clientId = clientId;
		this.clientPassword = clientPassword;
		this.goauthClientAuthenticator = new BasicAuthenticator(clientId, clientPassword);
	}
	
	public void setAccessToken(String token) {
		NexusAuthenticator auth = new GoauthAuthenticator(token);
		setAuthenticator(auth);
	}

	/**
	 * @param clientId
	 * @param clientPassword
	 */
	public GoauthClient(String clientId, String clientPassword) {
		this.clientId = clientId;
		this.clientPassword = clientPassword;
	}

	public String getLoginUrl(String redirectUrl)
			throws UnsupportedEncodingException {
		return getLoginUrl(redirectUrl, null);
	}

	public String getLoginUrl(String redirectUrl, String state)
			throws UnsupportedEncodingException {
		String loginUrl = "https://" + this.globusOnlineHost
				+ "/OAuth?response_type=code" + "&client_id="
				+ URLEncoder.encode(this.clientId, "UTF-8") + "&redirect_uri="
				+ URLEncoder.encode(redirectUrl, "UTF-8");

		if (state != null) {
			loginUrl += "&state=" + URLEncoder.encode(state, "UTF-8");
		}
		return loginUrl;
	}

	/**
	 * Takes an authorization code and exchanges it for an access token
	 * 
	 * @param code
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NexusClientException
	 */
	public JSONObject exchangeAuthCodeForAccessToken(String code)
			throws UnsupportedEncodingException, NexusClientException {
		String path = "/goauth/token?grant_type=authorization_code" + "&code="
				+ URLEncoder.encode(code, "UTF-8");
		JSONObject response = issueRestRequest(path, goauthClientAuthenticator);
		try {
			setAccessToken(response.getString("access_token"));
		} catch (JSONException e) {
			logger.error("Error getting access_token", e);
			throw new ValueErrorException();
		}
		return response;
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

	/**
	 * Validate a token and return a json object representing the fields
	 * associated with this access token.
	 * 
	 * @param token
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NexusClientException
	 */
	public JSONObject validateAccessToken(String token)
			throws UnsupportedEncodingException, NexusClientException {
		String path = "/goauth/validate?token="
				+ URLEncoder.encode(token, "UTF-8");
		return issueRestRequest(path);
	}



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

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NexusClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
