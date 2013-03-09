package net.nightwhistler.pageturner.catalog;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.nightwhistler.nucular.atom.AtomConstants;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.Nucular;
import net.nightwhistler.nucular.parser.opensearch.SearchDescription;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;

import static net.nightwhistler.pageturner.catalog.Catalog.getImageLink;

public class LoadOPDSTask extends AsyncTask<String, Object, Feed> implements
		OnCancelListener {
	
	private static final Logger LOG = LoggerFactory.getLogger("LoadOPDSTask");

	@Inject
	private Configuration config;

	@Inject
	private Context context;

	@Inject
	private HttpClient httpClient;
	
	private Entry previousEntry;
	private boolean isBaseFeed;
	private Dialog waitDialog;
	private LoadFeedCallback callBack;
		
	LoadOPDSTask() {			
	}

	@Override
	protected void onPreExecute() {
		waitDialog.setTitle(context.getString(R.string.loading_wait));
		waitDialog.setOnCancelListener(this);
		waitDialog.show();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		this.cancel(true);
	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	protected Feed doInBackground(String... params) {

		String baseUrl = params[0];

		this.isBaseFeed = baseUrl.equals(config.getBaseOPDSFeed());

		if (baseUrl == null || baseUrl.trim().length() == 0) {
			return null;
		}

		baseUrl = baseUrl.trim();

		try {
			
			Feed feed = Nucular.readAtomFeedFromStream(httpClient.getURL(baseUrl));
			
			List<Link> remoteImages = new ArrayList<Link>();

			for (final Entry entry : feed.getEntries()) {

				if (isCancelled()) {
					return feed;
				}

				Link imageLink = getImageLink(feed, entry);

				if (imageLink != null) {
					String href = imageLink.getHref();

					// If the image is contained in the feed, load it
					// directly
					if (href.startsWith("data:image/png;base64")) {
						String dataString = href
								.substring(href.indexOf(',') + 1);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
							imageLink.setBinData(Base64.decode(dataString,
									Base64.DEFAULT));
						} else {
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

				if (AtomConstants.TYPE_OPENSEARCH.equals(searchLink.getType())) {
					String searchURL = searchLink.getHref();
					SearchDescription desc = Nucular
							.readOpenSearchFromStream(httpClient.getURL(searchURL));

					if (desc.getSearchLink() != null) {
						searchLink.setType(AtomConstants.TYPE_ATOM);
						searchLink.setHref(desc.getSearchLink().getHref());
					}

				}
			}

			publishProgress(feed);

			Map<String, byte[]> cache = new HashMap<String, byte[]>();
			for (Link link : remoteImages) {
				Catalog.loadImageLink(cache, link, baseUrl);
				publishProgress(link);
			}

			return feed;
		} catch (Exception e) {
			LOG.error("Download failed for url: " + baseUrl, e);
			return null;
		}

	}
	
	public LoadOPDSTask setWaitDialog(Dialog waitDialog) {
		this.waitDialog = waitDialog;
		return this;
	}

	public LoadOPDSTask setCallBack(LoadFeedCallback callBack) {
		this.callBack = callBack;
		return this;
	}
	
	public LoadOPDSTask setPreviousEntry(Entry previousEntry) {
		this.previousEntry = previousEntry;
		return this;
	}
	
	@Override
	protected void onPostExecute(Feed result) {

		if (result == null) {
			callBack.setNewFeed(null);
		}
	}

	private void addCustomSitesEntry(Feed feed) {

		Entry entry = new Entry();
		entry.setTitle(context.getString(R.string.custom_site));
		entry.setSummary(context.getString(R.string.custom_site_desc));
		entry.setId(Catalog.CUSTOM_SITES_ID);

		feed.addEntry(entry);
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

			if (previousEntry != null && previousEntry.getEpubLink() != null) {
				if (result == null || result.getSize() != 1
						|| result.getEntries().get(0).getEpubLink() == null) {
					callBack.loadFakeFeed(previousEntry);
					return;
				}
			}

			if (isBaseFeed) {
				addCustomSitesEntry(result);
			}

			callBack.setNewFeed(result);
		} else if (val instanceof Link) {
			callBack.notifyLinkUpdated();
		}
	}

}
