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
		Link link = new Link(
				attributes.get("template"),
				attributes.get("type"),
				attributes.get("rel"),
                attributes.get("title"));
		
		this.element.addLink(link);
	}

}
