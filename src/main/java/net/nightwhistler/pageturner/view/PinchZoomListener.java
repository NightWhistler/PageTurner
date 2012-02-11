package net.nightwhistler.pageturner.view;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;

/**
 * Simple pinch-zoom implementation based on:
 * http://stackoverflow.com/questions/5375817/android-pinch-zoom
 * 
 * @author Alex Kuiper
 * 
 */
public class PinchZoomListener extends SimpleOnScaleGestureListener implements View.OnTouchListener {

	static final int MIN_FONT_SIZE = 10;
	static final int MAX_FONT_SIZE = 50;
	
	private ScaleGestureDetector scaleGestureDector;

	float oldDist = 1f;
	float zoomThreshold;
	
	private FloatAdapter adapter;
		
	public PinchZoomListener(Context context, FloatAdapter adapter) {
		this.adapter = adapter;	
		this.scaleGestureDector = new ScaleGestureDetector(context, this);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return this.scaleGestureDector.onTouchEvent(event);
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scale = detector.getScaleFactor();
		
		float origValue = this.adapter.getValue();
		float newValue = origValue * scale;
		
		if ( newValue > MAX_FONT_SIZE ) {
			newValue = MAX_FONT_SIZE;
		} else if ( newValue < MIN_FONT_SIZE ) {
			newValue = MIN_FONT_SIZE;
		}
		
		adapter.setValue(newValue);
		return true;
	}
	
	public interface FloatAdapter {
		float getValue();
		void setValue( float value );
	}
}
