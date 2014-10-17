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
package net.nightwhistler.nucular.atom;

import jedi.option.Option;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static jedi.functional.FunctionalPrimitives.firstOption;
import static jedi.option.Options.some;

public abstract class AtomElement implements Serializable {

	private String title;
	private String id;
	private Content content;
	
	private Author author;
	
	private List<Link> links = new ArrayList<Link>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public List<Link> getLinks() {
		return Collections.unmodifiableList( links );
	}		
	
	public void addLink( Link link ) {
		this.links.add(link);
	}	
	
	public Option<Link> findByRel(String rel) {
        return firstOption( getLinks(),
                link -> link.getRel() != null && link.getRel().equals(rel) );
	}
}
