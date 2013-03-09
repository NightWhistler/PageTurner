package net.nightwhistler.pageturner.catalog;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class HttpClient {

	public InputStream getURL(String url) throws ClientProtocolException, IOException {		
		HttpParams httpParams = new BasicHttpParams();
		DefaultHttpClient client = new DefaultHttpClient(httpParams);		
		return getStream(url, client);		
	}
	
	public InputStream getUrl(String url, String username, String password) throws ClientProtocolException, IOException {
		HttpParams httpParams = new BasicHttpParams();
		DefaultHttpClient client = new DefaultHttpClient(httpParams);
		client.getCredentialsProvider().setCredentials(
				new AuthScope(null, -1),
				new UsernamePasswordCredentials(username, password));
		
		return getStream(url, client);
	}
	
	private static InputStream getStream(String url, DefaultHttpClient client) throws ClientProtocolException, IOException  {
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		
		return response.getEntity().getContent();
	}
	
}
