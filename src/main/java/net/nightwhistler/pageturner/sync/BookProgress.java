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

import java.util.Date;

public class BookProgress {

	String fileName;
	
	int index;
	
	private int progress;
	
	private Date timeStamp;
	private int percentage;
	
	private String deviceName;
	
	public BookProgress( String fileName, int index, int progress, int percentage,
			Date timeStamp, String deviceName ) {
		this.fileName = fileName;
		this.index = index;
		this.progress = progress;
		this.percentage = percentage;
		this.timeStamp = timeStamp;
		this.deviceName = deviceName;
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
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public int getPercentage() {
		return percentage;
	}
	
	public Date getTimeStamp() {
		return timeStamp;
	}
	
}
