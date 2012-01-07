package net.nightwhistler.pageturner.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;

public class PageTimer implements Animator {
	
	private Bitmap backgroundImage;
	
	private int speed = 20;
	
	private static final int MAX_STEP = 500;

	int count = 0;
	
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
		Rect timer = new Rect( 0, backgroundImage.getHeight() - timerHeight, backgroundImage.getWidth(), backgroundImage.getHeight() );
		canvas.drawRect(timer, paint);
		
		float percentage = (float) count / (float) MAX_STEP;
		int width = (int) (backgroundImage.getWidth() * percentage);
		
		timer.right = width;
		paint.setStyle(Style.FILL);
		canvas.drawRect(timer, paint);
		
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
	
	public void setBackgroundImage(Bitmap backgroundImage) {
		this.backgroundImage = backgroundImage;
	}
	
}
