package net.nightwhistler.pageturner.view;

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
	
	private enum ZoomMode { ZOOM, NONE };

	float oldDist = 1f;
	
	private FloatAdapter adapter;
	private ZoomMode mode = ZoomMode.NONE;
	
	public PinchZoomListener(FloatAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			//Log.d(TAG, "oldDist=" + oldDist);
			if (oldDist > 10f) {
				mode = ZoomMode.ZOOM;
				//Log.d(TAG, "mode=ZOOM");
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mode = ZoomMode.NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == ZoomMode.ZOOM) {
				float newDist = spacing(event);
				// If you want to tweak font scaling, this is the place to go.
				if (newDist > 10f) {
					float scale = newDist / oldDist;

					if (scale > 1) {
						scale = 1.1f;
					} else if (scale < 1) {
						scale = 0.95f;
					}

					float currentSize = adapter.getValue() * scale;
					if ((currentSize < MAX_FONT_SIZE && currentSize > MIN_FONT_SIZE)
							|| (currentSize >= MAX_FONT_SIZE && scale < 1)
							|| (currentSize <= MIN_FONT_SIZE && scale > 1)) {
						adapter.setValue( currentSize);
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
