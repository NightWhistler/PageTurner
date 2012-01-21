package net.nightwhistler.pageturner.view;

import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

/**
 * Simple pinch-zoom implementation based on:
 * http://stackoverflow.com/questions/5375817/android-pinch-zoom
 * 
 * @author Alex Kuiper
 * 
 */
public class PinchZoomListener implements View.OnTouchListener {

	static final int MIN_FONT_SIZE = 10;
	static final int MAX_FONT_SIZE = 50;
	
	static final float MIN_ZOOM_DIST = 10f;
	static final float ZOOM_FACTOR_DECREASE = 0.95f;
	static final float ZOOM_FACTOR_INCREASE = 1.1f;
	
	private enum ZoomMode { ZOOM, NONE };

	float oldDist = 1f;
	float zoomDist;
	
	private FloatAdapter adapter;
	private ZoomMode mode = ZoomMode.NONE;
	
	public PinchZoomListener(DisplayMetrics metrics, FloatAdapter adapter) {
		this.adapter = adapter;
		this.zoomDist = MIN_ZOOM_DIST * metrics.density;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			
			if (oldDist > zoomDist) {
				mode = ZoomMode.ZOOM;				
			}
			break;
		
		case MotionEvent.ACTION_POINTER_UP:
			mode = ZoomMode.NONE;
			break;
		
		case MotionEvent.ACTION_MOVE:
			if (mode == ZoomMode.ZOOM) {
				
				float newDist = spacing(event);
				
				// If you want to tweak font scaling, this is the place to go.
				if (newDist > zoomDist) {
					float scale = newDist / oldDist;

					if (scale > 1) {
						scale = ZOOM_FACTOR_INCREASE;
					} else if (scale < 1) {
						scale = ZOOM_FACTOR_DECREASE;
					}

					float newSize = adapter.getValue() * scale;
					
					if (newSize < MAX_FONT_SIZE && newSize > MIN_FONT_SIZE) {
						adapter.setValue( newSize);
					}
				}
			}
			break;
		}
		return false;
	}

	private float spacing(MotionEvent event) {
	   float x = event.getX(0) - event.getX(1);
	   float y = event.getY(0) - event.getY(1);
	   return FloatMath.sqrt(x * x + y * y);
	}
	
	public interface FloatAdapter {
		float getValue();
		void setValue( float value );
	}
}
