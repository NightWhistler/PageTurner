package net.nightwhistler.pageturner.activity;


import java.util.List;
import java.util.Stack;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.Nucular;
import net.nightwhistler.pageturner.catalog.CatalogListAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class CatalogActivity extends ListActivity {

	private static final String BASE_URL = "http://www.feedbooks.com/catalog.atom";
	
	private CatalogListAdapter adapter;
	
	private Stack<String> navStack = new Stack<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		this.adapter = new CatalogListAdapter(this);
		setListAdapter(adapter);
		
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
				navStack.push(href);
				new LoadOPDSTask().execute(href);
				return;
			}
		}
		
	}
	
	@Override
	public void onBackPressed() {
		navStack.pop();
		
		if ( navStack.isEmpty() ) {
			new LoadOPDSTask().execute(BASE_URL);
		} else {
			new LoadOPDSTask().execute(navStack.peek());
		}
	}
	
	
	private class LoadOPDSTask extends AsyncTask<String, Integer, Feed> {
		
		@Override
		protected Feed doInBackground(String... params) {
			
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet( params[0] );
			
			try {
				HttpResponse response = client.execute(get);
				Feed feed = Nucular.readFromStream( response.getEntity().getContent() );
				return feed;
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}				
			
		}
		
		@Override
		protected void onPostExecute(Feed result) {
			adapter.setFeed(result);
		}
		
	}
	
}
