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

import net.nightwhistler.nucular.atom.AtomElement;
import net.nightwhistler.nucular.atom.Author;

public class AuthorParser extends ElementParser {
	
	private Author author;

	public AuthorParser(AtomElement parent) {
		super("author");
		this.author = new Author();
		parent.setAuthor(author);
	}
	
	@Override
	protected ElementParser createChildParser(String tagName) {
		
		if ( tagName.equals("name")) {
			return new NameParser();
		} else if ( tagName.equals("uri")) {
			return new UriParser();
		} else if ( tagName.equals("email")) {
			return new EmailParser();
		}
		
		return super.createChildParser(tagName);
	}
	
	private class NameParser extends ElementParser {
		public NameParser() {
			super("name");
		}
		
		@Override
		public void setTextContent(String text) {
			author.setName(text);
		}
	}
	
	private class UriParser extends ElementParser {
		public UriParser() {
			super("uri");
		}
		
		@Override
		public void setTextContent(String text) {
			author.setUri(text);
		}
	}
	
	private class EmailParser extends ElementParser {
		public EmailParser() {
			super("email");
		}
		
		@Override
		public void setTextContent(String text) {
			author.setEmail(text);
		}
	}
	
}
