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
import net.nightwhistler.nucular.parser.opensearch.OpenSearchParser;
import net.nightwhistler.nucular.parser.opensearch.SearchDescription;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

public class Nucular {

	public static Feed readAtomFeedFromStream(InputStream stream)
			throws ParserConfigurationException, SAXException, IOException {

		FeedParser feedParser = new FeedParser();		
		parseStream(feedParser, stream );
		return feedParser.getFeed();
	}
	
	public static SearchDescription readOpenSearchFromStream(InputStream stream ) 
			throws ParserConfigurationException, SAXException, IOException {
		OpenSearchParser search = new OpenSearchParser();
		parseStream(search, stream);
		
		return search.getDesc();
	}
	
	private static void parseStream( ElementParser rootElementsParser, InputStream stream ) 
			throws ParserConfigurationException, SAXException, IOException {
		
		SAXParserFactory parseFactory = SAXParserFactory.newInstance();
		SAXParser xmlParser = parseFactory.newSAXParser();

		XMLReader xmlIn = xmlParser.getXMLReader();
		
		StreamParser catalogParser = new StreamParser( rootElementsParser );
		xmlIn.setContentHandler(catalogParser);

		xmlIn.parse(new InputSource(stream));
	}

}
