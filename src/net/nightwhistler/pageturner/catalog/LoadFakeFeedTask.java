package net.nightwhistler.pageturner.catalog;

import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.pageturner.R;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;

public class LoadFakeFeedTask extends AsyncTask<String, Integer, Feed> {

	private Entry singleEntry;
	private LoadFeedCallback callBack;
	private Dialog waitDialog;
	
	private static final Logger LOG = LoggerFactory.getLogger("LoadFakeFeedTask");
	
	@Inject
	private Context context;

	public LoadFakeFeedTask() {		
	}
	
	public void setCallBack(LoadFeedCallback callBack) {
		this.callBack = callBack;
	}	
	
	public LoadFakeFeedTask setSingleEntry(Entry singleEntry) {
		this.singleEntry = singleEntry;
		return this;
	}
	
	public void setWaitDialog(Dialog waitDialog) {
		this.waitDialog = waitDialog;
	}	

	@Override
	protected void onPreExecute() {
		waitDialog.setTitle(context.getString(R.string.loading_wait));
		waitDialog.show();
	}

	@Override
	protected Feed doInBackground(String... params) {
		Feed fakeFeed = new Feed();
		fakeFeed.addEntry(singleEntry);
		fakeFeed.setTitle(singleEntry.getTitle());

		try {
			Catalog.loadImageLink(new HashMap<String, byte[]>(),
					singleEntry.getImageLink(), params[0]);
		} catch (IOException io) {
			LOG.error("Could not load image: ", io);
		}

		return fakeFeed;
	}

	@Override
	protected void onPostExecute(Feed result) {
		callBack.setNewFeed(result);
	}
}

