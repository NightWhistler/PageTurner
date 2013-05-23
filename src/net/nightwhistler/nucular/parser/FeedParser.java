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
package net.nightwhistler.nucular.parser;

import net.nightwhistler.nucular.atom.Feed;

import java.util.Map;


public class FeedParser extends ElementParser {
	
	private Feed feed;
	
	public FeedParser() {
		super("feed");
		this.feed = new Feed();
	}
	
	public Feed getFeed() {
		return feed;
	}
	
	@Override
	public void startElement(String name, Map<String, String> attributes) {
		if ( ! name.equals("feed") ) { //Minor bootstrapping hack
			super.startElement(name, attributes);
		}
	}
	
	@Override
	protected ElementParser createChildParser(String tagName) {
		
		if ( tagName.equals("link")) {
			return new LinkParser(feed);
		} else if ( tagName.equals("content")) {
			return new ContentParser(feed);
		} else if ( tagName.equals("title") ) {
			return new TitleParser(feed);
		} else if ( tagName.equals("entry")) {
			return new EntryParser(feed);
		} else if ( tagName.equals("author")) {
			return new AuthorParser(feed);
		} else if ( tagName.equals("id")) {
			return new IDParser(feed);
		}
		
		return super.createChildParser(tagName);
	}

}
