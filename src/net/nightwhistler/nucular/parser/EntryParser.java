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

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;

public class EntryParser extends ElementParser {
				
	private Entry entry;
	
	public EntryParser(Feed parent) {
		super("entry");
		this.entry = new Entry();
		parent.addEntry(entry);
	}	

	@Override
	protected ElementParser createChildParser(String tagName) {
		
		if ( tagName.equals("link")) {
			return new LinkParser(entry);
		} else if ( tagName.equals("content")) {
			return new ContentParser(entry);
		} else if ( tagName.equals("title") ) {
			return new TitleParser(entry);
		} else if ( tagName.equals("author")) {
			return new AuthorParser(entry);
		} else if ( tagName.equals("id")) {
			return new IDParser(entry);
		} else if ( tagName.equals("summary")) {
			return new SummaryParser(entry);
		}
		
		return super.createChildParser(tagName);
	}
}
