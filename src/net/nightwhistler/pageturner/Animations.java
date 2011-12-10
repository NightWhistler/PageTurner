package net.nightwhistler.pageturner;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class Animations {

	public static final int DURATION = 500;
	
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
