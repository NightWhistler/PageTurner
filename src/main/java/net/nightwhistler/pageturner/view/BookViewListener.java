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

import nl.siegmann.epublib.domain.Book;

/**
 * Listener interface for updates from a BookView.
 * 
 * @author Alex Kuiper
 *
 */
public interface BookViewListener {

	/**
	 * Called after the Bookview has successfully parsed the book.
	 * 
	 * @param book
	 */
	void bookOpened( Book book );
	
	/**
	 * Called if the book could not be opened for some reason.
	 * 
	 * @param errorMessage
	 */
	void errorOnBookOpening( String errorMessage );
	 
	/**
	 * Called when the BookView starts parsing a new entry
	 * of the book. Usually after a pageUp or pageDown event.
	 * 	
	 * @param entry
	 * @param name
	 */
	void parseEntryStart( int entry);
	
	/**
	 * Called after parsing is complete.
	 * 
	 * @param entry
	 * @param name
	 */
	void parseEntryComplete( int entry, String name );
	
	/** Indicates how far we've progressed in the book **/
	void progressUpdate( int progressPercentage );
	
}
