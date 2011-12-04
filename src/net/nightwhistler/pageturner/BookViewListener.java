/*
 * Copyright (C) 2011 Alex Kuiper
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nightwhistler.pageturner;

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
