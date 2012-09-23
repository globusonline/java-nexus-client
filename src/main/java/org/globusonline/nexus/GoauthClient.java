package main.java.org.globusonline.nexus;

public class GoauthClient {
	
	String nexusApiUrl = "https://nexus.api.globusonline.org";
	String globusOnlineUrl = "https://www.globusonline.org";
	String clientId;
	String clientPassword;

	/**
	 * @param nexusApiUrl
	 * @param globusOnlineUrl
	 * @param clientId
	 * @param clientPassword
	 */
	public GoauthClient(String nexusApiUrl, String globusOnlineUrl,
			String clientId, String clientPassword) {
		this.nexusApiUrl = nexusApiUrl;
		this.globusOnlineUrl = globusOnlineUrl;
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
	
	public String getLoginUrl(String redirectUrl){
		
		return loginUrl;
	}
	
}
