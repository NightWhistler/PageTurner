package net.nightwhistler.pageturner.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

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
