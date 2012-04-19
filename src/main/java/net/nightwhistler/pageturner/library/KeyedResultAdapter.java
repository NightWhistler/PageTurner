package net.nightwhistler.pageturner.library;

import java.util.List;

import android.widget.SectionIndexer;

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
		
		if ( keyedResult == null ) {
			return 0;
		}
		
		Character c = this.keyedResult.getAlphabet().get(section);
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