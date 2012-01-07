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
package net.nightwhistler.pageturner.library;

import nl.siegmann.epublib.domain.Book;


public interface LibraryService {
	
	public void storeBook( String fileName, Book book, boolean updateLastRead );
	
	public QueryResult<LibraryBook> findAllByLastRead();
	
	public QueryResult<LibraryBook> findAllByLastAdded();
	
	public QueryResult<LibraryBook> findAllByTitle();
	
	public QueryResult<LibraryBook> findAllByAuthor();
	
	public QueryResult<LibraryBook> findUnread();
	
	public boolean hasBook( String fileName );
	
	public void deleteBook( String fileName );
	
	public void close();
}
