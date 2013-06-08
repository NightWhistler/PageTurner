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
package net.nightwhistler.pageturner.catalog;

import android.content.Context;
import com.google.inject.Inject;
import net.nightwhistler.nucular.atom.AtomConstants;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.Nucular;
import net.nightwhistler.nucular.parser.opensearch.SearchDescription;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;

public class LoadOPDSTask extends QueueableAsyncTask<String, Object, Feed> {
	
	private static final Logger LOG = LoggerFactory.getLogger("LoadOPDSTask");

	private Configuration config;
	private Context context;

	private HttpClient httpClient;

	private LoadFeedCallback callBack;
	
	private String errorMessage;
	private boolean asDetailsFeed;
    private boolean asSearchFeed;

    private LoadFeedCallback.ResultType resultType;

	@Inject
	LoadOPDSTask(Context context, Configuration config, HttpClient httpClient) {
		this.context = context;
		this.config = config;
		this.httpClient = httpClient;
	}

	@Override
	protected void onPreExecute() {
        callBack.onLoadingStart();
	}

    @Override
    public void requestCancellation() {
        LOG.debug("Got cancel request");
        super.requestCancellation();
    }

    @Override
	protected Feed doInBackground(String... params) {

		String baseUrl = params[0];

		if (baseUrl == null || baseUrl.trim().length() == 0) {
			return null;
		}

        boolean isBaseFeed = baseUrl.equals(config.getBaseOPDSFeed());

		baseUrl = baseUrl.trim();

		try {			

            HttpGet currentRequest = new HttpGet(baseUrl);
            currentRequest.setHeader("User-Agent", config.getUserAgent() );
            currentRequest.setHeader("Accept-Language", config.getLocale().getLanguage());
			HttpResponse response = httpClient.execute(currentRequest);

            LOG.debug("Starting download of " + baseUrl );

			if ( response.getStatusLine().getStatusCode() != 200 ) {
				this.errorMessage = "HTTP " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase();
				return null;
			}

			InputStream stream = response.getEntity().getContent();
			Feed feed = Nucular.readAtomFeedFromStream(stream);
            feed.setURL(baseUrl);
			feed.setDetailFeed(asDetailsFeed);
            feed.setSearchFeed(asSearchFeed);

            for ( Entry entry: feed.getEntries() ) {
                entry.setBaseURL( baseUrl );
            }

            if (isBaseFeed) {
                addCustomSitesEntry(feed);
            }

            if ( isCancelled() ) {
                return null;
            }

			Link searchLink = feed.findByRel(AtomConstants.REL_SEARCH);

			if (searchLink != null) {
				URL mBaseUrl = new URL(baseUrl);
				URL mSearchUrl = new URL(mBaseUrl, searchLink.getHref());
				searchLink.setHref(mSearchUrl.toString());

				if (AtomConstants.TYPE_OPENSEARCH.equals(searchLink.getType())) {
					String searchURL = searchLink.getHref();

                    currentRequest = new HttpGet(searchURL);
					InputStream searchStream = httpClient.execute(currentRequest).getEntity().getContent();
					
					SearchDescription desc = Nucular
							.readOpenSearchFromStream(searchStream);

					if (desc.getSearchLink() != null) {
						searchLink.setType(AtomConstants.TYPE_ATOM);
						searchLink.setHref(desc.getSearchLink().getHref());
					}

				}
			}

			return feed;
		} catch (Exception e) {
			this.errorMessage = e.getLocalizedMessage();
			LOG.error("Download failed for url: " + baseUrl, e);
			return null;
		}

	}

    void setResultType(LoadFeedCallback.ResultType type) {
        this.resultType = type;
    }

	public LoadOPDSTask setCallBack(LoadFeedCallback callBack) {
		this.callBack = callBack;
		return this;
	}
	
	public void setAsDetailsFeed(boolean asDetailsFeed) {
		this.asDetailsFeed = asDetailsFeed;
	}

    public void setAsSearchFeed(boolean asSearchFeed) {
        this.asSearchFeed = asSearchFeed;
    }

    @Override
    protected void doOnPostExecute(Feed result) {
		if (result == null) {
			callBack.errorLoadingFeed(errorMessage);
		}  else if ( result.getSize() == 0 ) {
            callBack.emptyFeedLoaded(result);
        } else {
            callBack.setNewFeed(result, resultType);
        }
	}

	private void addCustomSitesEntry(Feed feed) {

        Link iconLink = feed.findByRel("pageturner:custom_sites");

		Entry entry = new Entry();
		entry.setTitle(context.getString(R.string.custom_site));
		entry.setSummary(context.getString(R.string.custom_site_desc));
		entry.setId(Catalog.CUSTOM_SITES_ID);
        entry.setBaseURL( feed.getURL() );

        if ( iconLink != null ) {
            Link thumbnailLink = new Link(iconLink.getHref(), iconLink.getType(), AtomConstants.REL_IMAGE, null);
            entry.addLink(thumbnailLink);
        }

		feed.addEntry(entry);
	}

}
