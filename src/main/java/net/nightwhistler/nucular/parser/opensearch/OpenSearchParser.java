package net.nightwhistler.nucular.parser.opensearch;

import java.util.Map;

import net.nightwhistler.nucular.parser.ElementParser;

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
