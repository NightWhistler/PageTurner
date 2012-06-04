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

import static net.nightwhistler.nucular.atom.AtomConstants.REL_BUY;
import static net.nightwhistler.nucular.atom.AtomConstants.REL_COVER;
import static net.nightwhistler.nucular.atom.AtomConstants.REL_IMAGE;
import static net.nightwhistler.nucular.atom.AtomConstants.REL_STANZA_COVER_IMAGE;
import static net.nightwhistler.nucular.atom.AtomConstants.REL_STANZA_THUMBNAIL_IMAGE;
import static net.nightwhistler.nucular.atom.AtomConstants.REL_THUMBNAIL;
import static net.nightwhistler.nucular.atom.AtomConstants.REL_THUMBNAIL_ALT;
import static net.nightwhistler.nucular.atom.AtomConstants.TYPE_ATOM;
import static net.nightwhistler.nucular.atom.AtomConstants.TYPE_EPUB;

import java.util.List;

public class Entry extends AtomElement {

	private String updated;
	private String summary;	

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}		
	
	public Link getAtomLink() {
		List<Link> links = getLinks();
		
		for ( Link link: links ) {
			if ( link.getType().startsWith(TYPE_ATOM)) {
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
	
	private Link findByRel(String... items) {
		Link link = null;
		for ( int i=0; i < items.length && link == null; i++ ) {
			link = findByRel( items[i] );
		}
		
		return link;
	}
	
	public Link getThumbnailLink() {		
		return findByRel(REL_THUMBNAIL, REL_THUMBNAIL_ALT, REL_STANZA_THUMBNAIL_IMAGE);
	}
	
	public Link getImageLink() {
		return findByRel(REL_IMAGE, REL_COVER, REL_STANZA_COVER_IMAGE );		
	}
	
	public Link getBuyLink() {
		return findByRel(REL_BUY);
	}
	
	public Link getEpubLink() {
		for ( Link link: getLinks() ) {
			if ( link.getType().equals(TYPE_EPUB)) {
				return link;
			}
		}
		
		return null;
	}
}
