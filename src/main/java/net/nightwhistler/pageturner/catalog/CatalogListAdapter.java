package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.pageturner.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CatalogListAdapter extends BaseAdapter {
	
	private Feed feed;	
	private Context context;
	
	HtmlSpanner spanner = new HtmlSpanner();
	
	public CatalogListAdapter(Context context) {
		this.context = context;
	}
	
	public void setFeed( Feed feed ) {
		this.feed = feed;
		this.notifyDataSetChanged();
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
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		
		if ( convertView == null ) {			
			LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.catalog_item, parent, false);
		} else {
			rowView = convertView;
		}
		
		Entry entry = getItem(position);
		
		TextView title = (TextView) rowView.findViewById(R.id.itemTitle);
		TextView desc = (TextView) rowView.findViewById(R.id.itemDescription );
		ImageView icon = (ImageView) rowView.findViewById(R.id.itemIcon);
		
		
		
		title.setText(entry.getTitle());
		
		if ( entry.getContent() != null ) {
			desc.setText( spanner.fromHtml( entry.getContent().getText() ));
		} else {
			desc.setText("");
		}
	
		return rowView;
	}	
	
}
