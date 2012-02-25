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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.nightwhistler.nucular.atom.Feed;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class Nucular extends DefaultHandler {

	private FeedParser feedParser;

	public static Feed readFromStream(InputStream stream)
			throws ParserConfigurationException, SAXException, IOException {

		SAXParserFactory parseFactory = SAXParserFactory.newInstance();
		SAXParser xmlParser = parseFactory.newSAXParser();

		XMLReader xmlIn = xmlParser.getXMLReader();
		Nucular catalogParser = new Nucular();
		xmlIn.setContentHandler(catalogParser);

		xmlIn.parse(new InputSource(stream));

		return catalogParser.getFeed();
	}
	
	private Nucular() {
		this.feedParser = new FeedParser();
	}

	public Feed getFeed() {
		return feedParser.getFeed();
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

		this.feedParser.startElement(pickName(qName, localName), attrMap);
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

		StringBuffer buff = new StringBuffer();
		buff.append(ch, start, length);

		this.feedParser.setTextContent(buff.toString());
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		this.feedParser.endElement(pickName(qName,localName));
	}

}
