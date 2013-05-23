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
import net.nightwhistler.nucular.atom.Content;

import java.util.Map;

public class ContentParser extends ElementParser {
	
	private Content content;
	
	StringBuffer buffer;
	
	private boolean finished;
	
	public ContentParser(AtomElement parent) {
		super("content");
		this.content = new Content();
		parent.setContent(content);
		
		this.buffer = new StringBuffer();
		this.finished = false;
	}
	
	@Override
	public void startElement(String name, Map<String, String> attributes) {
		this.buffer.append("<" + name + ">");
	}
	
	@Override
	public boolean isFinished() {
		return finished;
	}
	
	@Override
	public void setAttributes(Map<String, String> attributes) {
		this.content.setType(attributes.get("type"));
	}
	
	@Override
	public void endElement(String name) {
		
		if ( name.equals("content") ) {
			finished = true;
			this.content.setText(buffer.toString());
		} else {		
			this.buffer.append( "</" + name + ">" );
		}
		
	}
	
	@Override
	public void setTextContent(String text) {
		this.buffer.append( text );
	}
}
