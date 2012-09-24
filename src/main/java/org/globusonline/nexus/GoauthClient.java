package org.globusonline.nexus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.globusonline.nexus.exception.InvalidCredentialsException;
import org.globusonline.nexus.exception.InvalidUrlException;
import org.globusonline.nexus.exception.NexusClientException;
import org.globusonline.nexus.exception.ValueErrorException;
import org.json.JSONException;
import org.json.JSONObject;

public class GoauthClient {

	String nexusApiHost = "https://nexus.api.globusonline.org";
	String globusOnlineHost = "https://www.globusonline.org";
	String clientId;
	String clientPassword;
	boolean ignoreCertErrors = false;

	// Create a trust manager that does not validate certificate chains
	static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			return;
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			return;
		}
	} };
	
	// Create all-trusting host name verifier
	HostnameVerifier allHostsValid = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};


	static org.apache.log4j.Logger logger = Logger
			.getLogger(GoauthClient.class);

	public String getNexusApiUrl() {
		return nexusApiHost;
	}

	public void setNexusApiUrl(String nexusApiHost) {
		this.nexusApiHost = nexusApiHost;
	}

	public String getGlobusOnlineUrl() {
		return globusOnlineHost;
	}

	public void setGlobusOnlineUrl(String globusOnlineHost) {
		this.globusOnlineHost = globusOnlineHost;
	}

	public boolean isIgnoreCertErrors() {
		return ignoreCertErrors;
	}

	public void setIgnoreCertErrors(boolean ignoreCertErrors) {
		this.ignoreCertErrors = ignoreCertErrors;
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

	public JSONObject exchangeAuthCodeForAccessToken(String code)
			throws UnsupportedEncodingException, NexusClientException {
		String path = "/goauth/token?grant_type=authorization_code" + "&code="
				+ URLEncoder.encode(code, "UTF-8");
		return issueRestRequest(path);
	}

	private JSONObject issueRestRequest(String path) throws NexusClientException {

		JSONObject json = null;

		String httpMethod = "GET";
		String contentType = "application/json";
		String accept = "application/json";

		HttpsURLConnection connection;
		int responseCode;

		try {
			
			URL url = new URL("https://" + nexusApiHost + path);

			connection = (HttpsURLConnection) url.openConnection();
			
			if (ignoreCertErrors) {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new SecureRandom());
				connection.setSSLSocketFactory(sc.getSocketFactory());
				connection.setHostnameVerifier(allHostsValid);
			}
			
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod(httpMethod);
			connection.setRequestProperty("Content-Type", contentType);
			connection.setRequestProperty("Accept", accept);
			// connection.setRequestProperty("X-Go-Community-Context",
			// community);

			String userpassword = clientId + ":" + clientPassword;
			String encodedAuthorization = DatatypeConverter
					.printBase64Binary(userpassword.getBytes());
			connection.setRequestProperty("Authorization", "Basic "
					+ encodedAuthorization);

			responseCode = connection.getResponseCode();

		} catch (Exception e) {
			logger.error("Unhandled connection error:", e);
			throw new ValueErrorException();
		}

		logger.info("ConnectionURL: " + connection.getURL());

		if (responseCode == 403) {
			logger.error("Access is denied.  Invalid credentials.");
			throw new InvalidCredentialsException();
		}
		if (responseCode == 404) {
			logger.error("URL not found.");
			throw new InvalidUrlException();
		}
		if (responseCode == 500) {
			logger.error("Internal Server Error.");
			throw new ValueErrorException();
		}
		if (responseCode != 200) {
			logger.info("Response code is: " + responseCode);
		}

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String decodedString = in.readLine();
	
			json = new JSONObject(decodedString);
		} catch (JSONException e) {
			logger.error("JSON Error", e);
			throw new ValueErrorException();
		} catch (IOException e) {
			logger.error("IO Error", e);
			throw new ValueErrorException();
		}

		return json;
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
			
			
			JSONObject accessTokenJSON = cli.exchangeAuthCodeForAccessToken(code);
			String accessToken = accessTokenJSON.getString("access_token");
			
			System.out.println("Your access token is " + accessToken);
			
			
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
