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

import java.util.List;

public class Entry extends AtomElement {

	private String updated;
	private String summary;
	
	public static final String THUMBNAIL = "http://opds-spec.org/image/thumbnail";
	public static final String THUMBNAIL_ALT = "http://opds-spec.org/thumbnail";
	
	public static final String BUY = "http://opds-spec.org/acquisition/buy";
	public static final String IMAGE = "http://opds-spec.org/image";
	public static final String COVER = "http://opds-spec.org/cover";

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}	
	
	private Link findByRel(String rel) {
		for ( Link link: getLinks() ) {
			if (link.getRel() != null && link.getRel().equals(rel)) {
				return link;
			}
		}
		
		return null;
	}
	
	public Link getAtomLink() {
		List<Link> links = getLinks();
		
		for ( Link link: links ) {
			if ( link.getType().startsWith("application/atom+xml")) {
				return link;
			}
		}
		
		return null;
	}
	
	public String getSummary() {
		return summary;
	}
	
	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	public Link getThumbnailLink() {
		Link l = findByRel(THUMBNAIL);
		if ( l != null ) {
			return l;
		} else {
			return findByRel(THUMBNAIL_ALT);
		}
	}
	
	public Link getImageLink() {
		Link l = findByRel(IMAGE);
		if ( l != null ) {
			return l;
		} else {
			return findByRel(COVER);
		}
	}
	
	public Link getBuyLink() {
		return findByRel(BUY);
	}
	
	public Link getEpubLink() {
		for ( Link link: getLinks() ) {
			if ( link.getType().equals("application/epub+zip")) {
				return link;
			}
		}
		
		return null;
	}
}
