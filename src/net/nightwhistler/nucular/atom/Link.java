/*
 * Copyright (C) 2012 Alex Kuiper
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
package net.nightwhistler.nucular.atom;


public class Link {

	private String href;
	private String type;
	private String rel;
	
	public Link( String href, String type, String rel ) {
		this.href = href;
		this.type = type;
		this.rel = rel;
	}
	
	private byte[] binData;
	
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;		
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getRel() {
		return rel;
	}
	public void setRel(String rel) {
		this.rel = rel;
	}
	
	public byte[] getBinData() {
		return binData;
	}
	
	public void setBinData(byte[] binData) {
		this.binData = binData;
	}
	
}
