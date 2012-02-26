package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class CatalogListAdapter extends BaseAdapter {
	
	private Feed feed;	
	
	
	public void setFeed( Feed feed ) {
		this.feed = feed;
		this.notifyDataSetChanged();		
	}
	
	public Feed getFeed() {
		return feed;
	}

	@Override
	public int getCount() {
		
		if ( feed == null ) {
			return 0;
		}
		
		return feed.getEntries().size();
	}
	
	@Override
	public Entry getItem(int position) {
		return feed.getEntries().get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	
	
}
