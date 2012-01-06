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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

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
	private Bitmap foregroudBitmap;
	
	private int count;
	
	private int stepSize = 1;
	
	private int animationSpeed;
		
	@Override
	public void advanceOneFrame() {
		count++;		
	}
	
	@Override
	public void draw(Canvas canvas) {
		if ( backgroundBitmap != null ) {
			
			int pixelsToDraw = count * stepSize;		
			
			Rect top = new Rect( 0, 0, backgroundBitmap.getWidth(), pixelsToDraw );
						
			canvas.drawBitmap(foregroudBitmap, top, top, null);
			
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
			|| (count * stepSize) >= backgroundBitmap.getHeight(); 
	}
	
	@Override
	public int getAnimationSpeed() {
		return this.animationSpeed;
	}	
	
	public void setBackgroundBitmap(Bitmap backgroundBitmap) {
		this.backgroundBitmap = backgroundBitmap;
	}
	
	public void setForegroudBitmap(Bitmap foregroudBitmap) {
		this.foregroudBitmap = foregroudBitmap;
	}
	
	public void setStepSize(int stepSize) {
		this.stepSize = stepSize;
	}
	
	public void setAnimationSpeed(int animationSpeed) {
		this.animationSpeed = animationSpeed;
	}
	
}
