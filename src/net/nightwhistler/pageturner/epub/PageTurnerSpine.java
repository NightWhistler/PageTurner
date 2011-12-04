/*
 * Copyright (C) 2011 Alex Kuiper
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.nightwhistler.pageturner.epub;

import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;

/**
 * Special spine class which handles navigation
 * and provides a custom cover.
 * 
 * @author Alex Kuiper
 *
 */
public class PageTurnerSpine {

	private List<SpineEntry> entries;
	
	private int position;
	
	public static final String COVER_HREF = "PageTurnerCover";
	
	/** How long should a cover page be to be included **/
	private static final int COVER_PAGE_THRESHOLD = 1024;
	
	/**
	 * Creates a new Spine from this book.
	 * 
	 * @param book
	 */
	public PageTurnerSpine(Book book) {
		this.entries = new ArrayList<PageTurnerSpine.SpineEntry>();
		this.position = 0;
		
		addResource(createCoverResource(book));
		
		String href = null;
	    
	    if ( book.getCoverPage() != null && 
	    		book.getCoverPage().getData().length <= COVER_PAGE_THRESHOLD ) {
	    	
	    	href = book.getCoverPage().getHref();
	    } 
	    
	    for ( int i=0; i < book.getSpine().size(); i++ ) {
	    	Resource res = book.getSpine().getResource(i);	      	
	      	
	      	if ( href == null || ! (href.equals(res.getHref()))) {
	      		addResource(res);
	      	}
	    }	 
	}
	
	/**
	 * Adds a new resource.
	 * @param resource
	 */
	private void addResource( Resource resource ) {
		
		SpineEntry newEntry = new SpineEntry();
		newEntry.title = resource.getTitle();
		newEntry.resource = resource;
		newEntry.href = resource.getHref();
		newEntry.size = resource.getData().length;
		
		entries.add(newEntry);
	}
	
	/**
	 * Returns the number of entries in this spine.
	 * This includes the generated cover.
	 * 
	 * @return
	 */
	public int size() {
		return this.entries.size();
	}
	
	/**
	 * Navigates one entry forward.
	 * 
	 * @return false if we're already at the end.
	 */
	public boolean navigateForward() {
		
		if ( this.position == size() -1 ) {
			return false;
		}
		
		this.position++;
		return true;				
	}
	
	/**
	 * Navigates one entry back.
	 * 
	 * @return false if we're already at the start
	 */
	public boolean navigateBack() {
		if ( this.position == 0 ) {
			return false;
		}
		
		this.position--;
		return true;
	}
	
	/**
	 * Checks if the current entry is the cover page.
	 * 
	 * @return
	 */
	public boolean isCover() {
		return this.position == 0;
	}
	
	/**
	 * Returns the title of the current entry,
	 * or null if it could not be determined.
	 * 
	 * @return
	 */
	public String getCurrentTitle() {
		if ( entries.isEmpty() ) {
			return null;
		}
		
		return entries.get(position).title;
	}
	
	/**
	 * Returns the current resource, or null
	 * if there is none.
	 * 
	 * @return
	 */
	public Resource getCurrentResource() {
		if ( entries.isEmpty() ) {
			return null;
		}
		
		return entries.get(position).resource;
	}
	
	/**
	 * Returns the href of the current resource.
	 * @return
	 */
	public String getCurrentHref() {
		if ( entries.isEmpty() ) {
			return null;
		}
		
		return entries.get(position).href;
	}
	
	/**
	 * Navigates to a specific point in the spine.
	 * 
	 * @param index
	 * @return false if the point did not exist.
	 */
	public boolean navigateByIndex( int index ) {
		if ( index < 0 || index >= size() ) {
			return false;
		}
		
		this.position = index;
		return true;
	}
	
	/**
	 * Returns the current position in the spine.
	 * 
	 * @return
	 */
	public int getPosition() {
		return position;
	}
	
	/**
	 * Navigates to the point with the given href.
	 * 
	 * @param href
	 * @return false if that point did not exist.
	 */
	public boolean navigateByHref( String href ) {
		
		for ( int i=0; i < size(); i++ ) {
			if ( entries.get(i).href.equals(href) ) {
				this.position = i;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns a percentage, which indicates how
	 * far the given point in the current entry is
	 * compared to the whole book.
	 * 
	 * @param progressInPart
	 * @return
	 */
	public int getProgressPercentage(int progressInPart) {		
		
		if ( this.entries == null ) {
			return -1;
		}
		
		int total = 0;
		int uptoHere = 0;
		
		for ( int i=0; i < entries.size(); i++ ) {
			
			if ( i < this.position ) {
				uptoHere += entries.get(i).size;
			}
			
			total += entries.get(i).size;
		}
		
		double pastParts = (double) uptoHere / (double) total; 
		
		int pos = progressInPart;
		int totalLength = entries.get(this.position).size;
		
		double thisPart = (double) entries.get(this.position).size / (double) total;
		
		double inThisPart = (double) pos / (double) totalLength;
		
		double progress = pastParts + (inThisPart * thisPart);
		
		return (int) (progress * 100);		
	}
	
	private Resource createCoverResource(Book book) {		
		Resource res = new Resource(generateCoverPage(book).getBytes(), COVER_HREF);
		res.setTitle("Cover");
		
		return res;
	}
	
	private String generateCoverPage(Book book) {
		
		String centerpiece;
		
		//Else we construct a basic front page with title and author.
		if ( book.getCoverImage() == null ) {												
			centerpiece = "<h1>" + book.getTitle() != null ? book.getTitle(): "Book without a title" + "</h1>";
			
			if ( ! book.getMetadata().getAuthors().isEmpty() ) {						
				for ( Author author: book.getMetadata().getAuthors() ) {							
					centerpiece += "<h3>" + author.getFirstname() + " " + author.getLastname() + "</h3>";
				}
			} else {
				centerpiece += "<h3>Unknown author</h3>";
			}			
		} else {
			//If the book has a cover image, we display that
			centerpiece = "<img src='" + book.getCoverImage().getHref() + "'>";
		}		
		
		return "<html><body><center>" + centerpiece + "</center></body></html>";
	}
	
	private class SpineEntry {
		
		private String title;
		private Resource resource;		
		private String href;
		
		private int size;
		
	}

}
