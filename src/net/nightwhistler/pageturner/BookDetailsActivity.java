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
package net.nightwhistler.pageturner;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import net.nightwhistler.pageturner.library.LibraryBook;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class BookDetailsActivity extends Activity {
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.book_details);
		
		LibraryBook book = (LibraryBook) getIntent().getExtras().getSerializable( "book" );
		ImageView coverView = (ImageView) findViewById(R.id.coverImage);
		
		if ( book.getCoverImage() != null ) {			
			coverView.setImageBitmap( book.getCoverImage() );
		} else {			
			coverView.setImageDrawable( getResources().getDrawable(R.drawable.river_diary));
		}
		
		TextView titleView = (TextView) findViewById(R.id.titleField);
		titleView.setText(book.getTitle());		
		
		TextView authorView = (TextView) findViewById(R.id.authorField);
		authorView.setText( "by " + book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName() );
		
		if (book.getLastRead() != null && ! book.getLastRead().equals(new Date(0))) {
			TextView lastRead = (TextView) findViewById(R.id.lastRead);
			lastRead.setText("Last read: " + DATE_FORMAT.format(book.getLastRead()) );
		}
		
		TextView added = (TextView) findViewById(R.id.addedToLibrary);
		added.setText( "Added to library: " + DATE_FORMAT.format(book.getAddedToLibrary()) );
		
		TextView descriptionView = (TextView) findViewById(R.id.bookDescription);
		descriptionView.setText(book.getDescription());
	}
	
}
