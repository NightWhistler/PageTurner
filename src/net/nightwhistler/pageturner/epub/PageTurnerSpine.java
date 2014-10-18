/*
 * Copyright (C) 2011 Alex Kuiper
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
package net.nightwhistler.pageturner.epub;

import android.util.Log;
import jedi.option.Option;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static jedi.functional.FunctionalPrimitives.isEmpty;
import static jedi.option.Options.none;
import static jedi.option.Options.option;
import static jedi.option.Options.some;

/**
 * Special spine class which handles navigation
 * and provides a custom cover.
 * 
 * @author Alex Kuiper
 *
 */
public class PageTurnerSpine implements Iterable<PageTurnerSpine.SpineEntry> {

	private List<SpineEntry> entries;
	
	private List<List<Integer>> pageOffsets = new ArrayList<>();
	
	private int position;
	
	public static final String COVER_HREF = "PageTurnerCover";
	
	/** How long should a cover page be to be included **/
	private static final int COVER_PAGE_THRESHOLD = 1024;
	
	private String tocHref;
	
	/**
	 * Creates a new Spine from this book.
	 * 
	 * @param book
	 */
	public PageTurnerSpine(Book book) {
		this.entries = new ArrayList<>();
		this.position = 0;
		
		addResource(createCoverResource(book));
		
		String href = null;
	    
	    if ( entries.size() > 0 && ! entries.get(0).href.equals( COVER_HREF ) ) {
	    	href = book.getCoverPage().getHref();
	    } 
	    
	    for ( int i=0; i < book.getSpine().size(); i++ ) {
	    	Resource res = book.getSpine().getResource(i);	      	
	      	
	      	if ( href == null || ! (href.equals(res.getHref()))) {
	      		addResource(res);
	      	}
	    }
	    
	    if ( book.getNcxResource() != null ) {
	    	this.tocHref = book.getNcxResource().getHref();
	    }
	}
	
	public void setPageOffsets(List<List<Integer>> pageOffsets) {
        if ( pageOffsets != null ) {
		    this.pageOffsets = pageOffsets;
        } else {
            this.pageOffsets = new ArrayList<>();
        }
	}
	
	public int getTotalNumberOfPages() {
		int total = 0;
		for ( List<Integer> pagesPerSection: pageOffsets ) {
			total += pagesPerSection.size();
		}
		
		return Math.max(0, total - 1);
	}

    @Override
    public Iterator<SpineEntry> iterator() {
        return this.entries.iterator();
    }

