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

import org.apache.log4j.Logger;

public class LoggingCacheWrapper {

	private NexusCache cache;
	static org.apache.log4j.Logger logger = Logger.getLogger(LoggingCacheWrapper.class);
	
	public LoggingCacheWrapper(NexusCache cache){
		init(cache);
	}
	
    private void init(NexusCache cache){
        this.cache = cache;
    }

    public void savePublicKey(String keyId, RSAPublicKey key){
    	String cacheType = cache.getClass().getName();
    	
        String message = cacheType + ": Saving public key " + keyId + ":" + key.toString();
        logger.debug(message);
        cache.savePublicKey(keyId, key);
    }

    public boolean hasPublicKey(String keyId){
        return cache.hasPublicKey(keyId);
    }
    
    public RSAPublicKey getPublicKey(String keyId){
    	String cacheType = cache.getClass().getName();
    	
    	String message = cacheType + ": Getting public key " + keyId;

        logger.debug(message);
        return cache.getPublicKey(keyId);
    }
}
