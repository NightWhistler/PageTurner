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

public class BookProgress {

	String fileName;
	
	int index;
	
	private int progress;
	
	public BookProgress( String fileName, int index, int progress ) {
		this.fileName = fileName;
		this.index = index;
		this.progress = progress;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public int getIndex() {
		return index;
	}
	
	public int getProgress() {
		return progress;
	}
	
}
