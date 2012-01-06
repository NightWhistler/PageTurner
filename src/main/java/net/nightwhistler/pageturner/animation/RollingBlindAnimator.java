package net.nightwhistler.pageturner.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class RollingBlindAnimator implements Animator {

	private Bitmap backgroundBitmap;
	
	private int horizontalMargin;
	private int verticalMargin;
	
	private int count;
	
	private int stepSize = 1;
	
	private int animationSpeed;
	
	public RollingBlindAnimator(int horizontalMargin, int verticalMargin) {
		this.horizontalMargin = horizontalMargin;
		this.verticalMargin = verticalMargin;
	}
	
	@Override
	public void advanceOneFrame() {
		count++;		
	}
	
	@Override
	public void draw(Canvas canvas) {
		if ( backgroundBitmap != null ) {
			
			int pixelsToDraw = count * stepSize;
			
			Rect source = new Rect( horizontalMargin,							
					verticalMargin + pixelsToDraw,
					backgroundBitmap.getWidth() - horizontalMargin,
					backgroundBitmap.getHeight() - verticalMargin );
			
			Rect dest = new Rect( 0, pixelsToDraw, 
					backgroundBitmap.getWidth() - (2*horizontalMargin), 
					backgroundBitmap.getHeight() - (2*verticalMargin) );					
			
			canvas.drawBitmap(backgroundBitmap, source, dest, null);
			
			Paint paint = new Paint();
			paint.setColor(Color.GRAY);
			paint.setStyle(Paint.Style.STROKE);
			
			canvas.drawLine(0, dest.top, dest.right, dest.top, paint);
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
	
	public void setStepSize(int stepSize) {
		this.stepSize = stepSize;
	}
	
	public void setAnimationSpeed(int animationSpeed) {
		this.animationSpeed = animationSpeed;
	}
	
}
