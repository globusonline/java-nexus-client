package org.globusonline.nexus;

import javax.net.ssl.HttpsURLConnection;

public interface NexusAuthenticator {

	public void authenticate(HttpsURLConnection con);
	
}
