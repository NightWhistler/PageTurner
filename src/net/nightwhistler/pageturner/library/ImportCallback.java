/*
 * Copyright (C) 2013 Alex Kuiper
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

import android.os.AsyncTask;

import java.util.List;

public interface ImportCallback {

    void booksDeleted(int numberOfDeletedBooks);

	void importComplete( int booksImported, List<String> failures, boolean emptyLibrary, boolean silent );
	
	void importStatusUpdate( String update, boolean silent );
		
	void importFailed( String reason, boolean silent );


}
