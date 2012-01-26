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

import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Translates low-level touch and gesture events into more high-level
 * navigation events.
 * 
 * @author Alex Kuiper
 *
 */
public class NavGestureDetector	extends GestureDetector.SimpleOnGestureListener {

	private static final int SWIPE_MIN_DISTANCE = 90;	
	private static final int SWIPE_THRESHOLD_VELOCITY = 150;
	
	private static final int SCROLL_MIN_DISTANCE = 40;
	
	//Distance to scroll 1 unit on edge slide.
	private static final int SCROLL_FACTOR = 20;
	
	private BookViewListener bookViewListener;
	private BookView bookView;
	private DisplayMetrics metrics;
	
	private boolean newEvent = true;

	public NavGestureDetector( BookView bookView, BookViewListener navListener, 
			DisplayMetrics metrics ) {
		this.bookView = bookView;
		this.bookViewListener = navListener;
		this.metrics = metrics;
	}	
	
	@Override
	public boolean onDown(MotionEvent e) {
		
		this.bookViewListener.onScreenTap();
		
		newEvent = true;
		return false;
	}
	
	

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		
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
    		return bookViewListener.onTopBottomEdge();	
    	}
    	
    	return false;        
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
			float distanceX, float distanceY) {
		
		float minScrollDist = metrics.density * SCROLL_MIN_DISTANCE;
		float scrollUnitSize = SCROLL_FACTOR * metrics.density;
		
		float dist = e1.getY() - e2.getY();
		
		if ( newEvent && Math.abs(dist) < minScrollDist ) {			
			return false;
		}
		
		this.newEvent = false; //Signal we've started measuring an event
		
		final int TAP_RANGE_H = bookView.getWidth() / 5;
		float delta = (e1.getY() - e2.getY()) / scrollUnitSize;			
		int level = (int) delta;
		
		if ( e1.getX() < TAP_RANGE_H ) {
			
			this.bookViewListener.onLeftEdgeSlide(level);
			
			return true;
		} else if ( e1.getX() > bookView.getWidth() - TAP_RANGE_H ) {
			
			this.bookViewListener.onRightEdgeSlide(level);			
			return true;
			
		}
		
		
		return super.onScroll(e1, e2, distanceX, distanceY);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		
		float swipeMinDistance = SWIPE_MIN_DISTANCE * metrics.density;
		float swipeThresholdVelocity = SWIPE_THRESHOLD_VELOCITY * metrics.density;		

		//Check speed first.
		if ( Math.abs(velocityX) < swipeThresholdVelocity && Math.abs(velocityY) < swipeThresholdVelocity ) {
			return false;
		}

		float distanceX = e2.getX() - e1.getX();
		float distanceY = e2.getY() - e1.getY();

		if ( Math.abs(distanceX) < swipeMinDistance && Math.abs(distanceY) < swipeMinDistance ) {
			return false;
		}

		if ( Math.abs(velocityX) > swipeThresholdVelocity && Math.abs(distanceX) > Math.abs(distanceY) ) {

			if ( distanceX > 0 ) {
				bookViewListener.onSwipeRight();
			} else {
				bookViewListener.onSwipeLeft();				
			}

			return true;

		} else if ( Math.abs(velocityY) > swipeThresholdVelocity && Math.abs(distanceY) > Math.abs(distanceX) ) {
			if ( distanceY > 0 ) {
				bookViewListener.onSwipeUp();
			} else {
				bookViewListener.onSwipeDown();
			}

			return true;
		}

		return false;
	}
	
	@Override
    public void onLongPress(MotionEvent e) {
    	CharSequence word = bookView.getWordAt(e.getX(), e.getY() );
    	
    	if ( word != null ) {
    		bookViewListener.onWordLongPressed(word);
    	}
    }     


}
