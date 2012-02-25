package net.nightwhistler.pageturner.activity;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Stack;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.Nucular;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.catalog.CatalogListAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class CatalogActivity extends ListActivity {

	//private static final String BASE_URL = "http://m.gutenberg.org/ebooks/?format=opds";
	private static final String BASE_URL = "http://www.feedbooks.com/catalog.atom";
	
	private CatalogListAdapter adapter;
	
	private ProgressDialog waitDialog;
	
	private static final Logger LOG = LoggerFactory.getLogger(CatalogActivity.class); 
	
	private Stack<String> navStack = new Stack<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		this.adapter = new CatalogListAdapter(this);
		setListAdapter(adapter);
		
		this.waitDialog = new ProgressDialog(this);
        this.waitDialog.setOwnerActivity(this);      
		super.onCreate(savedInstanceState);
		
		new LoadOPDSTask().execute(BASE_URL);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		Entry entry = adapter.getItem(position);
		
		List<Link> links = entry.getLinks();
				
		for ( Link link: links ) {
			if ( link.getType().startsWith("application/atom+xml")) {
				String href = link.getHref();
				loadURL(href);
				return;
			}
		}
		
	}
	
	private void loadURL( String url ) {
		
		String base = BASE_URL;
		
		if ( ! navStack.isEmpty() ) {
			base = navStack.peek();
		}
		
		try {
			String target = new URL(new URL(base), url).toString();
			LOG.info("Loading " + target );
			
			navStack.push(target);
			new LoadOPDSTask().execute(target);
		} catch (MalformedURLException u ) {
			LOG.error("Malformed URL:", u);
		}
	}
	
	@Override
	public void onBackPressed() {
		if ( ! navStack.isEmpty() ) {
			navStack.pop();
		}
		
		if ( navStack.isEmpty() ) {
			new LoadOPDSTask().execute(BASE_URL);
		} else {
			new LoadOPDSTask().execute(navStack.peek());
		}
	}
	
	
	private class LoadOPDSTask extends AsyncTask<String, Integer, Feed> {
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(getString(R.string.loading_wait));
	    	waitDialog.show();
		}
		
		@Override
		protected Feed doInBackground(String... params) {
			
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet( params[0] );
			
			try {
				HttpResponse response = client.execute(get);
				Feed feed = Nucular.readFromStream( response.getEntity().getContent() );
				return feed;
			} catch (Exception e) {
				LOG.error("Download failed.", e);
				return null;
			}				
			
		}
		
		@Override
		protected void onPostExecute(Feed result) {
			if ( result != null ) {
				adapter.setFeed(result);
			} 
			//else show popup
			waitDialog.hide();
		}
		
	}
	
}
