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
package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.inject.Inject;

public class CatalogListAdapter extends BaseAdapter {
	
	private Feed feed;	
	private Context context;
	
	@Inject
	public CatalogListAdapter(Context context) {
		this.context = context;
	}
	
	public void setFeed( Feed feed ) {
		this.feed = feed;

        if ( feed.getNextLink() != null ) {

            Entry nextEntry = new Entry();
            nextEntry.addLink(feed.getNextLink());
            nextEntry.setTitle(context.getString(R.string.next_page));

            feed.addEntry(nextEntry);
        }

        if ( feed.getPreviousLink() != null ) {
            Entry prevEntry = new Entry();

            prevEntry.addLink(feed.getPreviousLink());
            prevEntry.setTitle(context.getString(R.string.prev_page));

            feed.addEntryAt(0, prevEntry);
        }


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
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		final Entry entry = getItem(position);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final Link imgLink = Catalog.getImageLink(getFeed(), entry);

		rowView = inflater.inflate(R.layout.catalog_item, parent, false);			 			

		Catalog.loadBookDetails(context, rowView, entry, imgLink, true );
		return rowView;
	}
	
}