    public List<List<Integer>> getPageOffsets() {
		return pageOffsets;
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
		newEntry.size = (int) resource.getSize();
		
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
	public Option<String> getCurrentTitle() {
        if ( entries.size() > 0 ) {
            return option(entries.get(position).title);
        } else {
            return none();
        }
	}
	
	/**
	 * Returns the current resource, or null
	 * if there is none.
	 * 
	 * @return
	 */
	public Option<Resource> getCurrentResource() {
		return getResourceForIndex(position);
	}

    /**
     * Returns the resource after the current one
     * @return
     */
    public Option<Resource> getNextResource() {
        return getResourceForIndex(position + 1);
    }
	
	public Option<Resource> getResourceForIndex( int index ) {
		if ( entries.isEmpty() || index < 0 || index >= entries.size() ) {
			return none();
		}
		
		return option(entries.get(index).resource);
	}
	
	/**
	 * Resolves a href relative to the current resource.
	 * 
	 * @param href
	 * @return
	 */
	public String resolveHref( String href ) {		
		
		Option<Resource> res = getCurrentResource();

        if (! isEmpty(res) ) {
            Resource actualResource = res.unsafeGet();

            if ( actualResource.getHref() != null ) {
                return resolveHref(href, actualResource.getHref());
            }
        }

        return href;
	}
	
	/**
	 * Resolves a HREF relative to the Table of Contents
	 * 
	 * @param href
	 * @return
	 */
	public String resolveTocHref( String href ) {
		if ( this.tocHref != null ) {
			return resolveHref(href, tocHref);
		}
		
		return href;
	}
	
	private static String resolveHref( String href, String against ) {
		try {
			String result = new URI(encode(against)).resolve(encode(href)).getPath();
			return result;
		} 
		catch (URISyntaxException u) {
			return href;
		} catch (IllegalArgumentException i) {
            return href;
        }
	}
	
	private static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if ( ch == '\\' ) { //Some books use \ as a separator... invalid, but we'll try to fix it
                resultStr.append('/');
            } else if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    /**
     * This is slightly unsafe: it lets / and % pass, making 
     * multiple encodes safe.
     * @param ch
     * @return
     */
    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,:;=?@<>#[]".indexOf(ch) >= 0;
    }

	
	/**
	 * Returns the href of the current resource.
	 * @return
	 */
	public Option<String> getCurrentHref() {
		if ( entries.size() > 0 ) {
            return option(entries.get(position).href);
		} else {
            return none();
        }
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
		
		String encodedHref = encode(href);
		
		for ( int i=0; i < size(); i++ ) {
			String entryHref = encode(entries.get(i).href);
			if ( entryHref.equals(encodedHref)){
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
	public int getProgressPercentage(double progressInPart) {		
		return getProgressPercentage(getPosition(), progressInPart);				
	}
	
	private int getProgressPercentage(int index, double progressInPart) {
		
		if ( this.entries == null ) {
			return -1;
		}
		
		double uptoHere = 0;
		
		List<Double> percentages = getRelativeSizes();
		
		for ( int i=0; i < percentages.size() && i < index; i++ ) {
			uptoHere += percentages.get( i );
		}  
		
		double thisPart = percentages.get(index);
		
		double progress = uptoHere + (progressInPart * thisPart);
		
		return (int) (progress * 100);	
	}
	
	/**
	 * Returns the progress percentage for the given text position 
	 * in the given index.
	 * 
	 * @param index
	 * @param position
	 * @return
	 */
	public int getProgressPercentage(int index, int position) {
		if ( this.entries == null || index >= entries.size() ) {
			return -1;
		}
		
		double progressInPart = ( (double)position / (double) entries.get(index).size);
		return getProgressPercentage(index, progressInPart);		
	}
	
	
	
	/**
	 * Returns a list of doubles representing the relative size of each spine index.
	 * @return
	 */
	public List<Double> getRelativeSizes() {
		int total = 0;
		List<Integer> sizes = new ArrayList<>();
		
		for ( int i=0; i < entries.size(); i++ ) {
			int size = entries.get(i).size;
			sizes.add(size);
			total += size;
		}
		
		List<Double> result = new ArrayList<>();
		for ( int i=0; i < sizes.size(); i++ ) {
			double part = (double) sizes.get(i) / (double) total;
			result.add( part );
		}
		
		return result;
	}
	
	private Resource createCoverResource(Book book) {	
		
		if ( book.getCoverPage() != null && book.getCoverPage().getSize() > 0
                && book.getCoverPage().getSize() < COVER_PAGE_THRESHOLD ) {

            Log.d("PageTurnerSpine", "Using cover resource " + book.getCoverPage().getHref() );

			return book.getCoverPage();
		}

        Log.d("PageTurnerSpine", "Constructing a cover page" );
		Resource res = new Resource(generateCoverPage(book).getBytes(), COVER_HREF);
		res.setTitle("Cover");
		
		return res;
	}
	
	private String generateCoverPage(Book book) {
		
		String centerpiece;
		
		//Else we construct a basic front page with title and author.
		if ( book.getCoverImage() == null ) {												
			centerpiece = "<center><h1>" + (book.getTitle() != null ? book.getTitle(): "Book without a title") + "</h1>";
			
			if ( ! book.getMetadata().getAuthors().isEmpty() ) {						
				for ( Author author: book.getMetadata().getAuthors() ) {							
					centerpiece += "<h3>" + author.getFirstname() + " " + author.getLastname() + "</h3>";
				}
			} else {
				centerpiece += "<h3>Unknown author</h3>";
			}

            centerpiece += "</center>";

		} else {
			//If the book has a cover image, we display that
			centerpiece = "<img src='" + book.getCoverImage().getHref() + "'>";
		}		
		
		return "<html><body>" + centerpiece + "</body></html>";
	}
	
	public class SpineEntry {
		
		private String title;
		private Resource resource;		
		private String href;
		
		private int size;

        public String getTitle() {
            return title;
        }

        public int getSize() {
            return size;
        }

        public Resource getResource() {
            return resource;
        }

        public String getHref() {
            return href;
        }
    }

}
