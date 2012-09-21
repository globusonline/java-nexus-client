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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

public class FileSystemCache implements NexusCache{

//    Cache signing certificates to the filesystem.
  
	private String cachePath;
	
	public FileSystemCache(String path){
		
		init(path);
	}
	
    private void init(String cachePath){
        this.cachePath = cachePath;
        File file = new File(cachePath);
        if (!file.exists()){
           file.mkdir();
        }
    }

    public void savePublicKey(String keyId, RSAPublicKey key){
    	
        String cachedCertPath = cachePath + keyId + ".pem";

        try {
        	FileOutputStream outStream = new FileOutputStream(cachedCertPath);
			outStream.write(key.getEncoded());
	        outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public boolean hasPublicKey(String keyId){
    	String cachedCertPath = cachePath + keyId + ".pem";
    	File file = new File(cachedCertPath);
        return file.exists();
    }

    public RSAPublicKey getPublicKey(String keyId){
    	String cachedCertPath = cachePath + keyId + ".pem";
    	byte[] encodedPublicKey = null;

    	try {
        	File file = new File(cachedCertPath);
        	FileInputStream inStream = new FileInputStream(file);
        	encodedPublicKey = new byte[(int) file.length()];
        	inStream.read(encodedPublicKey);
			inStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	KeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
    	RSAPublicKey key = null;
    	
		try {
			key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return key;

    }
}
