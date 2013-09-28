/*
 * Copyright (C) 2013 Alex Kuiper, Rob Hoelz
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
package net.nightwhistler.bookmark.pageturner;

import android.content.ContentValues;

public class Bookmark {
	private String fileName;
	private String name;
	private int index;
	private int position;

	public Bookmark(String fileName, String name, int index, int position)
	{
		this.fileName = fileName;
		this.name     = name;
		this.index    = index;
		this.position = position;
	}

	void populateContentValues(ContentValues row)
	{
		row.put("file_name", this.fileName );
		row.put("name", this.name );
		row.put("book_index", this.index );
		row.put("book_position", this.position );
	}
}
