/*
 * Copyright (C) 2013 Alex Kuiper
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

package net.nightwhistler.pageturner.view.bookview;

import android.graphics.Canvas;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FixedPagesStrategy implements PageChangeStrategy {

	private Spanned text;
	
	private int pageNum;
	
	private List<Integer> pageOffsets = new ArrayList<Integer>();
	
	private BookView bookView;
	private TextView childView;
	
	private Configuration config;
	
	private int storedPosition = -1;

    private static final Logger LOG = LoggerFactory.getLogger("FixedPagesStrategy");

	private StaticLayoutFactory layoutFactory;
		
	public FixedPagesStrategy(BookView bookView, Configuration config, StaticLayoutFactory layoutFactory) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();
		this.config = config;
		this.layoutFactory = layoutFactory;
	}
	
	@Override
	public void clearStoredPosition() {
		this.pageNum = 0;
		this.storedPosition = 0;
	}
	
	@Override
	public void clearText() {
		this.text = new SpannedString("");
		this.childView.setText(text);
		this.pageOffsets = new ArrayList<Integer>();
	}
	
	/**
	 * Returns the current page INSIDE THE SECTION.
	 * 
	 * @return
	 */
	public int getCurrentPage() {
		return this.pageNum;
	}

    public List<Integer> getPageOffsets() {
        return new ArrayList<Integer>(this.pageOffsets);
    }

	public List<Integer> getPageOffsets(CharSequence text, boolean includePageNumbers ) {
		
		if ( text == null ) {
			return new ArrayList<Integer>();
		}
		
		List<Integer> pageOffsets = new ArrayList<Integer>();
		
		TextPaint textPaint = bookView.getInnerView().getPaint();
		int boundedWidth = bookView.getInnerView().getWidth();

        LOG.debug( "Page width: " + boundedWidth );

		StaticLayout layout = layoutFactory.create(text, textPaint, boundedWidth, bookView.getLineSpacing() );
        LOG.debug( "Layout height: " + layout.getHeight() );
		
		layout.draw(new Canvas());

        //Subtract the height of the top margin
		int pageHeight = bookView.getHeight() - bookView.getVerticalMargin();

		if ( includePageNumbers ) {
			String bottomSpace = "0\n";
		
			StaticLayout numLayout = layoutFactory.create(bottomSpace, textPaint, boundedWidth , bookView.getLineSpacing());		
			numLayout.draw(new Canvas());
			
			//Subtract the height needed to show page numbers, or the
            //height of the margin, whichever is more
			pageHeight = pageHeight - Math.max(numLayout.getHeight(), bookView.getVerticalMargin());
		} else {
            //Just subtract the bottom margin
            pageHeight = pageHeight - bookView.getVerticalMargin();
        }

        LOG.debug("Got pageHeight " + pageHeight );

		int totalLines = layout.getLineCount();				
		int topLineNextPage = -1;
		int pageStartOffset = 0;
		
		while ( topLineNextPage < totalLines -1 ) {	
			
			int topLine = layout.getLineForOffset(pageStartOffset);				
			topLineNextPage = layout.getLineForVertical( layout.getLineTop( topLine ) + pageHeight);
			
			if ( topLineNextPage == topLine ) { //If lines are bigger than can fit on a page
				topLineNextPage = topLine + 1;
			}
						
			int pageEnd = layout.getLineEnd(topLineNextPage -1);
			
			if (pageEnd > pageStartOffset && text.subSequence(pageStartOffset, pageEnd).toString().trim().length() > 0 ) {
                pageOffsets.add(pageStartOffset);
				pageStartOffset = layout.getLineStart(topLineNextPage);
			}
		}	
		
		return pageOffsets;		
	}
	
	
	@Override
	public void reset() {
		clearStoredPosition();
		this.pageOffsets.clear();
		clearText();
	}
	
	private void updatePageNumber() {
		for ( int i=0; i < this.pageOffsets.size(); i++ ) {
			if ( this.pageOffsets.get(i) > this.storedPosition ) {
				this.pageNum = i -1;
				return;
			}
		}

		this.pageNum = this.pageOffsets.size() - 1;
	}
	
	@Override
	public void updatePosition() {
		
		if ( pageOffsets.isEmpty() || text.length() == 0 || this.pageNum == -1) {
			return;
		}
		
		if ( storedPosition != -1 ) {
			updatePageNumber();
		}
		
		this.childView.setText(getTextForPage(this.pageNum));			
	}
	
	private CharSequence getTextForPage( int page ) {
		
		if ( pageOffsets.size() < 1 ) {
			return null;
		} else if ( page >= pageOffsets.size() -1 ) {
            int startOffset = pageOffsets.get(pageOffsets.size() -1);

            if ( startOffset >= 0 && startOffset <= text.length() -1 ) {
			    return this.text.subSequence(startOffset, text.length() );
            } else {
                return text;
            }
		} else {
			int start = this.pageOffsets.get(page);
			int end = this.pageOffsets.get(page +1 );
			return this.text.subSequence(start, end);
		}	
	}
	
	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;		
	}
	
	@Override
	public void setRelativePosition(double position) {
		
		int intPosition = (int) (this.text.length() * position);
		setPosition(intPosition);
		
	}

    public int getTopLeftPosition() {

        if ( pageOffsets.isEmpty() ) {
            return 0;
        }

        if ( this.pageNum >= this.pageOffsets.size() ) {
            return this.pageOffsets.get( this.pageOffsets.size() -1 );
        }

        return this.pageOffsets.get(this.pageNum);
    }
	
	public int getProgressPosition() {

        if ( storedPosition > 0 || this.pageOffsets.isEmpty() ||  this.pageNum == -1 ) {
            return this.storedPosition;
        }

		return getTopLeftPosition();
	}
	
	public android.text.Spanned getText() {
		return text;
	}
	
	public boolean isAtEnd() {
		return pageNum == this.pageOffsets.size() - 1;
	}
	
	public boolean isAtStart() {
		return this.pageNum == 0;
	}
	
	public boolean isScrolling() {
		return false;
	}
	
	@Override
	public CharSequence getNextPageText() {
		if ( isAtEnd() ) {
			return null;
		}
		
		return getTextForPage( this.pageNum + 1);
	}
	
	@Override
	public CharSequence getPreviousPageText() {
		if ( isAtStart() ) {
			return null;
		}
		
		return getTextForPage( this.pageNum - 1);
	}
	
	@Override
	public void pageDown() {

        this.storedPosition = -1;

		if ( isAtEnd() ) {
			PageTurnerSpine spine = bookView.getSpine();
		
			if ( spine == null || ! spine.navigateForward() ) {
				return;
			}
			
			this.clearText();
			this.pageNum = 0;
			bookView.loadText();
			
		} else {
			this.pageNum = Math.min(pageNum +1, this.pageOffsets.size() -1 );
			updatePosition();
		}
	}
	
	@Override
	public void pageUp() {

        this.storedPosition = -1;
	
		if ( isAtStart() ) {
			PageTurnerSpine spine = bookView.getSpine();
		
			if ( spine == null || ! spine.navigateBack() ) {
				return;
			}
			
			this.clearText();
			this.storedPosition = Integer.MAX_VALUE;
			this.bookView.loadText();
		} else {
			this.pageNum = Math.max(pageNum -1, 0);
			updatePosition();
		}
	}
	
	@Override
	public void loadText(Spanned text) {
		this.text = text;
		this.pageNum = 0;
		this.pageOffsets = getPageOffsets(text, config.isShowPageNumbers() );
	}

    @Override
    public void updateGUI() {
        updatePosition();   
	}
    
}
