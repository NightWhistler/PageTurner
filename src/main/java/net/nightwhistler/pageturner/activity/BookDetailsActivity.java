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
package net.nightwhistler.pageturner.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.inject.Inject;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.R.drawable;
import net.nightwhistler.pageturner.R.id;
import net.nightwhistler.pageturner.R.layout;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class BookDetailsActivity extends RoboActivity {
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
	
	@InjectView(R.id.coverImage)
	private ImageView coverView;
	
	@InjectExtra("book")
	private String book;
	
	@InjectView(R.id.titleField) 
	private TextView titleView;
	
	@InjectView(R.id.authorField)
	private TextView authorView;
	
	@InjectView(R.id.lastRead)
	private TextView lastRead;
	
	@InjectView(R.id.addedToLibrary)
	private TextView added;
	
	@InjectView(R.id.bookDescription)
	private TextView descriptionView;
	
	@Inject
	private LibraryService libraryService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.book_details);
		
		LibraryBook libraryBook = libraryService.getBook(this.book);
		
		if ( libraryBook != null ) {

			if ( libraryBook.getCoverImage() != null ) {			
				coverView.setImageBitmap( BitmapFactory.decodeByteArray(libraryBook.getCoverImage(),
						0, libraryBook.getCoverImage().length));
			} else {			
				coverView.setImageDrawable( getResources().getDrawable(R.drawable.river_diary));
			}

			titleView.setText(libraryBook.getTitle());		
			authorView.setText( "by " + libraryBook.getAuthor().getFirstName() + " " + libraryBook.getAuthor().getLastName() );

			if (libraryBook.getLastRead() != null && ! libraryBook.getLastRead().equals(new Date(0))) {			
				lastRead.setText("Last read: " + DATE_FORMAT.format(libraryBook.getLastRead()) );
			}

			added.setText( "Added to library: " + DATE_FORMAT.format(libraryBook.getAddedToLibrary()) );
			descriptionView.setText(libraryBook.getDescription());
		}
	}
	
}
