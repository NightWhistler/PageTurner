
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

import java.util.Map;

public abstract class ElementParser {
	
	private String elementName;
	
	private ElementParser childParser;
	
	private boolean finished;
	
	public ElementParser(String elementName) {
		this.elementName = elementName;
		this.finished = false;
	}

	public void startElement( String name, Map<String, String> attributes ) {
		
		if ( this.childParser != null ) {
			childParser.startElement(name, attributes);
		} else {
			this.childParser = createChildParser(name);		
			this.childParser.setAttributes(attributes);
		}
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public void endElement( String name ) {
		
		if ( this.childParser != null ) {
			this.childParser.endElement(name);
			
			if ( childParser.isFinished() ) {
				childParser = null;
			}
		} else {
			if ( name.equals(this.elementName) ) {
				this.finished = true;
			}
		}
		
	}
	
	public void setTextContent( String text ) {
		if ( childParser != null ) {
			childParser.setTextContent(text);
		}
	}
	
	public void setAttributes( Map<String, String> attributes ) {}

	protected ElementParser createChildParser(String tagName) {
		return new UnknownElementParser(tagName);
	}

}
