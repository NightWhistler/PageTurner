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
import android.widget.BaseAdapter;

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
