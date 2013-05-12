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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import javax.annotation.Nullable;

import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.widget.SearchView;
import net.nightwhistler.nucular.atom.AtomConstants;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.CustomOPDSSite;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.activity.*;
import net.nightwhistler.pageturner.catalog.DownloadFileTask.DownloadFileCallback;
import net.nightwhistler.pageturner.library.LibraryService;

import net.nightwhistler.pageturner.scheduling.TaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;
import com.google.inject.Provider;

import static net.nightwhistler.pageturner.catalog.Catalog.getImageLink;

public class CatalogFragment extends RoboSherlockFragment implements
		LoadFeedCallback, DialogFactory.SearchCallBack, TaskQueue.TaskQueueListener {
	
    private static final String STATE_NAV_ARRAY_KEY = "nav_array";    

	private static final Logger LOG = LoggerFactory
			.getLogger("CatalogFragment");

	private Stack<String> navStack = new Stack<String>();

	@InjectView(R.id.catalogList)
	@Nullable
	private ListView catalogList;

	@Inject
	private Configuration config;
	
	@Inject
	private LibraryService libraryService;
	
	@Inject
	private Provider<LoadOPDSTask> loadOPDSTaskProvider;

    @Inject
    private Provider<ParseBinDataTask> parseBinDataTaskProvider;

    @Inject
    private Provider<LoadThumbnailTask> loadThumbnailTaskProvider;

    @Inject
    private DialogFactory dialogFactory;
	
    @Inject
	private CatalogListAdapter adapter;

    @Inject
    private Provider<DisplayMetrics> metricsProvider;

    @Inject
    private TaskQueue taskQueue;

    private MenuItem searchMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			List<String> navList = savedInstanceState.getStringArrayList(STATE_NAV_ARRAY_KEY);
			if (navList != null && navList.size() > 0) {
				navStack.addAll(navList);
			}
		}

        DisplayMetrics metrics = metricsProvider.get();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        this.taskQueue.setTaskQueueListener(this);
        int displayDensity = metrics.densityDpi;
        this.adapter.setDisplayDensity(displayDensity);
        LOG.debug("Metrics at init: " + displayDensity );

	}
	
	public boolean dispatchKeyEvent(KeyEvent event) {

		int action = event.getAction();
		int keyCode = event.getKeyCode();		

		if( keyCode == KeyEvent.KEYCODE_SEARCH
				&& action == KeyEvent.ACTION_DOWN) {
			onSearchRequested();
			return true;

		} else if ( keyCode == KeyEvent.KEYCODE_BACK
                && action == KeyEvent.ACTION_DOWN) {
            onBackPressed();
            return true;
        }

		return false;		
	}

    @Override
    public void queueEmpty() {
        onLoadingDone();
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_catalog, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setHasOptionsMenu(true);
		catalogList.setAdapter(adapter);
        catalogList.setOnScrollListener(new LoadingScrollListener());
		catalogList.setOnItemClickListener(new OnItemClickListener() {			
			@Override
			public void onItemClick(AdapterView<?> list, View arg1, int position,
					long arg3) {
				Entry entry = adapter.getItem(position);
				onEntryClicked(entry, position);
			}
		});
	}	
	
	private void loadOPDSFeed(String url) {
		loadOPDSFeed(null, url, false, ResultType.REPLACE);
	}
	
	private void loadOPDSFeed( Entry entry, String url, boolean asDetailsFeed, ResultType resultType ) {

		LoadOPDSTask task = this.loadOPDSTaskProvider.get();
		task.setCallBack(this);

        task.setResultType(resultType);
		task.setAsDetailsFeed(asDetailsFeed);

        //If we're going to load a completely new feed,
        //cancel all pending downloads.
        if ( resultType == ResultType.REPLACE ) {
            taskQueue.clear();
            taskQueue.executeTask(task, url);
        } else {
            taskQueue.jumpQueueExecuteTask(task, url);
            this.adapter.setLoading(true);
        }
		

	}	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (!navStack.empty()) {			
			loadOPDSFeed(navStack.peek());
		} else {
			loadOPDSFeed(config.getBaseOPDSFeed());
		}
	}

    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!navStack.empty()) {
			ArrayList<String> navList = new ArrayList<String>(navStack);
			outState.putStringArrayList(STATE_NAV_ARRAY_KEY, navList);
		}
	}

    public void performSearch(String searchTerm) {
    	if (searchTerm != null && searchTerm.length() > 0) {
			String searchString = URLEncoder.encode(searchTerm);
			String linkUrl = adapter.getFeed().getSearchLink()
					.getHref();

			linkUrl = linkUrl.replace("{searchTerms}",
					searchString);

			loadURL(linkUrl);
		}
    }
    
	public void onSearchRequested() {

        if ( searchMenuItem == null || ! searchMenuItem.isEnabled() ) {
            return;
        }

        if ( searchMenuItem.getActionView() != null ) {
            this.searchMenuItem.expandActionView();
            this.searchMenuItem.getActionView().requestFocus();
        } else {
            dialogFactory.showSearchDialog(R.string.search_books, R.string.enter_query, CatalogFragment.this);
        }
	}

	public void onEntryClicked( Entry entry, int position ) {		
			
		if ( entry.getId() != null && entry.getId().equals(Catalog.CUSTOM_SITES_ID) ) {			
			loadCustomSiteFeed();
		} else if ( entry.getAlternateLink() != null ) {
			String href = entry.getAlternateLink().getHref();
			loadURL(entry, href, true, ResultType.REPLACE);
		} else if ( entry.getEpubLink() != null ) {
            loadFakeFeek(entry);
		} else if ( entry.getAtomLink() != null ) {
			String href = entry.getAtomLink().getHref();
			loadURL(entry, href, false, ResultType.REPLACE);
		} 
	}

    private void loadFakeFeek( Entry entry ) {
        Feed originalFeed = entry.getFeed();

        Feed fakeFeed = new Feed();
        fakeFeed.addEntry(entry);
        fakeFeed.setTitle(entry.getTitle());
        fakeFeed.setDetailFeed(true);
        fakeFeed.setURL(originalFeed.getURL());

        ((CatalogParent) getActivity()).loadFakeFeed(fakeFeed);
    }
	
	private void loadCustomSiteFeed() {
		
		List<CustomOPDSSite> sites = config.getCustomOPDSSites();
		
		if ( sites.isEmpty() ) {
			Toast.makeText(getActivity(), R.string.no_custom_sites, Toast.LENGTH_LONG).show();
			return;
		}

        pushUrlToNavStack(Catalog.CUSTOM_SITES_ID);
		
		Feed customSites = new Feed();
        customSites.setURL(Catalog.CUSTOM_SITES_ID);
		customSites.setTitle(getString(R.string.custom_site));

		
		for ( CustomOPDSSite site: sites ) {
			Entry entry = new Entry();
			entry.setTitle(site.getName());
			entry.setSummary(site.getDescription());
			
			Link link = new Link(site.getUrl(), AtomConstants.TYPE_ATOM, AtomConstants.REL_BUY, null);
			entry.addLink(link);
			
			customSites.addEntry(entry);
		}
		
		customSites.setId(Catalog.CUSTOM_SITES_ID);
		
		setNewFeed(customSites, ResultType.REPLACE);
	}

	public void loadURL(String url) {
		loadURL(null, url, false, ResultType.REPLACE);
	}

    private void loadURL(Entry entry, String url, boolean asDetailsFeed, ResultType resultType) {

        String base = null;

        if ( entry != null && entry.getFeed() != null ) {
            base = entry.getFeed().getURL();
        }

		if (base == null && !navStack.isEmpty()) {
			base = navStack.peek();
		}

		try {
			String target = url;
			
			if ( base != null && ! base.equals(Catalog.CUSTOM_SITES_ID)) {
				target = new URL(new URL(base), url).toString();
			}

			LOG.info("Loading " + target);

            if ( resultType == ResultType.REPLACE ) {
			    pushUrlToNavStack(target);
            }

			loadOPDSFeed(entry, target, asDetailsFeed, resultType);
		} catch (MalformedURLException u) {
			LOG.error("Malformed URL:", u);
		}
	}

    private void pushUrlToNavStack(String url) {
        if ( navStack.isEmpty() || ! url.equals(navStack.peek()) ) {
            navStack.push(url);
        }
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
		getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(true);		
		inflater.inflate(R.menu.catalog_menu, menu);

        this.searchMenuItem = menu.findItem(R.id.search);
        if (searchMenuItem != null) {
            final SearchView searchView = (SearchView) searchMenuItem.getActionView();

            if (searchView != null) {

                searchView.setSubmitButtonEnabled(true);
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        performSearch(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        return  false;
                    }
                } );
            } else {
                searchMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        dialogFactory.showSearchDialog(R.string.search_books, R.string.enter_query, CatalogFragment.this);
                        return false;
                    }
                });
            }
        }
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		Feed feed = adapter.getFeed();
		
		if ( feed == null ) {
			return;
		}

		boolean searchEnabled = feed.getSearchLink() != null;
		
		for ( int i=0; i < menu.size(); i++ ) {
			MenuItem item = menu.getItem(i);
			
			boolean enabled = false;
			
			switch (item.getItemId()) {

			case R.id.search:
				enabled = searchEnabled;
				break;			
			default:
				enabled = true;
			}			
			
			item.setEnabled(enabled);
			item.setVisible(enabled);			
		}
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		
		switch (item.getItemId()) {
		case android.R.id.home:
			navStack.clear();
			loadOPDSFeed(config.getBaseOPDSFeed());
			return true;
		case R.id.prefs:
			Intent prefsIntent = new Intent(getActivity(), PageTurnerPrefsActivity.class);
			startActivity(prefsIntent);
			break;
		case R.id.open_library:
			Intent libIntent = new Intent(getActivity(), LibraryActivity.class);
			startActivity(libIntent);
			getActivity().finish();
			break;
		}		
		
		return true;
	}

	// TODO Refactor this. Let the platform push/pop fragments from the fragment stack.
	public void onBackPressed() {
		if (navStack.isEmpty()) {
			getActivity().finish();
			return;
		} 	
		
		navStack.pop();
		
		if (navStack.isEmpty()) {
			loadOPDSFeed(config.getBaseOPDSFeed());
		} else if ( navStack.peek().equals(Catalog.CUSTOM_SITES_ID) ) {
			loadCustomSiteFeed();
		}else {
			loadOPDSFeed(navStack.peek());
		}
	}

    @Override
    public void notifyLinkUpdated() {
        adapter.notifyDataSetChanged();
    }

    @Override
	public void errorLoadingFeed(String error) {
		if ( isAdded() ) {
            Toast.makeText(getActivity(), getString(R.string.feed_failed) + ": " + error,
				Toast.LENGTH_LONG).show();
        }
	}

    public void setNewFeed(Feed result, ResultType resultType) {

        if (result != null && isAdded() ) {

            if ( resultType == null || resultType == ResultType.REPLACE ) {
                adapter.setFeed(result);
                ((CatalogParent) getActivity() ).onFeedReplaced(result);
            } else {
                this.adapter.setLoading(false);
                adapter.addEntriesFromFeed(result);
            }

            getSherlockActivity().supportInvalidateOptionsMenu();
            getSherlockActivity().getSupportActionBar().setTitle(result.getTitle());

            queueImageLoading(result);
        }
    }

    private void queueImageLoading( Feed feed ) {

        Map<String, byte[]> cache = new HashMap<String, byte[]>();

        for (final Entry entry : feed.getEntries()) {

            Link imageLink = getImageLink(feed, entry);

            if (imageLink != null) {
                String href = imageLink.getHref();

                // If the image is contained in the feed, load it
                // directly
                if (href.startsWith("data:image/png;base64")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                        ParseBinDataTask binDataTask = this.parseBinDataTaskProvider.get();
                        binDataTask.setLoadFeedCallback(this);

                        taskQueue.executeTask(binDataTask, imageLink);
                    }
                }else {

                    LoadThumbnailTask thumbnailTask = this.loadThumbnailTaskProvider.get();
                    thumbnailTask.setCache(cache);
                    thumbnailTask.setBaseUrl( feed.getURL() );
                    thumbnailTask.setLoadFeedCallback(this);

                    taskQueue.executeTask(thumbnailTask, imageLink);
                }
            }
        }

        cache.clear();
    }


    private void setSupportProgressBarIndeterminateVisibility(boolean enable) {
        SherlockFragmentActivity activity = getSherlockActivity();
        if ( activity != null) {
            LOG.debug("Setting progress bar to " + enable );
            activity.setSupportProgressBarIndeterminateVisibility(enable);
        } else {
            LOG.debug("Got null activity.");
        }
    }

    @Override
    public void onLoadingDone() {
        LOG.debug("Done loading.");
        setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoadingStart() {
        LOG.debug("Start loading.");
        setSupportProgressBarIndeterminateVisibility(true);
    }

    private class LoadingScrollListener implements AbsListView.OnScrollListener {

        private static final int LOAD_THRESHOLD = 2;

        private String lastLoadedUrl = "";

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            int lastVisibleItem = firstVisibleItem + visibleItemCount;

            if ( totalItemCount - lastVisibleItem  <= LOAD_THRESHOLD && adapter.getCount() > 0) {

                Entry lastEntry = adapter.getItem( adapter.getCount() -1 );
                Feed feed = lastEntry.getFeed();

                if ( feed == null || feed.getNextLink() == null) {
                    LOG.debug("Scroll down detected, but no next link available");
                    return;
                }

                Link nextLink = feed.getNextLink();

                if ( ! nextLink.getHref().equals(lastLoadedUrl) ) {
                    Entry nextEntry = new Entry();
                    nextEntry.setFeed(feed);
                    nextEntry.addLink(nextLink);

                    LOG.debug("Starting download for " + nextLink.getHref() + " after scroll");

                    lastLoadedUrl = nextLink.getHref();
                    loadURL(nextEntry, nextLink.getHref(), false, ResultType.APPEND);
                }
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }
    }

}
