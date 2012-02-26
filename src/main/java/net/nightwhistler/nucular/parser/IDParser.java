package net.nightwhistler.nucular.parser;

import net.nightwhistler.nucular.atom.AtomElement;

public class IDParser extends ElementParser {

	private AtomElement parent;
	
	public IDParser(AtomElement parent) {		
		super("id");
		this.parent = parent;
	}
	
	@Override
	public void setTextContent(String text) {
		parent.setId(text);
	}
}
