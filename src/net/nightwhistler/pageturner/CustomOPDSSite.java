/*
 * Copyright (C) 2013 Alex Kuiper
 *
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */

package net.nightwhistler.pageturner;

import jedi.option.Option;
import org.json.JSONException;
import org.json.JSONObject;

import static jedi.option.Options.none;
import static jedi.option.Options.some;

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

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("url", url);
        obj.put("name", name);
        obj.put("description", description);

        obj.put("userName", userName);
        obj.put("password", password);

        return obj;
    }

    public static CustomOPDSSite fromJSON(JSONObject json) throws JSONException {
        CustomOPDSSite site = new CustomOPDSSite();
        site.setUrl(json.getString("url"));
        site.setName(json.getString("name"));

        site.setDescription(json.optString("description"));
        site.setUserName( json.optString("userName") );
        site.setPassword( json.optString("password"));

        return site;
    }

}
