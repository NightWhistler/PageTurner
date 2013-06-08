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

import java.util.ArrayList;
import java.util.List;

import static net.nightwhistler.nucular.atom.AtomConstants.*;

public class Entry extends AtomElement {

	private String updated;
	private String summary;

    private Feed feed;

    private String baseURL;

	public String getUpdated() {
		return updated;
	}

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL( String baseURL ) {
        this.baseURL = baseURL;
    }

	public void setUpdated(String updated) {
		this.updated = updated;
	}

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public Feed getFeed() {
        return feed;
    }

    public Link getWebsiteLink() {
        return findByRel(AtomConstants.REL_WEBSITE);
    }

	public Link getAlternateLink() {
		Link atomLink = getAtomLink();
		if ( atomLink != null && atomLink.getRel() != null && atomLink.getRel().equalsIgnoreCase(REL_ALTERNATE)) {
			return atomLink;
		}
		
		return null;
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

    public List<Link> getAlternateLinks() {

        List<Link> result = new ArrayList<Link>();

        for ( Link link: getLinks() ) {

            String rel = link.getRel() != null ? link.getRel() : "";
            String type = link.getType() != null ? link.getType() : "";

            if ( rel.equals(REL_RELATED) && type.startsWith(TYPE_ATOM ) ) {
                result.add(link);
            }
        }

        return result;

    }

	public Link getThumbnailLink() {		
		return findByRel(REL_THUMBNAIL, REL_THUMBNAIL_ALT, REL_STANZA_THUMBNAIL_IMAGE);
	}
	
	public Link getImageLink() {
		return findByRel(REL_IMAGE, REL_COVER, REL_STANZA_COVER_IMAGE );		
	}
	
	public Link getBuyLink() {
		return findByRel(REL_BUY, REL_STANZA_BUY);
	}
	
	public Link getEpubLink() {
		for ( Link link: getLinks() ) {
			if (link.getType() != null && link.getType().equals(TYPE_EPUB)) {
				return link;
			}
		}
		
		return null;
	}
}
