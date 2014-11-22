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
import jedi.functional.Filter;
import jedi.functional.FunctionalPrimitives;
import jedi.option.Option;

import java.util.*;

import static java.lang.Character.toUpperCase;
import static java.util.Collections.unmodifiableList;
import static jedi.functional.Comparables.sort;
import static jedi.functional.FunctionalPrimitives.collect;
import static jedi.functional.FunctionalPrimitives.map;
import static jedi.functional.FunctionalPrimitives.select;
import static jedi.option.Options.none;
import static jedi.option.Options.some;

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

	public KeyedQueryResult(Cursor cursor, List<String> keys ) {
		super(cursor);		
		this.keys = keys;	
		
		this.alphabet = calculateAlphaBet();
	}
	
	public List<String> getKeys() {
		return keys;
	}
	
	private List<Character> calculateAlphaBet() {

        SortedSet<Character> firstLetters = new TreeSet<>(
                collect(
						select(keys, k -> k.length() > 0),
						key -> key.charAt(0)
				)
		);

        return unmodifiableList( new ArrayList<>(firstLetters) );
	}
	
	public List<Character> getAlphabet() {
		return this.alphabet;
	}
	
	public Option<Character> getCharacterFor( int position ) {
		String key = keys.get(position);
		
		if ( key.length() > 0 ) {
			return some(toUpperCase(key.charAt(0)));
		} else {
			return none();
		}
	}
	
	public Option<Integer> getOffsetFor( Character c ) {
		
		Character input = toUpperCase(c);
		
		for ( int i=0; i < keys.size(); i++ ) {
			String key = keys.get(i);
			if ( key.length() > 0 ) {
				Character keyStart = toUpperCase(key.charAt(0));
				if ( keyStart.compareTo(input) >= 0 ) {
					return some(i);
				}
			}			
		}
		
		return none();
	}

}
