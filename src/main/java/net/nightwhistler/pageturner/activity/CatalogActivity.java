package net.nightwhistler.pageturner.activity;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.Nucular;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.catalog.CatalogListAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.internal.Nullable;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class CatalogActivity extends RoboActivity implements OnItemClickListener {

	//private static final String BASE_URL = "http://m.gutenberg.org/ebooks/?format=opds";
	//private static final String BASE_URL = "http://www.feedbooks.com/catalog.atom";
	
	//private static final String BASE_URL = "http://m.gutenberg.org/ebooks/730.opds";
	
	//private static final String BASE_URL = "http://192.168.0.6/library/catalog/catalog.xml";
	
	private String baseURL;
	
	private CatalogListAdapter adapter;
	
	private ProgressDialog waitDialog;
	private ProgressDialog downloadDialog;
	
	private static final Logger LOG = LoggerFactory.getLogger(CatalogActivity.class); 
	
	private Stack<String> navStack = new Stack<String>();
	
	@InjectView(R.id.homeButton)
	@Nullable
	private ImageButton homeButton;
	
	@InjectView(R.id.prevButton)
	@Nullable
	private ImageButton prevButton;
	
	@InjectView(R.id.nextButton)
	@Nullable
	private ImageButton nextButton;
	
	@InjectView(R.id.searchButton)
	@Nullable
	private ImageButton searchButton;
	
	@InjectView(R.id.catalogList)
	@Nullable
	private ListView catalogList;
	
	private String catalogType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.catalog);
		
		this.adapter = new DownloadingCatalogAdapter();
		catalogList.setAdapter(adapter);
		catalogList.setOnItemClickListener(this);
		
		this.waitDialog = new ProgressDialog(this);
        this.waitDialog.setOwnerActivity(this);  
        
        this.downloadDialog = new ProgressDialog(this);
        //this.downloadDialog.setMessage("Downloading file...");
        this.downloadDialog.setIndeterminate(false);
        this.downloadDialog.setMax(100);
        this.downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.downloadDialog.setCancelable(true);
                
		super.onCreate(savedInstanceState);
		
		this.baseURL = getIntent().getStringExtra("url");
		
		if ( baseURL.contains("gutenberg") ) {
			catalogType = "gutenberg";
		} else {
			catalogType = "feedbooks";
		}
		
		new LoadOPDSTask().execute(baseURL);
	}
	
	@Override
	public void onItemClick(AdapterView<?> list, View arg1, int position, long arg3) {		
		
		Entry entry = adapter.getItem(position);
		if ( entry.getAtomLink() != null ) {
			String href = entry.getAtomLink().getHref();
			loadURL(href);		
		}		
	}
	
	private boolean isLeafEntry(Feed feed, Entry entry ) {
		if ( catalogType.equals("gutenberg") ) {
			return entry.getEpubLink() != null;
		} else {
			return feed.getEntries().size() == 1;			
		}
	}
	
	private void loadURL( String url ) {
		
		String base = baseURL;
		
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
	
	public void onNavClick( View v ) {
		if ( v == homeButton ) {
			navStack.clear();
			new LoadOPDSTask().execute(baseURL);
			return;
		} else if ( v == nextButton ) {
			loadURL( adapter.getFeed().getNextLink().getHref() );
		} else if ( v == prevButton ) {
			if ( navStack.size() > 0 ) {
				onBackPressed(); 
			} else if ( adapter.getFeed().getPreviousLink() != null ) {
				loadURL( adapter.getFeed().getPreviousLink().getHref() );
			} 
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add( getString(R.string.open_library) );
		item.setIcon(R.drawable.book_star);
		
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				finish();
				return true;
			}
		});
		
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
		if ( navStack.isEmpty() ) {
			finish();
		} else {
			navStack.pop();
		}
		
		if ( navStack.isEmpty() ) {
			new LoadOPDSTask().execute(baseURL);
		} else {
			new LoadOPDSTask().execute(navStack.peek());
		}
	}
	
	private class DownloadingCatalogAdapter extends CatalogListAdapter {
		
		HtmlSpanner spanner = new HtmlSpanner();
				
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView;		
			final Entry entry = getItem(position);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			Link imgLink;

			if ( isLeafEntry(adapter.getFeed(), entry) ) {			
				rowView = inflater.inflate(R.layout.catalog_download, parent, false);	
				
				Button button = (Button) rowView.findViewById( R.id.readButton );
				TextView authorTextView = (TextView) rowView.findViewById(R.id.itemAuthor);
				
				button.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						try {
							URL url = new URL(new URL(baseURL), entry.getEpubLink().getHref() );
							new DownloadFileTask().execute(url.toExternalForm());
						} catch (Exception e) {
							throw new RuntimeException(e);
						}					
					}
				});
				
				if ( entry.getAuthor() != null ) {				
					String authorText = String.format( getString(R.string.book_by),
						 entry.getAuthor().getName() );
					authorTextView.setText( authorText );
				} else {
					authorTextView.setText("");
				}
				
				if ( entry.getEpubLink() == null ) {
					button.setVisibility(View.INVISIBLE);
				} else {
					button.setVisibility(View.VISIBLE);
				}
				
				imgLink = entry.getImageLink();
				
			} else {
				rowView = inflater.inflate(R.layout.catalog_item, parent, false);
				imgLink = entry.getThumbnailLink();
			}
					
			
			TextView title = (TextView) rowView.findViewById(R.id.itemTitle);
			TextView desc = (TextView) rowView.findViewById(R.id.itemDescription );
			
			ImageView icon = (ImageView) rowView.findViewById(R.id.itemIcon);

			if ( imgLink != null && imgLink.getBinData() != null ) {			
				byte[] data = imgLink.getBinData();
				icon.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
			} else {
				icon.setImageDrawable(getResources().getDrawable(R.drawable.book));
			}

			title.setText(entry.getTitle());

			if ( entry.getContent() != null ) {
				desc.setText( spanner.fromHtml( entry.getContent().getText() ));
			} else if ( entry.getSummary() != null ){
				desc.setText( spanner.fromHtml( entry.getSummary()));
			} else {
				desc.setText("");
			}

			return rowView;
		}	
	}
	
	//this is our download file asynctask
    private class DownloadFileTask extends AsyncTask<String, String, String> {
    	
    	File destFile;
       
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
            	
            	String fileName = url.substring( url.lastIndexOf('/') + 1 );
            	
            	DefaultHttpClient client = new DefaultHttpClient();
    			HttpGet get = new HttpGet( url );
    				
   				HttpResponse response = client.execute(get);                
                
   				File destFolder = new File("/sdcard/PageTurner/Downloads/");
   				if ( ! destFolder.exists() ) {
   					destFolder.mkdirs();
   				}
   				
                destFile = new File(destFolder, catalogType + "_" + fileName );
               
                if ( destFile.exists() ) {
                	destFile.delete();
                }
               
                //lenghtOfFile is used for calculating download progress
                long lenghtOfFile = response.getEntity().getContentLength();
               
                //this is where the file will be seen after the download
                FileOutputStream f = new FileOutputStream(destFile);
                //file input is from the url
                InputStream in = response.getEntity().getContent();

                //here's the download code
                byte[] buffer = new byte[1024];
                int len1 = 0;
                long total = 0;
               
                while ((len1 = in.read(buffer)) > 0) {
                	
                	//Make sure the user can cancel the download.
                	if ( isCancelled() ) {
                		return null;
                	}
                	
                    total += len1; 
                    publishProgress("" + (int)((total*100)/lenghtOfFile));
                    f.write(buffer, 0, len1);
                }
                f.close();
               
            } catch (Exception e) {
            	LOG.error("Download failed.", e);				
            }
           
            return null;
        }
       
        protected void onProgressUpdate(String... progress) {
             downloadDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String unused) {
           downloadDialog.hide();
           
           if ( ! isCancelled() ) {
        	   Intent intent = new Intent(getBaseContext(), ReadingActivity.class);   		
        	   intent.setData( Uri.parse(destFile.getAbsolutePath()));   				   				
        	   startActivity(intent);
        	   finish();
           }
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
			
			String baseUrl = params[0];
			
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet( baseUrl );
			
			try {
				
				HttpResponse response = client.execute(get);
				Feed feed = Nucular.readFromStream( response.getEntity().getContent() );
				
				for ( Entry entry: feed.getEntries() ) {
					Link imageLink;
					
					if( isLeafEntry(feed, entry) ) {
						imageLink = entry.getImageLink();
					} else {
						imageLink = entry.getThumbnailLink();
					}
					
					if ( imageLink != null ) {
						String href = imageLink.getHref();					

						if ( href.startsWith("data:image/png;base64")) {
							String dataString = href.substring( href.indexOf(',') + 1 );
							imageLink.setBinData( Base64.decode(dataString, Base64.DEFAULT ));
						} else {
							String target = new URL(new URL(baseUrl), href).toString();	
							
							LOG.info("Downloading image: " + target );
							
							HttpResponse resp = client.execute(new HttpGet(target));

							imageLink.setBinData( EntityUtils.toByteArray( resp.getEntity() ) );
						}
					}
				}
				
				
				return feed;
			} catch (Exception e) {
				LOG.error("Download failed.", e);
				return null;
			}				
			
		}		
		
		@Override
		protected void onPostExecute(Feed result) {
			if ( result != null ) {
				nextButton.setEnabled( result.getNextLink() != null );
				prevButton.setEnabled( result.getPreviousLink() != null || navStack.size() > 0 );
				
				setTitle( result.getTitle() );
				searchButton.setEnabled(false);
				
				adapter.setFeed(result);
			} 
			//else show popup
			waitDialog.hide();
		}
		
	}
	
}
