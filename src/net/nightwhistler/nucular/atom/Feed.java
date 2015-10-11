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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.nightwhistler.nucular.atom.AtomConstants.*;

/**
 * Represents a low-level Atom feed, as parsed from XML.
 *
 * @author Alex Kuiper
 *
 */
public class Feed extends AtomElement {

    private boolean detailFeed;
    private boolean searchFeed;

    private String url;

    private List<Entry> entries = new ArrayList<Entry>();

    public List<Entry> getEntries() {
        return Collections.unmodifiableList( entries );
    }

    public void addEntryAt(int position, Entry entry) {
        this.entries.add(position, entry);
    }

    public void addEntry(Entry entry) {
        this.entries.add(entry);
        entry.setFeed(this);
    }

    public void removeEntry(Entry entry) {
        this.entries.remove(entry);
    }

    public Option<Link> getNextLink() {
        return findByRel(REL_NEXT);
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getURL() {
        return this.url;
    }

    public Option<Link> getPreviousLink() {
        return findByRel(REL_PREV);
    }

    public Option<Link> getSearchLink() {
        Option<Link> link = findByRel(REL_SEARCH);

        return link.filter( l -> l.getType().equals(AtomConstants.TYPE_ATOM ));
    }

    public void setDetailFeed(boolean detailFeed) {
        this.detailFeed = detailFeed;
    }

    public void setSearchFeed(boolean searchFeed) {
        this.searchFeed = searchFeed;
    }

    public boolean isDetailFeed() {
        return detailFeed;
    }

    public boolean isSearchFeed() {
        return searchFeed;
    }

    public int getSize() {
        return getEntries().size();
    }
}
