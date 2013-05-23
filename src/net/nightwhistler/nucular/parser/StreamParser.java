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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;

public class StreamParser extends DefaultHandler {

	private ElementParser rootParser;
	
	public StreamParser( ElementParser rootElementParser ) {
		this.rootParser = rootElementParser;
	}

	private String pickName( String qName, String localName ) {
		if ( localName.length() == 0 ) {
			return qName;
		} else {
			return localName;
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		Map<String, String> attrMap = new HashMap<String, String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			String value = attributes.getValue(i);
			String key = attributes.getLocalName(i);
			attrMap.put(key, value);
		}

		this.rootParser.startElement(pickName(qName, localName), attrMap);
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

		StringBuffer buff = new StringBuffer();
		buff.append(ch, start, length);

		this.rootParser.setTextContent(buff.toString());
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		this.rootParser.endElement(pickName(qName,localName));
	}

	
}
