package org.globusonline.nexus.utilities;

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
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

public class InMemoryCache implements NexusCache {

	// Simple cache implementation for signing certificates.

	Map<String, RSAPublicKey> cacheMap;

	public InMemoryCache() {
		init();
	}

	private void init() {
		cacheMap = new HashMap<String, RSAPublicKey>();
	}

	public void savePublicKey(String keyId, RSAPublicKey key) {
		this.cacheMap.put(keyId, key);
	}

	public boolean hasPublicKey(String keyId) {
		return this.cacheMap.containsKey(keyId);
	}

	public RSAPublicKey getPublicKey(String keyId) {

		RSAPublicKey rsaKey = cacheMap.get(keyId);
		return rsaKey;
	}

}
