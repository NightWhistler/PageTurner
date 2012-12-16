package net.nightwhistler.nucular.parser;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
