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

import java.io.Serializable;
import java.util.Date;

public class LibraryBook implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4417866928191974513L;

	private String fileName;
	
	private String title;
	
	private Author author;
	
	private byte[] coverImage;
		
	private Date lastRead;
	
	private Date addedToLibrary;
	
	private String description;
	
	private int progress;
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public byte[] getCoverImage() {
		return coverImage;
	}

	public void setCoverImage(byte[] coverImage) {
		this.coverImage = coverImage;
	}	

	public Date getLastRead() {
		return lastRead;
	}

	public void setLastRead(Date lastRead) {
		this.lastRead = lastRead;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public Date getAddedToLibrary() {
		return addedToLibrary;
	}
	
	public void setAddedToLibrary(Date addedToLibrary) {
		this.addedToLibrary = addedToLibrary;
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
	}
	
	public int getProgress() {
		return progress;
	}
}
