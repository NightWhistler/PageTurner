package net.nightwhistler.nucular.parser.opensearch;

import java.util.Map;

import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.nucular.parser.ElementParser;

public class UrlParser extends ElementParser {

	private SearchDescription element;
	
	public UrlParser(SearchDescription parent) {
		super("Url");
		this.element = parent;
	}	
	
	@Override
	public void setAttributes(Map<String, String> attributes) {
		Link link = new Link();
		
		link.setHref(attributes.get("template"));
		link.setRel(attributes.get("rel"));
		link.setType(attributes.get("type"));
				
		this.element.addLink(link);
	}

}
