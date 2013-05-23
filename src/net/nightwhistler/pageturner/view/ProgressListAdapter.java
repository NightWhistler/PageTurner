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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import net.nightwhistler.pageturner.PlatformUtil;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.sync.BookProgress;
import net.nightwhistler.pageturner.view.bookview.BookView;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * ListAdapter class for progress downloaded from a synchronization
 * server.
 * 
 * @author Alex Kuiper
 *
 */
public class ProgressListAdapter extends ArrayAdapter<BookProgress> implements 
	DialogInterface.OnClickListener {

	private List<BookProgress> books;
	private BookView bookView;
	
	public ProgressListAdapter(Context context, BookView bookView, 
			List<BookProgress> books) {
		super(context, R.id.deviceName, books);
		this.books = books;
		this.bookView = bookView;		
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		BookProgress progress = books.get(which);
		bookView.navigateTo(progress.getIndex(), progress.getProgress() );    		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView;

		if ( convertView == null ) {
			LayoutInflater inflater = PlatformUtil.getLayoutInflater(getContext());
			rowView = inflater.inflate(R.layout.progress_row, parent, false);
		} else {
			rowView = convertView;
		}		
		

		TextView deviceView = (TextView) rowView.findViewById(R.id.deviceName);
		TextView dateView = (TextView) rowView.findViewById(R.id.timeStamp );
		
		if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ) {
			deviceView.setTextColor( Color.BLACK );
			dateView.setTextColor( Color.BLACK );			
		}	
		
		BookProgress progress = books.get(position);

		deviceView.setText( progress.getDeviceName() + " - " + progress.getPercentage() + "%" );
		dateView.setText( SimpleDateFormat.getDateTimeInstance().format(progress.getTimeStamp()) );

		return rowView;

	}
}
