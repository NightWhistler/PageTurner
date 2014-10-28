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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.google.inject.Inject;
import jedi.functional.FunctionalPrimitives;
import jedi.option.Option;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;

import static jedi.option.Options.none;
import static jedi.option.Options.option;
import static jedi.option.Options.some;
import static net.nightwhistler.pageturner.catalog.Catalog.getImageLink;

public class CatalogListAdapter extends BaseAdapter {
	
	private Feed feed;	
	private Context context;

    private Entry loadingEntry = new Entry();

    private CatalogImageLoader imageLoader;

    public static interface CatalogImageLoader {
        Option<Drawable> getThumbnailFor( String baseURL, Link link );
    }

	@Inject
	public CatalogListAdapter(Context context) {
		this.context = context;
	}

    public void setImageLoader( CatalogImageLoader imageLoader ) {
        this.imageLoader = imageLoader;
    }

    public void setLoading(boolean loading) {
        if ( loading ) {
            if ( feed.getSize() > 0 &&  !feed.getEntries().contains(this.loadingEntry) ) {

                Entry lastEntry = feed.getEntries().get( feed.getSize() - 1);
                feed.addEntry(this.loadingEntry);

                lastEntry.getFeed().forEach( f -> this.loadingEntry.setFeed(f) );
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
	
	public Option<Feed> getFeed() {
		return option(feed);
	}

	@Override
	public int getCount() {
		
		if ( feed == null ) {
			return 0;
		}
		
		return feed.getEntries().size();
	}
	
	@Override
	public Option<Entry> getItem(int position) {
        if ( position >= 0 && position < feed.getEntries().size() ) {
		    return some(feed.getEntries().get(position));
        }

        return none();
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		final Option<Entry> entry = getItem(position);

        if ( entry.unsafeGet() == this.loadingEntry ) {
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

        if ( ! FunctionalPrimitives.isEmpty(entry) ) {
            Catalog.loadBookDetails(rowView, entry.unsafeGet(), true);
        }

        ImageView icon = (ImageView) rowView.findViewById(R.id.itemIcon);

        Option<Drawable> drawableOption = none();

        if ( ! FunctionalPrimitives.isEmpty(entry) ) {
            drawableOption = getThumbnail( entry.unsafeGet() );
        }

        icon.setImageDrawable( drawableOption.getOrElse(
                context.getResources().getDrawable(R.drawable.unknown_cover)
        ));

		return rowView;
	}

    private Option<Drawable> getThumbnail( Entry entry ) {

        Option<Feed> feed = getFeed();

        if ( FunctionalPrimitives.isEmpty( feed ) ) {
            return none();
        }

        final Option<Link> imgLink = getImageLink( feed.unsafeGet(), entry );

        if ( this.imageLoader == null || FunctionalPrimitives.isEmpty(imgLink) ) {
            return none();
        }

        return this.imageLoader.getThumbnailFor( entry.getBaseURL(), imgLink.unsafeGet() );
    }
	
}
