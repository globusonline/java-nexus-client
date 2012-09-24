package org.globusonline.nexus;

import javax.net.ssl.HttpsURLConnection;

public class GoauthAuthenticator implements NexusAuthenticator {

	public GoauthAuthenticator(String accessToken) {
		this.accessToken = accessToken;
	}

	private String accessToken;

	@Override
	public void authenticate(HttpsURLConnection con) {
		con.setRequestProperty("Authorization", "Globus-Goauthtoken " + accessToken);
	}

}
