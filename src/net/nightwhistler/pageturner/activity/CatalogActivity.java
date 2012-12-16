/*
 * Copyright (C) 2012 Alex Kuiper
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
package net.nightwhistler.pageturner.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.annotation.Nullable;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.nucular.atom.AtomConstants;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.Nucular;
import net.nightwhistler.nucular.parser.opensearch.SearchDescription;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.PlatformUtil;
import net.nightwhistler.pageturner.Configuration.LibrarySelection;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.catalog.CatalogListAdapter;
import net.nightwhistler.pageturner.library.LibraryService;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import com.google.inject.Inject;

public class CatalogActivity extends RoboSherlockActivity implements
		OnItemClickListener {

	private String baseURL;
	private String user;
	private String password;

	private CatalogListAdapter adapter;

	private ProgressDialog waitDialog;
	private ProgressDialog downloadDialog;

	private static final Logger LOG = LoggerFactory
			.getLogger(CatalogActivity.class);

	private Stack<String> navStack = new Stack<String>();

	@InjectView(R.id.catalogList)
	@Nullable
	private ListView catalogList;

	@Inject
	private Configuration config;
	
	@Inject
	private LibraryService libraryService;
	
	private LinkListener linkListener;

	private static interface LinkListener {
		void linkUpdated();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme( RoboGuice.getInjector(this).getInstance(Configuration.class).getTheme() );
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.catalog);

		this.adapter = new DownloadingCatalogAdapter();
		catalogList.setAdapter(adapter);
		catalogList.setOnItemClickListener(this);

		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);

		this.downloadDialog = new ProgressDialog(this);

		this.downloadDialog.setIndeterminate(false);
		this.downloadDialog.setMax(100);
		this.downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		this.downloadDialog.setCancelable(true);

		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		this.baseURL = intent.getStringExtra("url");
		this.user = intent.getStringExtra("user");
		this.password = intent.getStringExtra("password");

		Uri uri = intent.getData();

		if (uri != null && uri.toString().startsWith("epub://")) {
			String downloadUrl = uri.toString().replace("epub://", "http://");
			new DownloadFileTask(false).execute(downloadUrl);
		} else {
			new LoadOPDSTask().execute(baseURL);
		}
	}

	public void onSearchClick() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.search_books);
		alert.setMessage(R.string.enter_query);

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton(android.R.string.search_go,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						CharSequence value = input.getText();
						if (value != null && value.length() > 0) {
							String searchString = URLEncoder.encode(value
									.toString());
							String linkUrl = adapter.getFeed().getSearchLink()
									.getHref();

							linkUrl = linkUrl.replace("{searchTerms}",
									searchString);

							loadURL(linkUrl);
						}
					}
				});

		alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();
	}

	@Override
	public void onItemClick(AdapterView<?> list, View arg1, int position,
			long arg3) {

		Entry entry = adapter.getItem(position);

		if (entry.getAtomLink() != null) {
			String href = entry.getAtomLink().getHref();
			loadURL(entry, href);
		} else {
			loadFakeFeed(entry);
		}
	}

	private boolean isLeafEntry(Feed feed) {
		return feed.getEntries().size() == 1;
	}

	private void loadFakeFeed(Entry entry) {
		String base = baseURL;

		if (!navStack.isEmpty()) {
			base = navStack.peek();
		}

		navStack.push(base);
		new LoadFakeFeedTask(entry).execute(base);
	}

	private void loadURL(String url) {
		loadURL(null, url);
	}

	private void loadURL(Entry entry, String url) {

		String base = baseURL;

		if (!navStack.isEmpty()) {
			base = navStack.peek();
		}

		try {
			String target = new URL(new URL(base), url).toString();
			LOG.info("Loading " + target);

			navStack.push(target);
			new LoadOPDSTask(entry).execute(target);
		} catch (MalformedURLException u) {
			LOG.error("Malformed URL:", u);
		}
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getSupportActionBar().setHomeButtonEnabled(true);
		
		MenuItem item = menu.add(getString(R.string.open_library));
		item.setIcon(R.drawable.book_star);				

		menu.add("Left").setIcon(R.drawable.arrow_left)
				.setVisible(false)
				.setEnabled(false)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				
		
		menu.add("Right").setIcon(R.drawable.arrow_right)
			.setVisible(false)
			.setEnabled(false)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		menu.add("Search").setIcon(R.drawable.zoom)
			.setVisible(false)
			.setEnabled(false)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


		return true;

	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		Feed feed = adapter.getFeed();
		
		if ( feed == null ) {
			return true;
		}
		
		boolean nextEnabled = feed.getNextLink() != null;
		boolean prevEnabled = feed.getPreviousLink() != null || navStack.size() > 0;
		boolean searchEnabled = feed.getSearchLink() != null;
		
		for ( int i=0; i < menu.size(); i++ ) {
			MenuItem item = menu.getItem(i);
			
			boolean enabled = false;
			
			if ( item.getTitle().equals("Left") ) {
				enabled = prevEnabled;
			} else if ( item.getTitle().equals("Right")) {
				enabled = nextEnabled;
			} else if ( item.getTitle().equals("Search") ) {
				enabled = searchEnabled;
			}
			
			item.setEnabled(enabled);
			item.setVisible(enabled);			
		}
		
		return true;
		
	}
	

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		if (item.getItemId() == android.R.id.home ) {
			navStack.clear();
			new LoadOPDSTask().execute(baseURL);
			return true;
		} else if (item.getTitle().equals("Right")) {
			loadURL(adapter.getFeed().getNextLink().getHref());
		} else if ( item.getTitle().equals("Left" ) ) {
			if (navStack.size() > 0) {
				onBackPressed();
			} else if (adapter.getFeed().getPreviousLink() != null) {
				loadURL(adapter.getFeed().getPreviousLink().getHref());
			}
		} else if ( item.getTitle().equals("Search")) {
			onSearchClick();
		} else {		
			finish();
		}
		return true;
	}

	@Override
	protected void onStop() {
		downloadDialog.dismiss();
		waitDialog.dismiss();

		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (navStack.isEmpty()) {
			finish();
		} else {
			navStack.pop();
		}

		if (navStack.isEmpty()) {
			new LoadOPDSTask().execute(baseURL);
		} else {
			new LoadOPDSTask().execute(navStack.peek());
		}
	}

	private class DownloadingCatalogAdapter extends CatalogListAdapter {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView;
			final Entry entry = getItem(position);

			LayoutInflater inflater = PlatformUtil.getLayoutInflater(CatalogActivity.this);
			Link imgLink = entry.getThumbnailLink();

			rowView = inflater.inflate(R.layout.catalog_item, parent, false);			 			

			loadBookDetails( rowView, entry, imgLink );
			return rowView;
		}
	}
	
	

	// this is our download file asynctask
	private class DownloadFileTask extends AsyncTask<String, Integer, String> {

		File destFile;
		
		private boolean openAfterDownload;
		private Exception failure;
		
		public DownloadFileTask(boolean openAfterDownload) {
			this.openAfterDownload = openAfterDownload;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();			
			downloadDialog.setMessage(getString(R.string.downloading));
			downloadDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {

			try {

				String url = params[0];
				LOG.debug("Downloading: " + url);

				String fileName = url.substring(url.lastIndexOf('/') + 1);

				HttpParams httpParams = new BasicHttpParams();
				DefaultHttpClient client = new DefaultHttpClient(httpParams);
				client.getCredentialsProvider().setCredentials(
						new AuthScope(null, -1),
						new UsernamePasswordCredentials(user, password));
				HttpGet get = new HttpGet(url);

				HttpResponse response = client.execute(get);

				if (response.getStatusLine().getStatusCode() == 200) {

					File destFolder = new File(config.getDownloadsFolder());
					if (!destFolder.exists()) {
						destFolder.mkdirs();
					}
					
					/**
					 * Make sure we always store downloaded files as .epub, 
					 * so they show up in scans later on.
					 */
					if ( ! fileName.endsWith(".epub") ) {
						fileName = fileName + ".epub";
					}

					destFile = new File(destFolder, URLDecoder.decode(fileName));

					if (destFile.exists()) {
						destFile.delete();
					}

					// lenghtOfFile is used for calculating download progress
					long lenghtOfFile = response.getEntity().getContentLength();

					// this is where the file will be seen after the download
					FileOutputStream f = new FileOutputStream(destFile);
					
					try {
						// file input is from the url
						InputStream in = response.getEntity().getContent();

						// here's the download code
						byte[] buffer = new byte[1024];
						int len1 = 0;
						long total = 0;

						while ((len1 = in.read(buffer)) > 0) {

							// Make sure the user can cancel the download.
							if (isCancelled()) {
								return null;
							}

							total += len1;
							publishProgress((int) ((total * 100) / lenghtOfFile));
							f.write(buffer, 0, len1);
						}
					} finally {
						f.close();
					}
					
					Book book = new EpubReader().readEpub( new FileInputStream(destFile) );					
					libraryService.storeBook(destFile.getAbsolutePath(), book, false, config.isCopyToLibrayEnabled() );
					
				} else {
					this.failure = new RuntimeException(response
							.getStatusLine().getReasonPhrase());
					LOG.error("Download failed: "
							+ response.getStatusLine().getReasonPhrase());
				}

			} catch (Exception e) {
				LOG.error("Download failed.", e);
				this.failure = e;
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			downloadDialog.setProgress(values[0]);
		}

		@Override
		protected void onPostExecute(String unused) {
			downloadDialog.hide();

			if (!isCancelled() && failure == null) {
				
				Intent intent;
				
				if ( openAfterDownload ) {				
					intent = new Intent(getBaseContext(),
						ReadingActivity.class);
					intent.setData(Uri.parse(destFile.getAbsolutePath()));
					
				} else {
					intent = new Intent(getBaseContext(), LibraryActivity.class);					
					config.setLastLibraryQuery(LibrarySelection.LAST_ADDED);
				}
				
				startActivity(intent);
				finish();				
				
			} else if (failure != null) {
				Toast.makeText(CatalogActivity.this, R.string.book_failed,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void loadBookDetails(View layout, Entry entry, Link imageLink ) {
		
		HtmlSpanner spanner = new HtmlSpanner();
		
		TextView title = (TextView) layout.findViewById(R.id.itemTitle);
		TextView desc = (TextView) layout
				.findViewById(R.id.itemDescription);

		ImageView icon = (ImageView) layout.findViewById(R.id.itemIcon);
		loadImageLink(icon, imageLink);

		title.setText(entry.getTitle());

		if (entry.getContent() != null) {
			desc.setText(spanner.fromHtml(entry.getContent().getText()));
		} else if (entry.getSummary() != null) {
			desc.setText(spanner.fromHtml(entry.getSummary()));
		} else {
			desc.setText("");
		}

	}
	
	private void loadImageLink(ImageView icon, Link imageLink ) {

		if (imageLink != null && imageLink.getBinData() != null) {
			byte[] data = imageLink.getBinData();
			icon.setImageBitmap(BitmapFactory.decodeByteArray(data, 0,
					data.length));
		} else {
			icon.setImageDrawable(getResources().getDrawable(
					R.drawable.unknown_cover));
		}
	}
	
	private void loadImageLink(Map<String, byte[]> cache, Link imageLink,
			String baseUrl) throws IOException {

		HttpParams httpParams = new BasicHttpParams();
		DefaultHttpClient client = new DefaultHttpClient(httpParams);
		client.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
				new UsernamePasswordCredentials(user, password));

		if (imageLink != null) {
			String href = imageLink.getHref();

			if (cache.containsKey(href)) {
				imageLink.setBinData(cache.get(href));
			} else {

				String target = new URL(new URL(baseUrl), href).toString();

				LOG.info("Downloading image: " + target);

				HttpResponse resp = client.execute(new HttpGet(target));

				imageLink.setBinData(EntityUtils.toByteArray(resp.getEntity()));

				cache.put(href, imageLink.getBinData());
			}
		}
	}
	
	private void notifyLinkUpdated() {
		adapter.notifyDataSetChanged();
		
		if ( linkListener != null ) {
			linkListener.linkUpdated();
			linkListener = null;
		}		
	}
	
	private void showItemPopup(final Feed feed) {
		
		//If we're here, the feed always has just 1 entry
		final Entry entry = feed.getEntries().get(0);
		
		//Also, we don't want this entry on the nav-stack
		navStack.pop();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(feed.getTitle());
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(this);
		View layout = inflater.inflate(R.layout.catalog_download, null);
		builder.setView( layout );		

		TextView authorTextView = (TextView) layout
				.findViewById(R.id.itemAuthor);

		builder.setNegativeButton(android.R.string.cancel, null);
		
		if ( entry.getEpubLink() != null ) {
			
			String base = baseURL;

			if (!navStack.isEmpty()) {
				base = navStack.peek();
			}
			
			try {
				final URL url = new URL(new URL(base), entry.getEpubLink().getHref());
				
				builder.setPositiveButton(R.string.read, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new DownloadFileTask(true).execute(url.toExternalForm());						
					}
				});
				
				builder.setNeutralButton(R.string.add_to_library, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new DownloadFileTask(false).execute(url.toExternalForm());						
					}
				});				
				
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
			
			
		}
		

		if (entry.getBuyLink() != null) {
			builder.setNeutralButton(R.string.buy_now, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String url = entry.getBuyLink().getHref();
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				}
			});
		}

		if (entry.getAuthor() != null) {
			String authorText = String.format(
					getString(R.string.book_by), entry.getAuthor()
							.getName());
			authorTextView.setText(authorText);
		} else {
			authorTextView.setText("");
		}
		
		final Link imgLink = entry.getImageLink();
		
		loadBookDetails(layout, entry, imgLink);
		final ImageView icon = (ImageView) layout.findViewById(R.id.itemIcon);
		
		linkListener = new LinkListener() {
			
			@Override
			public void linkUpdated() {
				loadImageLink(icon, imgLink);				
			}
		};
		
		builder.show();
	}

	private void setNewFeed(Feed result) {		

		if (result != null) {
			
			if ( isLeafEntry(result) ) {
				showItemPopup(result);
			} else {

				supportInvalidateOptionsMenu();
				getSupportActionBar().setTitle(result.getTitle());
				adapter.setFeed(result);
			}
			
			waitDialog.hide();
		} else {
			waitDialog.hide();
			Toast.makeText(CatalogActivity.this, R.string.feed_failed,
					Toast.LENGTH_LONG).show();
		}
	}

	private class LoadFakeFeedTask extends AsyncTask<String, Integer, Feed> {

		private Entry singleEntry;

		public LoadFakeFeedTask(Entry entry) {
			this.singleEntry = entry;
		}

		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(getString(R.string.loading_wait));
			waitDialog.show();
		}

		@Override
		protected Feed doInBackground(String... params) {
			Feed fakeFeed = new Feed();
			fakeFeed.addEntry(singleEntry);
			fakeFeed.setTitle(singleEntry.getTitle());

			try {
				loadImageLink(new HashMap<String, byte[]>(),
						singleEntry.getImageLink(), params[0]);
			} catch (IOException io) {
				LOG.error("Could not load image: ", io);
			}

			return fakeFeed;
		}

		@Override
		protected void onPostExecute(Feed result) {
			setNewFeed(result);
		}
	}

	private class LoadOPDSTask extends AsyncTask<String, Object, Feed>
			implements OnCancelListener {

		private Entry previousEntry;

		public LoadOPDSTask() {
			// leave previousEntry null
		}

		public LoadOPDSTask(Entry entry) {
			this.previousEntry = entry;
		}

		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(getString(R.string.loading_wait));
			waitDialog.setOnCancelListener(this);
			waitDialog.show();
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			this.cancel(true);
		}

		@Override
		protected Feed doInBackground(String... params) {

			String baseUrl = params[0];

			if (baseUrl == null || baseUrl.trim().length() == 0) {
				return null;
			}

			baseUrl = baseUrl.trim();

			try {
				HttpParams httpParams = new BasicHttpParams();
				DefaultHttpClient client = new DefaultHttpClient(httpParams);
				client.getCredentialsProvider().setCredentials(
						new AuthScope(null, -1),
						new UsernamePasswordCredentials(user, password));
				HttpGet get = new HttpGet(baseUrl);
				HttpResponse response = client.execute(get);
				Feed feed = Nucular.readAtomFeedFromStream(response.getEntity()
						.getContent());
				List<Link> remoteImages = new ArrayList<Link>();

				for (Entry entry : feed.getEntries()) {

					if (isCancelled()) {
						return feed;
					}

					Link imageLink;

					if (isLeafEntry(feed)) {
						imageLink = entry.getImageLink();
					} else {
						imageLink = entry.getThumbnailLink();
					}

					if (imageLink != null) {
						String href = imageLink.getHref();

						// If the image is contained in the feed, load it
						// directly
						if (href.startsWith("data:image/png;base64")) {
							String dataString = href.substring(href
									.indexOf(',') + 1);
							try {
								imageLink.setBinData(Base64.decode(dataString,
										Base64.DEFAULT));
							} catch (NoClassDefFoundError ncd) {
								// Slight hack for Android 2.1
								imageLink.setBinData(null);
							}
						} else {
							remoteImages.add(imageLink);
						}
					}
				}

				Link searchLink = feed.findByRel(AtomConstants.REL_SEARCH);

				if (searchLink != null) {
					URL mBaseUrl = new URL(baseUrl);
					URL mSearchUrl = new URL(mBaseUrl, searchLink.getHref());
					searchLink.setHref(mSearchUrl.toString());

					if (AtomConstants.TYPE_OPENSEARCH.equals(searchLink
							.getType())) {
						HttpGet searchGet = new HttpGet(searchLink.getHref());

						HttpResponse searchResponse = client.execute(searchGet);
						SearchDescription desc = Nucular
								.readOpenSearchFromStream(searchResponse
										.getEntity().getContent());

						if (desc.getSearchLink() != null) {
							searchLink.setType(AtomConstants.TYPE_ATOM);
							searchLink.setHref(desc.getSearchLink().getHref());
						}

					}
				}

				publishProgress(feed);

				Map<String, byte[]> cache = new HashMap<String, byte[]>();
				for (Link link : remoteImages) {
					loadImageLink(cache, link, baseUrl);
					publishProgress(link);
				}

				return feed;
			} catch (Exception e) {
				LOG.error("Download failed.", e);
				return null;
			}

		}

		@Override
		protected void onPostExecute(Feed result) {
			if (result == null) {
				setNewFeed(null);
			}
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			if (values == null || values.length == 0) {
				return;
			}

			Object val = values[0];

			if (val instanceof Feed) {
				Feed result = (Feed) val;

				/**
				 * This is a bit hacky: some feeds have the download link in the
				 * list, and clicking an item will take you to another list.
				 * 
				 * Since we always want to send the user to a single-item page
				 * before downloading, we have to fake it some times.
				 */

				if (previousEntry != null
						&& previousEntry.getEpubLink() != null) {
					if (result == null || result.getSize() != 1
							|| result.getEntries().get(0).getEpubLink() == null) {
						loadFakeFeed(previousEntry);
						return;
					}
				}

				setNewFeed(result);
			} else if (val instanceof Link) {
				notifyLinkUpdated();
			}
		}

	}

}
