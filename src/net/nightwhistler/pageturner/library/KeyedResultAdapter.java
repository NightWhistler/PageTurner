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

package net.nightwhistler.pageturner.library;

import android.widget.SectionIndexer;

import java.util.List;

/**
 * Abstract adapter super-class for KeyedQueryResults
 * 
 * @author Alex Kuiper
 *
 */
public abstract class KeyedResultAdapter extends QueryResultAdapter<LibraryBook> implements SectionIndexer {
	
	private KeyedQueryResult<LibraryBook> keyedResult;
	
	@Override
	public void setResult(QueryResult<LibraryBook> result) {
		
		if ( result instanceof KeyedQueryResult) {
			this.keyedResult = (KeyedQueryResult<LibraryBook>) result;	
		} else {
			this.keyedResult = null;
		}
		
		super.setResult(result);
	}		
	
	public boolean isKeyed() {
		return this.keyedResult != null;
	}
	
	public String getKey(int position) {
		List<String> keys = keyedResult.getKeys();
		
		if ( keys == null || position >= keys.size() ) {
			return null;
		}
		
		return keys.get(position);
	}
	
	public List<Character> getAlphabet() {
		return keyedResult.getAlphabet();
	}

	@Override
	public int getPositionForSection(int section) {
		
		if ( section < 0 || keyedResult == null ) {
			return -1;
		}
		
		List<Character> alphabet = this.keyedResult.getAlphabet();
		
		if ( section >= alphabet.size() ) {
			return 0;
		}
		
		Character c = alphabet.get(section);
		
		return this.keyedResult.getOffsetFor(c);
	}

	@Override
	public int getSectionForPosition(int position) {			
		if ( this.keyedResult == null ) {
			return 0;
		}
		
		if ( position < 0 || position >= this.keyedResult.getSize() ) {
			return 0;
		}
		
		Character c = this.keyedResult.getCharacterFor(position);
		
		if ( c == null ) { 
			return 0;
		}
		
		return this.keyedResult.getAlphabet().indexOf(c);
	}

	@Override
	public Object[] getSections() {			
		
		if ( keyedResult == null ) {
			return new Object[0];
		}
		
		List<Character> sectionNames = this.keyedResult.getAlphabet();
		
		return sectionNames.toArray( new Character[ sectionNames.size() ] );
		
	}
}