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
package net.nightwhistler.pageturner.animation;

import android.graphics.*;

/**
 * Rolling-blind autoscroll Animator.
 * 
 * Slowly unveils the new page on top of the old one.
 * 
 * @author Alex Kuiper
 *
 */
public class RollingBlindAnimator implements Animator {

	private Bitmap backgroundBitmap;
	private Bitmap foregroundBitmap;
	
	private int count;	
		
	private int animationSpeed;
	
	private static final int MAX_STEPS = 500;
		
	@Override
	public void advanceOneFrame() {
		count++;		
	}
	
	@Override
	public void draw(Canvas canvas) {
		if ( backgroundBitmap != null && foregroundBitmap != null ) {
			
			float percentage = (float) count / (float) MAX_STEPS;
			
			int pixelsToDraw = (int) (backgroundBitmap.getHeight() * percentage); 	
			
			Rect top = new Rect( 0, 0, backgroundBitmap.getWidth(), pixelsToDraw );
						
			canvas.drawBitmap(foregroundBitmap, top, top, null);
			
			Rect bottom = new Rect( 0, pixelsToDraw, backgroundBitmap.getWidth(), backgroundBitmap.getHeight() );
			
			canvas.drawBitmap(backgroundBitmap, bottom, bottom, null);
			
			Paint paint = new Paint();
			paint.setColor(Color.GRAY);
			paint.setStyle(Paint.Style.STROKE);
			
			canvas.drawLine(0, pixelsToDraw, backgroundBitmap.getWidth(), pixelsToDraw, paint);
		}		
	}
	
	@Override
	public boolean isFinished() {
		return backgroundBitmap == null
			|| count >= MAX_STEPS; 
	}
	
	@Override
	public void stop() {
		this.backgroundBitmap = null;		
	}
	
	@Override
	public int getAnimationSpeed() {
		return this.animationSpeed;
	}	
	
	public void setBackgroundBitmap(Bitmap backgroundBitmap) {
		this.backgroundBitmap = backgroundBitmap;
	}
	
	public void setForegroundBitmap(Bitmap foregroundBitmap) {
		this.foregroundBitmap = foregroundBitmap;
	}	
	
	public void setAnimationSpeed(int animationSpeed) {
		this.animationSpeed = animationSpeed;
	}
	
}
