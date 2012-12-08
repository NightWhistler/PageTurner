/*
 * Copyright (C) 2011 Alex Kuiper
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

package net.nightwhistler.pageturner.view;

import java.util.List;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.tasks.SearchTextTask;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * ListAdapter class for progress downloaded from a synchronization
 * server.
 * 
 * @author Alex Kuiper
 *
 */
public class SearchResultAdapter extends ArrayAdapter<SearchTextTask.SearchResult> implements 
	DialogInterface.OnClickListener {

	private List<SearchTextTask.SearchResult> results;
	private BookView bookView;

	public SearchResultAdapter(Context context, BookView bookView, 
			List<SearchTextTask.SearchResult> books) {
		super(context, R.id.deviceName, books);
		this.results = books;
		this.bookView = bookView;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		bookView.navigateBySearchResult(this.results, which);    		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView;

		if ( convertView == null ) {
			LayoutInflater inflater = (LayoutInflater) 
			getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			rowView = inflater.inflate(R.layout.progress_row, parent, false);
		} else {
			rowView = convertView;
		}

		TextView deviceView = (TextView) rowView.findViewById(R.id.deviceName);
		TextView dateView = (TextView) rowView.findViewById(R.id.timeStamp );

		SearchTextTask.SearchResult progress = results.get(position);

		deviceView.setText( progress.getDisplay() );
		dateView.setText( progress.getPercentage() + "%" );

		return rowView;

	}
}
