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

package net.nightwhistler.nucular.parser.opensearch;

import net.nightwhistler.nucular.parser.ElementParser;

import java.util.Map;

public class OpenSearchParser extends ElementParser {
	
	private SearchDescription desc;
	
	public OpenSearchParser() {
		super("OpenSearchDescription");
		this.desc = new SearchDescription();
	}
	
	public SearchDescription getDesc() {
		return desc;
	}
	
	@Override
	public void startElement(String name, Map<String, String> attributes) {
		if ( ! name.equals("OpenSearchDescription") ) { //Minor bootstrapping hack
			super.startElement(name, attributes);
		}
	}
	
	@Override
	protected ElementParser createChildParser(String tagName) {
		
		if ( tagName.equals("Url")) {
			return new UrlParser(desc);
		} 
		
		return super.createChildParser(tagName);
	}


}
