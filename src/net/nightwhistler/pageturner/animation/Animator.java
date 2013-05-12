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
package net.nightwhistler.pageturner.animation;

import android.graphics.Canvas;

/**
 * Interface for animated transitions for a textview.
 * 
 * @author Alex Kuiper
 *
 */
public interface Animator {

	/**
	 * Returns the speed of this animation in FPS.
	 * 
	 * @return
	 */
	int getAnimationSpeed();
	
	/**
	 * Advances the animation by 1 frame.
	 */
	void advanceOneFrame();
	
	
	/**
	 * Draw an animation frame on the given Canvas.
	 * 
	 * @param canvas
	 */
	void draw( Canvas canvas );
	
	/**
	 * Checks if this Animator is done animating.
	 * 
	 * @return
	 */
	boolean isFinished();
	
	/**
	 * Stop the animation.
	 */
	void stop();
	
}
