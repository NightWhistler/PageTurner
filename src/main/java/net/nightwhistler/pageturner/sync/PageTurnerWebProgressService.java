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

import java.io.InputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.nightwhistler.pageturner.R;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.inject.ContextScoped;
import android.content.Context;

import com.google.inject.Inject;


@ContextScoped
public class PageTurnerWebProgressService implements ProgressService {
	
	private static final Logger LOG = LoggerFactory.getLogger(PageTurnerWebProgressService.class);	
	
	private String userId;
	private String deviceId;
	
	private HttpClient client;
	private HttpContext context;
	
	private static final String BASE_URL = "http://api.pageturner-reader.org/progress/";
	private static final int HTTP_SUCCESS = 200;
	
	private SimpleDateFormat dateFormat;
	
	@Inject
	public PageTurnerWebProgressService(Context context) {		
		this.context = new BasicHttpContext();
		this.client = new SSLHttpClient(context);	
		
		this.dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		// explicitly set timezone of input if needed
		dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));	

	}	
	
	@Override
	public void setEmail(String email) {
		this.userId = email;	
	}
	
	@Override
	public List<BookProgress> getProgress(String fileName) {
		
		if ( "".equals( this.userId) || "".equals(fileName) ) {
			LOG.debug( "Empty username or filename. Aborting sync. (" + this.userId + " / " + fileName + ")" );
			return null;
		}
		
		String key = computeKey(fileName);
		
		LOG.debug( "Doing progress query for key: " + key );
		
		HttpGet get = new HttpGet( BASE_URL + key );
		
		try {
			HttpResponse response = client.execute(get);
			
			int statusCode = response.getStatusLine().getStatusCode();
			LOG.debug( "Got status " + statusCode + " from server.");
			
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
			
		} catch (Exception e) {
			LOG.error( "Got error while querying server", e );
			return null;
		} 		
		
	}
	
	@Override
	public void storeProgress(String fileName, int index, int progress, int percentage) {
				
		if ( "".equals( this.userId) ) {
			return;
		}	
		
		String key = computeKey(fileName);
				
		HttpPost post = new HttpPost( BASE_URL + key );
		
		String filePart = fileName;
		
		if ( fileName.indexOf("/") != -1 ) {
			filePart = fileName.substring( fileName.lastIndexOf('/') );
		}
		
		try {
			
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add( new BasicNameValuePair("bookIndex", "" + index ) );
			pairs.add(new BasicNameValuePair("progress", "" + progress ));
			pairs.add(new BasicNameValuePair("title", filePart ));
			pairs.add(new BasicNameValuePair("deviceName", this.deviceId ));
			pairs.add(new BasicNameValuePair("percentage", "" + percentage ));
			pairs.add(new BasicNameValuePair("userId", this.userId ));
			
			post.setEntity( new UrlEncodedFormEntity(pairs) );			
			
			HttpResponse response = client.execute(post, this.context);
			
			LOG.debug("Got status " + response.getStatusLine().getStatusCode() + " from server.");
			
		} catch (Exception io) {	
			LOG.error("Got error while POSTing update:", io);	
			//fail silently
		}	
		
	}
	
	@Override
	public void setDeviceName(String deviceName) {
		this.deviceId = deviceName;		
	}
	
	private String computeKey( String fileName ) {		
		
		String filePart = fileName;
		
		if ( fileName.indexOf("/") != -1 ) {
			filePart = fileName.substring( fileName.lastIndexOf('/') );
		}
		
		String plainTextKey = this.userId + ":" + filePart;
		
		String hash = Integer.toHexString( plainTextKey.hashCode() );
		
		return hash;		
	}
	
	public class SSLHttpClient extends DefaultHttpClient {

		final Context context;

		public SSLHttpClient(Context context) {
			this.context = context;
		}

		@Override protected ClientConnectionManager createClientConnectionManager() {
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(
					new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", newSSlSocketFactory(), 443));
			return new SingleClientConnManager(getParams(), registry);
		}

		private SSLSocketFactory newSSlSocketFactory() {
			try {
				KeyStore trusted = KeyStore.getInstance("BKS");
				InputStream in = context.getResources().openRawResource(R.raw.pageturner);
				try {
					trusted.load(in, "pageturner".toCharArray());
				} finally {
					in.close();
				}
				return new SSLSocketFactory(trusted);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}
	}

	
	
}
