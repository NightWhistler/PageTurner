package net.nightwhistler.pageturner.library;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import android.database.Cursor;

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
	
	public KeyedQueryResult(Cursor cursor, List<String> keys ) {
		super(cursor);
		this.keys = keys;		
	}
	
	public SortedSet<Character> getAlphabet() {
		SortedSet<Character> result = new TreeSet<Character>();
		
		for ( String key: keys ) {
			if ( key.length() > 0 ) {
				result.add( key.charAt(0) );
			}
		}
		
		return result;
	}
	
	public int getOffsetFor( Character c ) {
		for ( int i=0; i < keys.size(); i++ ) {
			String key = keys.get(i);
			if ( key.length() > 0 ) {
				Character keyStart = key.charAt(0);
				if ( keyStart.compareTo(c) >= 0 ) {
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
