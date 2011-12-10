package net.nightwhistler.pageturner.sync;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;



public class PageTurnerWebProgressService implements ProgressService {
	
	private static final Logger LOG = LoggerFactory.getLogger(PageTurnerWebProgressService.class);	
	
	private String userId;
	
	private HttpClient client;
	private HttpContext context;
	
	private static final String BASE_URL = "https://ostara.nightwhistler.net/pageturner/progress/";
	private static final int HTTP_SUCCESS = 200;
	
	public PageTurnerWebProgressService(Context context) {		
		this.context = new BasicHttpContext();
		this.client = new SSLHttpClient(context);			
	}	
	
	@Override
	public void setEmail(String email) {
		this.userId = email;	
	}
	
	@Override
	public BookProgress getProgress(String fileName) {
		
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
			
			JSONObject json = new JSONObject(responseString);
			
			int index = json.getInt("bookIndex");
			int progress = json.getInt("progress"); 
			
			return new BookProgress(fileName, index, progress );
			
		} catch (Exception e) {
			LOG.error( "Got error while querying server", e );
			return null;
		} 		
		
	}
	
	@Override
	public void storeProgress(String fileName, int index, int progress) {
				
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
			
			post.setEntity( new UrlEncodedFormEntity(pairs) );			
			
			HttpResponse response = client.execute(post, this.context);
			
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
