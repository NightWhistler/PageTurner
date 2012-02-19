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
package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import android.graphics.Canvas;
import android.text.Layout.Alignment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;


public class SinglePageStrategy implements PageChangeStrategy {
	
	private Spanned text = new SpannableString("");	
	private int storedPosition = 0;
	
	private BookView bookView;
	private TextView childView;
	
	//FIXME: This should really be dynamically calculated based on screen size.
	private static final int MAX_PAGE_SIZE = 5000;
	
	public SinglePageStrategy(BookView bookView) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();			
	}

	@Override
	public int getPosition() {
		//Hack
		if ( this.storedPosition == Integer.MAX_VALUE ) {
			return 0;
		}
		
		return this.storedPosition;
	}
	
	@Override
	public void loadText(Spanned text) {
		this.text = text;	
		updatePosition();
	}
	
	@Override
	public boolean isAtEnd() {
		return getPosition() + childView.getText().length() >= this.text.length();
	}
	
	@Override
	public boolean isAtStart() {
		return getPosition() == 0;
	}
	
	@Override
	public void pageDown() {			
		
		int oldPos = this.storedPosition;
		int totalLength = this.text.length();
		
		int textEnd = this.storedPosition + 
			this.childView.getText().length();
		
		if ( textEnd >= text.length() -1 ) {
			PageTurnerSpine spine = bookView.getSpine();
			
			if ( spine == null || ! spine.navigateForward() ) {
				return;
			}
			
			this.storedPosition = 0;
			this.childView.setText("");
			this.clearText();
			
			bookView.loadText();
			return;
		}
		
		if ( textEnd == oldPos ) {
			textEnd++;
		}
		
		this.storedPosition = Math.min(textEnd, totalLength -1 ); 
				
		
		updatePosition();
	}

	private int findStartOfPage( int endOfPageOffset ) {
		
		int endOffset = endOfPageOffset;
		
		endOffset = Math.min(this.text.length() -1, endOffset);
		endOffset = Math.max(0, endOffset);
		
		int start = Math.max(0, endOffset - MAX_PAGE_SIZE);
		
		CharSequence cutOff = this.text.subSequence(start, endOffset);		

		TextPaint textPaint = childView.getPaint();
		int boundedWidth = childView.getWidth();
		StaticLayout layout = new StaticLayout(cutOff, textPaint, boundedWidth , 
				Alignment.ALIGN_NORMAL, 1.0f, bookView.getLineSpacing(), false);
		
		layout.draw(new Canvas());	
		
		if ( layout.getHeight() < bookView.getHeight() ) {
			return start;
		} else {
				
			int topLine = layout.getLineForVertical( layout.getHeight() - (bookView.getHeight() - 2 * bookView.getVerticalMargin() ) );
			int offset = layout.getLineStart( topLine +2 );
		
			return start + offset;
		}
	}
	
	@Override
	public Spanned getText() {
		return this.text;
	}
	
	@Override
	public void pageUp() {				
		
		int pageStart = findStartOfPage(this.storedPosition);		
		
		if ( pageStart == this.storedPosition ) {
			if ( bookView.getSpine() == null || ! bookView.getSpine().navigateBack() ) {
				return;
			}
			
			this.childView.setText("");
			this.clearText();
			
			this.storedPosition = Integer.MAX_VALUE;
			this.bookView.loadText();
			return;
		} else {
			this.storedPosition = pageStart;
		}				
		
		updatePosition();
	}
	
	@Override
	public void clearText() {
		this.text = new SpannedString("");		
	}	
	
	@Override
	public void clearStoredPosition() {
		//No-op		
	}
	
	@Override
	public void reset() {
		this.storedPosition = 0;
		this.text = new SpannedString("");
	}
	
	public void updatePosition() {	
		
		if ( this.text.length() == 0 ) {
			return;
		}
		
		if ( this.storedPosition >= text.length() ) {
			this.storedPosition = findStartOfPage( text.length() -1 );
		}
		
		this.storedPosition = Math.max(0, this.storedPosition);
		this.storedPosition = Math.min(this.text.length() -1, this.storedPosition);				
		
		int totalLength = this.text.length();
		int end = Math.min( storedPosition + MAX_PAGE_SIZE, totalLength);
		
		CharSequence cutOff = this.text.subSequence(storedPosition, end ); 
	
		TextPaint textPaint = childView.getPaint();
		int boundedWidth = childView.getWidth();

		StaticLayout layout = new StaticLayout(cutOff, textPaint, boundedWidth , Alignment.ALIGN_NORMAL, 1.0f, bookView.getLineSpacing(), false);
		layout.draw(new Canvas());
					
		int bottomLine = layout.getLineForVertical( bookView.getHeight() - ( 2 * bookView.getVerticalMargin()) );
		bottomLine = Math.max( 1, bottomLine );
		
		if ( layout.getHeight() >= bookView.getHeight() && text.length() > 10) {
			
			int offset = layout.getLineStart(bottomLine -1);		
			CharSequence section = cutOff.subSequence(0, offset);	
			
			/*
			 * Special case, happens with big pictures
			 * We increase the length of the text we display until it becomes to big for
			 * the screen, then cut off 1 before that.			
			 */			
			if ( section.length() == 0 ) {
				for ( int i=1; i < cutOff.length(); i++ ) {
					section = cutOff.subSequence(0, i);
					layout = new StaticLayout(section, textPaint, boundedWidth , 
							Alignment.ALIGN_NORMAL, 1.0f, bookView.getLineSpacing(), false);
					if ( layout.getHeight() >= bookView.getHeight() ) {
						section = cutOff.subSequence(0, i-1);
						break;
					}
				}
			}
			
			childView.setText(section);
			
		} else {
			childView.setText(cutOff);
		}
	}
	
	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;
		
		updatePosition();
	}
	
	@Override
	public boolean isScrolling() {		
		return false;
	}
	
}
