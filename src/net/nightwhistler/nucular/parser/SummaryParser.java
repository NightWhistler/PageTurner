package net.nightwhistler.nucular.parser;

import net.nightwhistler.nucular.atom.Entry;

public class SummaryParser extends ElementParser {

	private Entry parent;
	
	public SummaryParser(Entry parent) {
		super("summary");
		this.parent = parent;
	}
	
	@Override
	public void setTextContent(String text) {
		parent.setSummary(text);
	}
}
