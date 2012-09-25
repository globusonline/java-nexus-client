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
