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

import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class Animations {

	public static final int DURATION = 300;
	
	public static Animation inFromRightAnimation() {

    	Animation inFromRight = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  +1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	inFromRight.setDuration(DURATION);
    	inFromRight.setInterpolator(new AccelerateInterpolator());
    	return inFromRight;
    }
    
    public static Animation outToLeftAnimation() {
    	Animation outtoLeft = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  -1.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	outtoLeft.setDuration(DURATION);
    	outtoLeft.setInterpolator(new AccelerateInterpolator());
    	return outtoLeft;
    }

    public static Animation inFromLeftAnimation() {
    	Animation inFromLeft = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  -1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	inFromLeft.setDuration(DURATION);
    	inFromLeft.setInterpolator(new AccelerateInterpolator());
    	return inFromLeft;
    }
    
    public static Animation outToRightAnimation() {
    	Animation outtoRight = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  +1.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	outtoRight.setDuration(DURATION);
    	outtoRight.setInterpolator(new AccelerateInterpolator());
    	return outtoRight;
    }
    
    public static Animation outToBottomAnimation() {
    	Animation outtoRight = new TranslateAnimation(    			
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  +1.0f
    	);
    	outtoRight.setDuration(DURATION);
    	outtoRight.setInterpolator(new AccelerateInterpolator());
    	return outtoRight;
    }
    
    public static Animation inFromTopAnimation() {
    	Animation outtoRight = new TranslateAnimation(    			
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f,
    			Animation.RELATIVE_TO_PARENT,  -1.0f, Animation.RELATIVE_TO_PARENT,  0.0f
    	);
    	outtoRight.setDuration(DURATION);
    	outtoRight.setInterpolator(new AccelerateInterpolator());
    	return outtoRight;
    }
    
    public static Animation inFromBottomAnimation() {
    	Animation outtoRight = new TranslateAnimation(    			
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f,
    			Animation.RELATIVE_TO_PARENT,  +1.0f, Animation.RELATIVE_TO_PARENT,  0.0f
    	);
    	outtoRight.setDuration(DURATION);
    	outtoRight.setInterpolator(new AccelerateInterpolator());
    	return outtoRight;
    }
    
    public static Animation outToTopAnimation() {
    	Animation outtoRight = new TranslateAnimation(    			
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  -1.0f
    	);
    	outtoRight.setDuration(DURATION);
    	outtoRight.setInterpolator(new AccelerateInterpolator());
    	return outtoRight;
    }
    
}
