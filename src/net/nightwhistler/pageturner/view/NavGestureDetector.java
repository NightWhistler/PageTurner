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

package net.nightwhistler.pageturner.view;

import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import net.nightwhistler.pageturner.view.bookview.BookView;
import net.nightwhistler.pageturner.view.bookview.BookViewListener;

/**
 * Translates low-level touch and gesture events into more high-level
 * navigation events.
 * 
 * @author Alex Kuiper
 *
 */
public class NavGestureDetector	extends GestureDetector.SimpleOnGestureListener {

	
	//Distance to scroll 1 unit on edge slide.
	private static final int SCROLL_FACTOR = 50;
	
	private BookViewListener bookViewListener;
	private BookView bookView;
	private DisplayMetrics metrics;
		
	public NavGestureDetector( BookView bookView, BookViewListener navListener, 
			DisplayMetrics metrics ) {
		this.bookView = bookView;
		this.bookViewListener = navListener;
		this.metrics = metrics;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		
		//Links get preference
		ClickableSpan[] spans = bookView.getLinkAt(e.getX(), e.getY() );
		if ( spans != null && spans.length > 0 ) {
			
			for ( ClickableSpan span: spans ) {
				span.onClick(bookView);
			}
			
			return true;
		}
		
    	final int TAP_RANGE_H = bookView.getWidth() / 5;
    	final int TAP_RANGE_V = bookView.getHeight() / 5;
    	
    	if ( e.getX() < TAP_RANGE_H ) {
    		return bookViewListener.onTapLeftEdge();
    	} else if (e.getX() > bookView.getWidth() - TAP_RANGE_H ) {
    		return bookViewListener.onTapRightEdge();
    	}
    	    	
    	int yBase = bookView.getScrollY();        	
    	
    	if ( e.getY() < TAP_RANGE_V + yBase ) {
    		return bookViewListener.onTapTopEdge();
    	} else if ( e.getY() > (yBase + bookView.getHeight()) - TAP_RANGE_V ) {
    		return bookViewListener.onTapBottomEdge();	
    	}
    	
    	this.bookViewListener.onScreenTap();	
    	return false;    	        
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
			float distanceX, float distanceY) {
		
		float scrollUnitSize = SCROLL_FACTOR * metrics.density;			
		
		final int TAP_RANGE_H = bookView.getWidth() / 5;
		float delta = (e1.getY() - e2.getY()) / scrollUnitSize;			
		int level = (int) delta;
		
		if ( e1.getX() < TAP_RANGE_H ) {			
			return this.bookViewListener.onLeftEdgeSlide(level);
		} else if ( e1.getX() > bookView.getWidth() - TAP_RANGE_H ) {
			
			return this.bookViewListener.onRightEdgeSlide(level);
		}		
		
		return super.onScroll(e1, e2, distanceX, distanceY);
	}
	

	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {	
				
		float distanceX = e2.getX() - e1.getX();
		float distanceY = e2.getY() - e1.getY();
	
		if (  Math.abs(distanceX) > Math.abs(distanceY) ) {

			if ( distanceX > 0 ) {
				return bookViewListener.onSwipeRight();
			} else {
				return bookViewListener.onSwipeLeft();				
			}			

		} else if (Math.abs(distanceY) > Math.abs(distanceX) ) {
			if ( distanceY > 0 ) {
				return bookViewListener.onSwipeUp();
			} else {
				return bookViewListener.onSwipeDown();
			}			
		}

		return false;
	}
	

	@Override
	public void onLongPress(MotionEvent e) {

		CharSequence word = bookView.getWordAt(e.getX(), e.getY() );

		if ( word != null ) {
			bookViewListener.onWordLongPressed(word);
		}

		super.onLongPress(e);
	}
      


}
