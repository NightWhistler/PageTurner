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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.View;

import com.google.inject.Inject;

import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.dto.HighLight;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import android.graphics.Canvas;
import android.widget.TextView;
import net.nightwhistler.pageturner.view.HighlightManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FixedPagesStrategy implements PageChangeStrategy {

    private Configuration config;

    private StaticLayoutFactory layoutFactory;

    private HighlightManager highlightManager;

    private static final Logger LOG = LoggerFactory.getLogger("FixedPagesStrategy");

	private Spanned text;
	
	private int pageNum;
	
	private List<Integer> pageOffsets = new ArrayList<Integer>();
	
	private BookView bookView;
	private TextView childView;

	private int storedPosition = -1;

    @Override
    public void setBookView(BookView bookView) {
        this.bookView = bookView;
        this.childView = bookView.getInnerView();
    }

    @Inject
    public void setHighlightManager( HighlightManager highlightManager ) {
        this.highlightManager = highlightManager;
    }

    @Inject
    public void setLayoutFactory(StaticLayoutFactory layoutFactory) {
        this.layoutFactory = layoutFactory;
    }

    @Inject
    public void setConfig( Configuration config ) {
        this.config = config;
    }

    @Override
	public void clearStoredPosition() {
		this.pageNum = 0;
		this.storedPosition = 0;
	}
	
	@Override
	public void clearText() {
		this.text = new SpannableStringBuilder("");
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

        if ( layout == null ) {
            return new ArrayList<Integer>();
        }

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

            LOG.debug( "Processing line " + topLineNextPage + " / " + totalLines );

			int topLine = layout.getLineForOffset(pageStartOffset);				
			topLineNextPage = layout.getLineForVertical( layout.getLineTop( topLine ) + pageHeight);

            LOG.debug( "topLine " + topLine + " / " + topLineNextPage );
			if ( topLineNextPage == topLine ) { //If lines are bigger than can fit on a page
				topLineNextPage = topLine + 1;
			}
						
			int pageEnd = layout.getLineEnd(topLineNextPage -1);

            LOG.debug("pageStartOffset=" + pageStartOffset + ", pageEnd=" + pageEnd );
			
			if (pageEnd > pageStartOffset ) {
                if ( text.subSequence(pageStartOffset, pageEnd).toString().trim().length() > 0) {
                    pageOffsets.add(pageStartOffset);
                }
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


        CharSequence sequence = getTextForPage(this.pageNum);

        // #555 Remove \n at the end of sequence which get InnerView size changed
        int endIndex = sequence.length();
        while (sequence.charAt(endIndex-1) == '\n') {
        	endIndex--;
        }
        
        sequence = sequence.subSequence(0, endIndex);
        
        try {
		    this.childView.setText( sequence );

            //If we get an error setting the formatted text,
            //strip formatting and try again.
        } catch ( ArrayIndexOutOfBoundsException a ) {
            this.childView.setText( sequence );
        } catch ( IndexOutOfBoundsException ie ) {
            this.childView.setText( sequence );
        }
	}
	
	private CharSequence getTextForPage( int page ) {
		
		if ( pageOffsets.size() < 1 || page < 0 ) {
			return null;
		} else if ( page >= pageOffsets.size() -1 ) {
            int startOffset = pageOffsets.get(pageOffsets.size() -1);

            if ( startOffset >= 0 && startOffset <= text.length() -1 ) {
			    return applySpans( this.text.subSequence(startOffset, text.length() ), startOffset );
            } else {
                return applySpans(text, 0);
            }
		} else {
			int start = this.pageOffsets.get(page);
			int end = this.pageOffsets.get(page +1 );
			return applySpans( this.text.subSequence(start, end), start );
		}	
	}

    private CharSequence applySpans(CharSequence text, int offset) {
        List<HighLight> highLights = highlightManager.getHighLights( bookView.getFileName() );
        int end = offset + text.length() -1;

        for ( final HighLight highLight: highLights ) {
            if ( highLight.getIndex() == bookView.getIndex() &&
                    highLight.getStart() >= offset && highLight.getStart() < end ) {

                LOG.debug("Got highlight from " + highLight.getStart() + " to " + highLight.getEnd() + " with offset " + offset );

                int highLightEnd = Math.min(end, highLight.getEnd() );

                ( (Spannable) text).setSpan(new HighlightSpan(highLight),
                        highLight.getStart() - offset, highLightEnd - offset,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            }
        }

        return text;
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
