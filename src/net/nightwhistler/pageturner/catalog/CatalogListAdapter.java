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

import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

    private int displayDensity;

    private Entry loadingEntry = new Entry();

	@Inject
	public CatalogListAdapter(Context context) {
		this.context = context;
	}

    public void setLoading(boolean loading) {
        if ( loading ) {
            if ( feed.getSize() > 0 &&  !feed.getEntries().contains(this.loadingEntry) ) {

                Entry lastEntry = feed.getEntries().get( feed.getSize() - 1);
                feed.addEntry(this.loadingEntry);

                this.loadingEntry.setFeed(lastEntry.getFeed());

            }
        } else {
            feed.removeEntry(this.loadingEntry);
        }

        notifyDataSetChanged();
    }

    public void addEntriesFromFeed( Feed newFeed ) {
        for ( Entry entry: newFeed.getEntries() ) {
            this.feed.addEntry(entry);

            //Point the parent back at the original feed, bit of a hack
            entry.setFeed(newFeed);
        }

        this.notifyDataSetChanged();
    }

	public void setFeed( Feed feed ) {
		this.feed = feed;
		this.notifyDataSetChanged();		
	}
	
	public Feed getFeed() {
		return feed;
	}

    public void setDisplayDensity(int density) {
        this.displayDensity = density;
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

        if ( entry == this.loadingEntry ) {
            ProgressBar bar = new ProgressBar(this.context);
            bar.setIndeterminate(true);
            return bar;
        }

        if ( convertView == null || convertView instanceof  ProgressBar  ) {
		    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.catalog_item, parent, false);
        } else {
            rowView = convertView;
        }

        final Link imgLink = Catalog.getImageLink(getFeed(), entry);

		Catalog.loadBookDetails(context, rowView, entry, imgLink, true, Catalog.getMaxThumbnailWidth(this.displayDensity) );

        ImageView icon = (ImageView) rowView.findViewById(R.id.itemIcon);
        int maxWidth = Catalog.getMaxThumbnailWidth(displayDensity);
        icon.setMinimumWidth(maxWidth);

		return rowView;
	}
	
}
