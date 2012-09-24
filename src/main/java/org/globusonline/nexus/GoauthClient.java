package org.globusonline.nexus;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
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
		String loginUrl = "https://" + this.globusOnlineHost + "/OAuth?response_type=code"
				+ "&client_id=" + URLEncoder.encode(this.clientId, "UTF-8")
				+ "&redirect_uri=" + URLEncoder.encode(redirectUrl, "UTF-8");

		if (state != null) {
			loginUrl += "&state=" + URLEncoder.encode(state, "UTF-8");
		}
		return loginUrl;
	}

	public JSONObject exchangeAuthCodeForAccessToken(String code)
			throws UnsupportedEncodingException {
		String path = "/goauth/token?grant_type=authorization_code" + "&code="
				+ URLEncoder.encode(code, "UTF-8");
		return issueRestRequest(path);
	}

	private JSONObject issueRestRequest(String path) {

		JSONObject json = null;

		String httpMethod = "GET";
		String contentType = "application/json";
		String accept = "application/json";

		try {
			SSLContext sc = SSLContext.getInstance("SSL");

			if (ignoreCertErrors) {
				sc.init(null, trustAllCerts, new SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc
						.getSocketFactory());
			}

			URL url = new URL("https://" + nexusApiHost + path);

			HttpsURLConnection connection = (HttpsURLConnection) url
					.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod(httpMethod);
			connection.setRequestProperty("Content-Type", contentType);
			connection.setRequestProperty("Accept", accept);
			// connection.setRequestProperty("X-Go-Community-Context",
			// community);

			String userpassword = clientId + ":" + clientPassword;
			String encodedAuthorization = DatatypeConverter.printBase64Binary(userpassword.getBytes());
			connection.setRequestProperty("Authorization", "Basic "
					+ encodedAuthorization);

			System.out.println("ConnectionURL: " + connection.getURL());

			// if(params != null){
			// OutputStreamWriter out = new
			// OutputStreamWriter(connection.getOutputStream());
			//
			// if(contentType.equals("application/x-www-form-urlencoded")){
			// // body = urllib.urlencode(params)
			// }
			// else {
			// body = params.toString();
			// }
			// out.write(body);
			// System.out.println("Body:" + body);
			// out.close();
			// }

			if (connection.getResponseCode() == 203) {
				logger.error("Access is denied.  Invalid credentials.");
				throw new Exception();
			}
			if (connection.getResponseCode() == 204) {
				logger.error("Authentciation URL invalid.");
				throw new Exception();
			}
			if (connection.getResponseCode() == 500) {
				logger.error("Internal Server Error.");
				throw new Exception();
			}
			if (connection.getResponseCode() != 200) {
				System.out.println("Response code is: "
						+ connection.getResponseCode());
				throw new Exception();
			} else {

				BufferedReader in = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				String decodedString = in.readLine();

				json = new JSONObject(decodedString);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}
	
	public static void main(String[] args){
		if (args.length != 4){
			System.out.println("Usage: java " + GoauthClient.class.getName() + "<api host> <globusonline host> <client_id> <client_password>");
			System.exit(1);
		}
		
		GoauthClient cli = new GoauthClient(args[0], args[1], args[2], args[3]);
		
		try {
			System.out.println("Log in at: " + cli.getLoginUrl("https://www.example.org") );
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
