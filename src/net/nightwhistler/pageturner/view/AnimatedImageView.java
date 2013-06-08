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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;
import net.nightwhistler.pageturner.animation.Animator;

public class AnimatedImageView extends ImageView {

	private Animator animator;
	
	public AnimatedImageView(Context context, AttributeSet attributes) {
		super(context, attributes);		
	}
	
	public void setAnimator(Animator animator) {
		this.animator = animator;
	}
	
	public Animator getAnimator() {
		return animator;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if ( this.animator != null ) {
			animator.draw(canvas);
		}
	}
}
