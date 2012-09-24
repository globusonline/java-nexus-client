package org.globusonline.nexus;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;

public class BasicAuthenticator implements NexusAuthenticator {

	public BasicAuthenticator(String clientId, String clientPassword) {
		this.clientId = clientId;
		this.clientPassword = clientPassword;
	}

	private String clientId;
	private String clientPassword;

	@Override
	public void authenticate(HttpsURLConnection con) {
		// TODO Auto-generated method stub
		String userpassword = clientId + ":" + clientPassword;
		String encodedAuthorization = DatatypeConverter
				.printBase64Binary(userpassword.getBytes());
		con.setRequestProperty("Authorization", "Basic "
				+ encodedAuthorization);
	}

}
