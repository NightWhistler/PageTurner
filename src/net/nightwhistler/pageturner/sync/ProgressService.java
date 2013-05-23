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
package net.nightwhistler.pageturner.sync;

import java.util.List;

public interface ProgressService {

	/**
	 * Stores the progress for the given book.
	 * @param userId
	 * @param fileName
	 * @param progress
	 */
	public void storeProgress( String fileName, int index, int progress, int percentage ) throws AccessException;
	
	/**
	 * Returns the progress, or -1 of it wasn't found.
	 * 
	 * @param fileName
	 * @return
	 */
	public List<BookProgress> getProgress( String fileName ) throws AccessException;

}
