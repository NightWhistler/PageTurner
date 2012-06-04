package net.nightwhistler.nucular.parser.opensearch;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.nucular.atom.AtomConstants;
import net.nightwhistler.nucular.atom.Link;

public class SearchDescription {

	private List<Link> links = new ArrayList<Link>();
	
	public void addLink( Link link ) {
		this.links.add(link);
	}
	
	public Link getSearchLink() {
		for ( Link l: this.links ) {
			if ( AtomConstants.TYPE_ATOM.equals(l.getType() )) {
				return l;
			}
		}
		
		return null;
	}
}
