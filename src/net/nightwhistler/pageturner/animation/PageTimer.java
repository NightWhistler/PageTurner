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
import android.graphics.Paint.Style;

public class PageTimer implements Animator {
	
	private Bitmap backgroundImage;
	private int bottom;
	
	private int speed = 20;
	
	private static final int MAX_STEP = 500;

	int count = 0;
	
	public PageTimer(Bitmap backgroundImage, int bottom) {
		this.backgroundImage = backgroundImage;
		this.bottom = bottom;
	}
	
	@Override
	public void advanceOneFrame() {
		count += 1;		
	}
	
	@Override
	public void draw(Canvas canvas) {
		Paint paint = new Paint();
		
		Rect fullScreen = new Rect(0,0, backgroundImage.getWidth(), backgroundImage.getHeight() );		
		canvas.drawBitmap(backgroundImage, null, fullScreen, paint);
		
		paint.setColor(Color.GRAY);
		paint.setStyle(Style.STROKE);
		
		int timerHeight = 10;
		Rect timer = new Rect( 0, backgroundImage.getHeight() - (timerHeight + bottom), 
				backgroundImage.getWidth(), backgroundImage.getHeight() - bottom );
		canvas.drawRect(timer, paint);
		
		float percentage = (float) count / (float) MAX_STEP;
		int width = (int) (backgroundImage.getWidth() * percentage);
		
		timer.right = width;
		paint.setStyle(Style.FILL);
		canvas.drawRect(timer, paint);
		
	}
	
	@Override
	public void stop() {
		count = MAX_STEP + 1;		
	}
	
	@Override
	public int getAnimationSpeed() {
		return speed;
	}
	
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	@Override
	public boolean isFinished() {
		return count >= MAX_STEP;
	}
	
	
}
