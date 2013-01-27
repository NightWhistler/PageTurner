package net.nightwhistler.pageturner;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomOPDSSite {

	private String url;
	private String name;
	private String description;
	
	private String userName;
	private String password;
		
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public JSONObject toJSON() {
		
		try {
			JSONObject obj = new JSONObject();
			obj.put("url", url);
			obj.put("name", name);
			obj.put("description", description);
						
			obj.put("userName", userName);
			obj.put("password", password);
			
			return obj;
			
		} catch (JSONException json) {
			return null;
		}		
	}
	
	public static CustomOPDSSite fromJSON(JSONObject json) {
		try {
			CustomOPDSSite site = new CustomOPDSSite();
			site.setUrl(json.getString("url"));
			site.setName(json.getString("name"));
			
			site.setDescription(json.optString("description"));			
			site.setUserName( json.optString("userName") );
			site.setPassword( json.optString("password"));
			
			return site;
		} catch (JSONException js) {
			return null;
		}
	}
	
}
