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

import android.database.Cursor;

import java.util.*;

/**
 * Special QueryResult which buffers the keys,
 * allowing for direct access.
 * 
 * @author Alex Kuiper
 *
 * @param <T>
 */
public abstract class KeyedQueryResult<T> extends QueryResult<T> {

	private List<String> keys;	
	
	private List<Character> alphabet;
	private String alphabetString;
	
	public KeyedQueryResult(Cursor cursor, List<String> keys ) {
		super(cursor);		
		this.keys = keys;	
		
		this.alphabet = calculateAlphaBet();
		this.alphabetString = calculateAlphabetString();
	}
	
	public List<String> getKeys() {
		return keys;
	}
	
	private List<Character> calculateAlphaBet() {
		SortedSet<Character> result = new TreeSet<Character>();
		
		for ( String key: keys ) {
			if ( key.length() > 0 ) {
				result.add( Character.toUpperCase(key.charAt(0)) );
			}
		}
		
		return Collections.unmodifiableList( new ArrayList<Character>(result) );
	}
	
	private String calculateAlphabetString() {
		StringBuffer buff = new StringBuffer();
		for ( Character c: getAlphabet() ) {
			buff.append(c);
		}
		
		return buff.toString();
	}	
	
	public String getAlphabetString() {
		return alphabetString;
	}
	
	public List<Character> getAlphabet() {
		return this.alphabet;
	}
	
	public Character getCharacterFor( int position ) {
		String key = keys.get(position);
		
		if ( key.length() > 0 ) {
			return Character.toUpperCase(key.charAt(0));
		} else {
			return null;
		}
	}
	
	public int getOffsetFor( Character c ) {
		
		Character input = Character.toUpperCase(c);
		
		for ( int i=0; i < keys.size(); i++ ) {
			String key = keys.get(i);
			if ( key.length() > 0 ) {
				Character keyStart = Character.toUpperCase(key.charAt(0));
				if ( keyStart.compareTo(input) >= 0 ) {
					return i;
				}
			}			
		}
		
		return -1;
	}
	
	public T getFirstItemFor( Character c ) {
		int i = getOffsetFor(c);
		
		if ( i == -1 ) {
			return null;
		} else {
			return getItemAt(i);
		}
	}
	
	

}
