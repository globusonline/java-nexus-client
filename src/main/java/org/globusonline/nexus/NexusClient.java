/*
Copyright 2012 Johns Hopkins University Institute for Computational Medicine

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


package org.globusonline.nexus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.globusonline.nexus.exception.NexusClientException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
Root object for interacting with the Nexus service
*/
public class NexusClient {

	private GlobusOnlineRestClient restClient;
	
	private JSONObject workingUser = null;
	
	public NexusClient() throws NexusClientException{
		
		initialize();
		
	}
	
	private void initialize() throws NexusClientException{
		
		restClient = new GlobusOnlineRestClient();
		
	}
	
	public boolean authenticateUserPassword(String username, String password) throws NexusClientException{
		
		JSONObject result = restClient.usernamePasswordLogin(username, password);
		return (result != null);
	}
	
//Public User Operations*******************************************
	
	public boolean isEmailValidated(String userId, UUID groupId) throws NexusClientException{
		
			return isJsonEmailValidated(getWorkingUser(groupId, userId));
	}
	
	public boolean isOptIn(String userId, UUID groupId) throws NexusClientException{
		
			return isJsonOptIn(getWorkingUser(groupId, userId));
	}
	
	public String getUserFullname(String userId, UUID groupId) throws NexusClientException{
		
			return getJsonFullname(getWorkingUser(groupId, userId));
	}
	

	
//Public Group Operations******************************************
	
	public UUID getRootGroupId() throws NexusClientException{
		
		UUID uuid;
		String rootId = "";
		String depth = "1";
		
		JSONObject json = restClient.getGroupList(depth);
		try {
			rootId = json.getString("id");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		uuid = UUID.fromString(rootId);
		return uuid;		
	}
	
	public boolean hasChildGroups(UUID rootId) throws NexusClientException{
		String depth = "1";
		JSONObject json = restClient.getGroupList(rootId, depth);
		
		return json.has("children");
	}
	
	public List<UUID> getChildGroupIds(UUID rootId) throws NexusClientException{
		
		String depth = "1";
		List<UUID> groupList = new ArrayList<UUID>();
		JSONObject json = restClient.getGroupList(rootId, depth);
		JSONArray jsonArray = null;
		int arrayLength = 0;

		if(!hasChildGroups(rootId)){
			return null;
		}
		
		try {
			jsonArray = json.getJSONArray("children");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Array is " + jsonArray.toString());
		
		arrayLength = jsonArray.length();
		
		for(int i = 0; i < arrayLength; i++){
			try {
				JSONObject item = (JSONObject) jsonArray.get(i);
				groupList.add(UUID.fromString(item.getString("id")));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		return groupList;	
	}
	
	public String getGroupNameById(UUID groupId) throws NexusClientException{
		String name = "";
		
		JSONObject group = restClient.getGroupSummary(groupId);
		
		try {
			name = group.getString("name");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return name;
	}
	
	public UUID getGroupIdByName(String name) throws NexusClientException{
		
		return getGroupIdByName(name, getRootGroupId());
	}
	
	public UUID getGroupIdByName(String name, UUID parentId) throws NexusClientException{//assumes names must be unique	
		
		UUID uuid = null;
		String parentName = "";
		JSONObject rootObject = null;
		List<UUID> childGroupIds;
		
		if(parentId == null){
			parentId = getRootGroupId();
		}
		parentName = getGroupNameById(parentId);
		
		if(parentName.equals(name)){
			return parentId;
		}

		childGroupIds = getChildGroupIds(parentId);
		
		for(UUID childUUID : childGroupIds){
			if(getGroupNameById(childUUID).equals(name)){
				return childUUID;
			}
			if(hasChildGroups(childUUID)){
				UUID grandchildUUID = getGroupIdByName(name, childUUID);
				if(grandchildUUID != null){
					return grandchildUUID;
				}
			}
		}
		
		return null;
	}
	
//private user utility methods *********************************

	private boolean isJsonOptIn(JSONObject userObject){
		return getJsonBooleanValueFromKey(userObject, "opt_in");
	}
	
	private boolean isJsonEmailValidated(JSONObject userObject){
		return getJsonBooleanValueFromKey(userObject, "email_validated");
	}
	
	private String getJsonFullname(JSONObject userObject){
		return getJsonStringValueFromKey(userObject, "fullname");
	}
	
	private String getJsonEmail(JSONObject userObject){
		
		return getJsonStringValueFromKey(userObject, "email");
	}

	private String getJsonUsername(JSONObject userObject){
		
		return getJsonStringValueFromKey(userObject, "username");
	}
		
	private JSONObject getWorkingUser(UUID groupId, String username) throws NexusClientException{
		if(this.workingUser == null){
			this.workingUser = restClient.getGroupMember(groupId, username);
		}
		
		else if(getJsonUsername(workingUser).equals(username)){
			this.workingUser = restClient.getGroupMember(groupId, username);
		}
		else{
			this.workingUser = restClient.getGroupMember(groupId, username);
		}
		
		return this.workingUser;
	}

//	General Utility methods**********************************
	
	private boolean getJsonBooleanValueFromKey(JSONObject jsonObject, String key){
		
		boolean value = false;
		
		try {
			value = jsonObject.getBoolean(key);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return value;
		
	}
	
	private String getJsonStringValueFromKey(JSONObject jsonObject, String key){
		
		String value = "";
		
		try {
			value = jsonObject.getString(key);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return value;
	}
}
