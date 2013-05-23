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

import net.nightwhistler.nucular.atom.AtomElement;
import net.nightwhistler.nucular.atom.Link;

import java.util.Map;

public class LinkParser extends ElementParser {

	private AtomElement element;
	
	public LinkParser(AtomElement parent) {
		super("link");
		this.element = parent;
	}	
	
	@Override
	public void setAttributes(Map<String, String> attributes) {
		Link link = new Link(
				attributes.get("href"),
				attributes.get("type"),
				attributes.get("rel"),
                attributes.get("title"));
		
		this.element.addLink(link);
	}
	
}
