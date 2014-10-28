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
import jedi.option.Option;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static jedi.functional.FunctionalPrimitives.isEmpty;
import static jedi.option.Options.none;
import static jedi.option.Options.some;

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
    public void doOnPreExecute() {
        callBack.onLoadingStart();
	}

    @Override
    public void requestCancellation() {
        LOG.debug("Got cancel request");
        super.requestCancellation();
    }

    @Override
	public Option<Feed> doInBackground(String... params) {

		String baseUrl = params[0];

		if (baseUrl == null || baseUrl.trim().length() == 0) {
			return none();
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
				return none();
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
                return none();
            }

			Option<Link> searchLinkOption = feed.findByRel(AtomConstants.REL_SEARCH);

            if ( ! isEmpty(searchLinkOption) ) {

                Link searchLink = searchLinkOption.unsafeGet();

            	URL mBaseUrl = new URL(baseUrl);
				URL mSearchUrl = new URL(mBaseUrl, searchLink.getHref());
				searchLink.setHref(mSearchUrl.toString());

                LOG.debug("Got searchLink of type " + searchLink.getType() +
                        " with href=" + searchLink.getHref() );

                /*
                Some sites report the search as OpenSearch, but still have the
                searchTerms in the URL. If the URL already contains searchTerms,
                we ignore the reported type and treat it as Atom
                 */
                if ( searchLink.getHref().contains(AtomConstants.SEARCH_TERMS) ) {
                    searchLink.setType(AtomConstants.TYPE_ATOM);
                }

				if (AtomConstants.TYPE_OPENSEARCH.equals( searchLink.getType() )) {

					String searchURL = searchLink.getHref();

                    LOG.debug("Attempting to download OpenSearch description from " + searchURL );

                    try {
                        currentRequest = new HttpGet(searchURL);
					    InputStream searchStream = httpClient.execute(currentRequest).getEntity().getContent();
					
					    SearchDescription desc = Nucular
							.readOpenSearchFromStream(searchStream);

					    desc.getSearchLink().forEach( l ->
                            searchLink.setHref(l.getHref())
                        );

                        searchLink.setType(AtomConstants.TYPE_ATOM);

                    } catch ( Exception searchIO ) {
                        LOG.error("Could not get search info", searchIO );
                    }
				}

                LOG.debug("Using searchURL " + searchLink.getHref() );
			}

			return some(feed);
		} catch (Exception e) {
			this.errorMessage = e.getLocalizedMessage();
			LOG.error("Download failed for url: " + baseUrl, e);
			return none();
		}

	}

    public void setResultType(LoadFeedCallback.ResultType type) {
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
    public void doOnPostExecute(Option<Feed> result) {

        result.match( f -> {
            if ( f.getSize() == 0 ) {
                callBack.emptyFeedLoaded(f);
            } else {
                callBack.setNewFeed(f, resultType);
            }
        }, () -> callBack.errorLoadingFeed(errorMessage) );
	}

	private void addCustomSitesEntry(Feed feed) {

        Option<Link> iconLink = feed.findByRel("pageturner:custom_sites");

		Entry entry = new Entry();
		entry.setTitle(context.getString(R.string.custom_site));
		entry.setSummary(context.getString(R.string.custom_site_desc));
		entry.setId(Catalog.CUSTOM_SITES_ID);
        entry.setBaseURL( feed.getURL() );

        iconLink.forEach( l -> {
            Link thumbnailLink = new Link(l.getHref(), l.getType(), AtomConstants.REL_IMAGE, null);
            entry.addLink(thumbnailLink);
        });

		feed.addEntry(entry);
	}

}
