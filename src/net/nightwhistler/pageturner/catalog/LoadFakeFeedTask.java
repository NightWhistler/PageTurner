package net.nightwhistler.pageturner.catalog;

import java.io.IOException;
import java.util.HashMap;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;

public class LoadFakeFeedTask extends AsyncTask<Link, Integer, Void> {

    private LoadFeedCallback callback;

	private static final Logger LOG = LoggerFactory.getLogger("LoadFakeFeedTask");
	
	private Context context;	
	private HttpClient client;

    private String baseURL;

	@Inject
	public LoadFakeFeedTask(Context context, HttpClient httpClient) {
		this.context = context;
		this.client = httpClient;
	}


    public void setCallback( LoadFeedCallback callback ) {
        this.callback = callback;
    }

    public void setBaseURL( String baseURL ) {
        this.baseURL = baseURL;
    }

	@Override
	protected void onPreExecute() {
		callback.onLoadingStart();
	}

	@Override
	protected Void doInBackground(Link... params) {

		try {
			Catalog.loadImageLink(client, null,	params[0], baseURL);

		} catch (IOException io) {
			LOG.error("Could not load image: ", io);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
        callback.notifyLinkUpdated();
        callback.onLoadingDone();
	}
}

