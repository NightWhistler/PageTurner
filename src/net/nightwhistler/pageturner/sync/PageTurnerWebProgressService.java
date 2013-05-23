/*
 * Copyright (C) 2011 Alex Kuiper
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
package net.nightwhistler.pageturner.sync;

import android.content.Context;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.inject.ContextSingleton;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@ContextSingleton
public class PageTurnerWebProgressService implements ProgressService {
	
	private static final Logger LOG = LoggerFactory.getLogger(PageTurnerWebProgressService.class);	
	
	private Configuration config;
	
	private HttpClient client;
	private HttpContext httpContext;
	
	private static final int HTTP_SUCCESS = 200;
	private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
	
	private SimpleDateFormat dateFormat;

	@Inject
	public PageTurnerWebProgressService(Context context, Configuration config, HttpClient client) {
		this.httpContext = new BasicHttpContext();
		this.config = config;
        this.client = client;
		
		this.dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		// explicitly set timezone of input if needed
		dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
    }


	@Override
	public List<BookProgress> getProgress(String fileName) throws AccessException {
		
		String userId = this.config.getSynchronizationEmail();
		String accessKey = this.config.getSynchronizationAccessKey();
		
		if ( "".equals( userId ) || "".equals(fileName) ) {
			LOG.debug( "Empty username or filename. Aborting sync. (" + userId + " / " + fileName + ")" );
			return null;
		}
		
		String key = computeKey(fileName);
		
		LOG.debug( "Doing progress query for key: " + key );		
		
		HttpGet get = new HttpGet( config.getSyncServerURL() + key + "?accessKey=" + URLEncoder.encode(accessKey) );
        get.setHeader("User-Agent", config.getUserAgent() );
		
		try {
			HttpResponse response = client.execute(get);
			
			int statusCode = response.getStatusLine().getStatusCode();
			LOG.debug( "Got status " + statusCode + " from server.");
			
			if ( statusCode == HTTP_FORBIDDEN ) {
				throw new AccessException( EntityUtils.toString(response.getEntity()) );
			}

            if ( statusCode == HTTP_NOT_FOUND ) {
                return new ArrayList<BookProgress>();
            }
			
			if ( statusCode != HTTP_SUCCESS ) {
				return null;
			}
			
			String responseString = EntityUtils.toString(response.getEntity());
			
			JSONArray jsonArray = new JSONArray(responseString);
			
			List<BookProgress> result = new ArrayList<BookProgress>();
			
			for ( int i=0; i < jsonArray.length(); i++ ) {
				
				JSONObject json = jsonArray.getJSONObject(i);
				
				int index = json.getInt("bookIndex");
				int progress = json.getInt("progress");
				int percentage = json.getInt("percentage");
				
				Date timeStamp = dateFormat.parse( json.getString("storedOn") );
				
				String deviceName = json.getString("deviceName");
			
				result.add( new BookProgress(fileName, index, progress, percentage, timeStamp, deviceName ) );
			}
			
			return result;
			
		} catch (IOException e) {
			LOG.error( "Got error while querying server", e );
			return null;
		} catch (JSONException json ) {
			LOG.error( "Error reading response", json );
			return null;
		} catch (ParseException p ) {
			LOG.error( "Invalid date", p );
			return null;
		}
		
	}
	
	@Override
	public void storeProgress(String fileName, int index, int progress, int percentage) {
				
		if ( ! config.isSyncEnabled() ) {
			return;
		}	
		
		String key = computeKey(fileName);
				
		HttpPost post = new HttpPost( config.getSyncServerURL() + key );
		
		String filePart = fileName;
		
		if ( fileName.indexOf("/") != -1 ) {
			filePart = fileName.substring( fileName.lastIndexOf('/') );
		}
		
		try {
			
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add( new BasicNameValuePair("bookIndex", "" + index ) );
			pairs.add(new BasicNameValuePair("progress", "" + progress ));
			pairs.add(new BasicNameValuePair("title", Integer.toHexString( filePart.hashCode() )));
			pairs.add(new BasicNameValuePair("deviceName", this.config.getDeviceName() ));
			pairs.add(new BasicNameValuePair("percentage", "" + percentage ));
			pairs.add(new BasicNameValuePair("userId", Integer.toHexString( this.config.getSynchronizationEmail().hashCode() )));
			pairs.add(new BasicNameValuePair("accessKey", this.config.getSynchronizationAccessKey()));
			
			post.setEntity( new UrlEncodedFormEntity(pairs) );
            post.setHeader("User-Agent", config.getUserAgent() );

			
			HttpResponse response = client.execute(post, this.httpContext);
			
			if ( response.getStatusLine().getStatusCode() == HTTP_FORBIDDEN ) {
				throw new AccessException( EntityUtils.toString(response.getEntity()) );
			}
			
			LOG.debug("Got status " + response.getStatusLine().getStatusCode() + " from server.");
			
		} catch (Exception io) {	
			LOG.error("Got error while POSTing update:", io);	
			//fail silently
		}	
		
	}	
	
	private String computeKey( String fileName ) {		
		
		String filePart = fileName;
		
		if ( fileName.indexOf("/") != -1 ) {
			filePart = fileName.substring( fileName.lastIndexOf('/') );
		}
		
		String plainTextKey = this.config.getSynchronizationEmail() + ":" + filePart;
		
		String hash = Integer.toHexString( plainTextKey.hashCode() );
		
		return hash;		
	}
}
