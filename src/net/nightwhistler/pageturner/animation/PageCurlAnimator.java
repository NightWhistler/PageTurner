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

import android.graphics.*;
import android.graphics.Paint.Style;
import android.text.TextPaint;

/**
 * PageCurl animator, simulates flipping pages.
 * 
 * All credit for this goes to Moritz 'Moss' Wundke (b.thax.dcg@gmail.com),
 * since it's just an adapted and simplified version of his PageCurlView.
 * 
 * The original project is here: http://code.google.com/p/android-page-curl/
 *
 * @author Alex Kuiper
 *
 */
public class PageCurlAnimator implements Animator {
		
	
	// Debug text paint stuff
	private Paint mTextPaint;
	private TextPaint mTextPaintShadow;
	
	/** Px / Draw call */
	private int mCurlSpeed;
	
	/** Fixed update time used to create a smooth curl animation */
	//private int mUpdateRate;
	
	/** The initial offset for x and y axis movements */
	private int mInitialEdgeOffset;	
		
	/** Maximum radius a page can be flipped, by default it's the width of the view */
	private float mFlipRadius;
	
	/** Point used to move */
	private Vector2D mMovement;	
	
	/** Movement point form the last frame */
	private Vector2D mOldMovement;
	
	/** Page curl edge */
	private Paint mCurlEdgePaint;
	
	/** Our points used to define the current clipping paths in our draw call */
	private Vector2D mA, mB, mC, mD, mE, mF, mOrigin;	
	
	/** If false no draw call has been done */
	private boolean bViewDrawn;
	
	/** Defines the flip direction that is currently considered */
	private boolean bFlipRight;
		
	/** LAGACY The current foreground */
	private Bitmap mForeground;
	
	/** LAGACY The current background */
	private Bitmap mBackground;		
	
	private int backgroundColor = Color.WHITE;
	private int edgeColor = Color.BLACK;

	private boolean started = false;
	private boolean finished = false;
	
	private boolean drawDebugEnabled = false;
	
	public PageCurlAnimator(boolean flipRight) {
		this.bFlipRight = flipRight;
		//this.drawDebugEnabled = true;
		init();
	}
	
	public void setDrawDebugEnabled(boolean drawDebugEnabled) {
		this.drawDebugEnabled = drawDebugEnabled;
	}

	@Override
	public synchronized void advanceOneFrame() {
		
		started = true;
		
		if ( finished || mOrigin == null ) {
			return;
		}
		
		int width = getWidth();
				
		// Handle speed
		float curlSpeed = mCurlSpeed;
		if ( !bFlipRight ) {
			curlSpeed *= -1;
		}
		
		// Move us
		mMovement.x += curlSpeed;
		mMovement = CapMovement(mMovement, false);
		
		// Create values
		doSimpleCurl();
		
		// Check for endings :D
		if (mA.x < 1 || mA.x > width - 1) {
			finished = true;
			
			ResetClipEdge();
			
			// Create values
			doSimpleCurl();			
		}
	}
	
	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	
	public void setEdgeColor( int edgeColor ) {
		this.edgeColor = edgeColor;
	}
	
	private void drawDebug(Canvas canvas)
	{
		float posX = 10;
		float posY = 20;
		
		Paint paint = new Paint();
		paint.setStrokeWidth(5);
		paint.setStyle(Style.FILL);
		
		paint.setColor(Color.BLACK);
		
		canvas.drawCircle(mOrigin.x, mOrigin.y, getWidth(), paint);
		
		paint.setStrokeWidth(3);
		paint.setColor(Color.RED);		
		canvas.drawCircle(mOrigin.x, mOrigin.y, getWidth(), paint);
		
		paint.setStrokeWidth(5);
		paint.setColor(Color.BLACK);
		canvas.drawLine(mOrigin.x, mOrigin.y, mMovement.x, mMovement.y, paint);
		
		paint.setStrokeWidth(3);
		paint.setColor(Color.RED);
		canvas.drawLine(mOrigin.x, mOrigin.y, mMovement.x, mMovement.y, paint);
		
		posY = debugDrawPoint(canvas,"A",mA,Color.RED,posX,posY);
		posY = debugDrawPoint(canvas,"B",mB,Color.GREEN,posX,posY);
		posY = debugDrawPoint(canvas,"C",mC,Color.BLUE,posX,posY);
		posY = debugDrawPoint(canvas,"D",mD,Color.CYAN,posX,posY);
		posY = debugDrawPoint(canvas,"E",mE,Color.YELLOW,posX,posY);
		posY = debugDrawPoint(canvas,"F",mF,Color.LTGRAY,posX,posY);
		posY = debugDrawPoint(canvas,"Mov",mMovement,Color.DKGRAY,posX,posY);
		posY = debugDrawPoint(canvas,"Origin",mOrigin,Color.MAGENTA,posX,posY);
		
		/**/
	}
	
	private float debugDrawPoint(Canvas canvas, String name, Vector2D point, int color, float posX, float posY) {	
		return debugDrawPoint(canvas,name+" "+point.toString(),point.x, point.y, color, posX, posY);
	}
	
	private float debugDrawPoint(Canvas canvas, String name, float X, float Y, int color, float posX, float posY) {
		mTextPaint.setColor(color);
		drawTextShadowed(canvas,name,posX , posY, mTextPaint,mTextPaintShadow);
		Paint paint = new Paint();
		paint.setStrokeWidth(5);
		paint.setColor(color);	
		canvas.drawPoint(X, Y, paint);
		return posY+15;
	}

	/**
	 * Draw a text with a nice shadow
	 */
	public static void drawTextShadowed(Canvas canvas, String text, float x, float y, Paint textPain, Paint shadowPaint) {
    	canvas.drawText(text, x-1, y, shadowPaint);
    	canvas.drawText(text, x, y+1, shadowPaint);
    	canvas.drawText(text, x+1, y, shadowPaint);
    	canvas.drawText(text, x, y-1, shadowPaint);    	
    	canvas.drawText(text, x, y, textPain);
    }

	
	public void setForegroundBitmap(Bitmap bitmap ) {
		this.mForeground = bitmap;
	}
	
	public void setBackgroundBitmap(Bitmap bitmap) {
		this.mBackground = bitmap;
	}
	
	private int getWidth() {
		if ( mBackground != null ) {
			return mBackground.getWidth();
		}
		
		return 0;
	}
	
	private int getHeight() {
		if ( mBackground != null ) {
			return mBackground.getHeight();
		}
		
		return 0;
	}
	
	@Override
	public void draw(Canvas canvas) {			
				
		// We need to initialize all size data when we first draw the view
		if ( !bViewDrawn ) {
			bViewDrawn = true;
			onFirstDrawEvent(canvas);
		}
		
		canvas.drawColor(backgroundColor);		
		
		// TODO: This just scales the views to the current
		// width and height. We should add some logic for:
		//  1) Maintain aspect ratio
		//  2) Uniform scale
		//  3) ...
		Rect rect = new Rect();
		rect.left = 0;
		rect.top = 0;
		rect.bottom = getHeight();
		rect.right = getWidth();
		
		// First Page render
		Paint paint = new Paint();
		
	
		// Draw our elements
		try {
			drawForeground(canvas, rect, paint);
		} catch (Exception e ) {
			//ErrorReporter.getInstance().handleException(e);
		}
		
		try {
			drawBackground(canvas, rect, paint);
		} catch (Exception e) {
			//ErrorReporter.getInstance().handleException(e);
		}		
		
		drawCurlEdge(canvas);
		
		if ( this.drawDebugEnabled  ) {
			drawDebug(canvas);
		}
		
	}
	
	/**
	 * Initialize the view
	 */
	private final void init() {		
		
		// Foreground text paint
		mTextPaint = new Paint();
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(16);
		mTextPaint.setColor(0xFF000000);
		
		// The shadow
		mTextPaintShadow = new TextPaint();
		mTextPaintShadow.setAntiAlias(true);
		mTextPaintShadow.setTextSize(16);
		mTextPaintShadow.setColor(0x00000000);
		
		// Base padding
		//setPadding(3, 3, 3, 3);
		
		mMovement =  new Vector2D(0,0);
		
		mOldMovement = new Vector2D(0,0);				
		
		// Create our edge paint
		mCurlEdgePaint = new Paint();		
		mCurlEdgePaint.setAntiAlias(true);
		mCurlEdgePaint.setStyle(Paint.Style.STROKE);
	//	mCurlEdgePaint.setColor(this.edgeColor);
		
	//	mCurlEdgePaint.setShadowLayer(10, -5, 5, 0x99000000);
		
		
		// Set the default props, those come from an XML :D
		mCurlSpeed = 30;		
		mInitialEdgeOffset = 20;
		
	}
	
	/**
	 * Reset points to it's initial clip edge state
	 */
	public void ResetClipEdge()
	{
		// Set our base movement
		mMovement.x = mInitialEdgeOffset;
		mMovement.y = mInitialEdgeOffset;		
		mOldMovement.x = 0;
		mOldMovement.y = 0;		
		
		if ( ! bFlipRight ) {
			mMovement.x = getWidth();
			mMovement.y = mInitialEdgeOffset;
		}
		
		// Now set the points
		// TODO: OK, those points MUST come from our measures and
		// the actual bounds of the view!
		mA = new Vector2D(mInitialEdgeOffset, 0);
		mB = new Vector2D(this.getWidth(), this.getHeight());
		mC = new Vector2D(this.getWidth(), 0);
		mD = new Vector2D(0, 0);
		mE = new Vector2D(0, 0);
		mF = new Vector2D(0, 0);		
				
		// The movement origin point
		mOrigin = new Vector2D(this.getWidth(), 0);
	}	
	
	/**
	 * Set the curl speed.
	 * @param curlSpeed - New speed in px/frame
	 * @throws IllegalArgumentException if curlspeed < 1
	 */
	public void SetCurlSpeed(int curlSpeed)
	{
		if ( curlSpeed < 1 )
			throw new IllegalArgumentException("curlSpeed must be greated than 0");
		mCurlSpeed = curlSpeed;
	}
	
	/**
	 * Get the current curl speed
	 * @return int - Curl speed in px/frame
	 */
	public int GetCurlSpeed()
	{
		return mCurlSpeed;
	}	
	
	/**
	 * Set the initial pixel offset for the curl edge
	 * @param initialEdgeOffset - px offset for curl edge
	 * @throws IllegalArgumentException if initialEdgeOffset < 0
	 */
	public void SetInitialEdgeOffset(int initialEdgeOffset)
	{
		if ( initialEdgeOffset < 0 )
			throw new IllegalArgumentException("initialEdgeOffset can not negative");
		mInitialEdgeOffset = initialEdgeOffset;
	}
	
	/**
	 * Get the initial pixel offset for the curl edge
	 * @return int - px
	 */
	public int GetInitialEdgeOffset()
	{
		return mInitialEdgeOffset;
	}
	
	/**
	 * Make sure we never move too much, and make sure that if we 
	 * move too much to add a displacement so that the movement will 
	 * be still in our radius.
	 * @param radius - radius form the flip origin
	 * @param bMaintainMoveDir - Cap movement but do not change the
	 * current movement direction
	 * @return Corrected point
	 */
	private Vector2D CapMovement(Vector2D point, boolean bMaintainMoveDir)
	{
		// Make sure we never ever move too much
		if (point.distance(mOrigin) > mFlipRadius)
		{
			if ( bMaintainMoveDir )
			{
				// Maintain the direction
				point = mOrigin.sum(point.sub(mOrigin).normalize().mult(mFlipRadius));
			}
			else
			{
				// Change direction
				if ( point.x > (mOrigin.x+mFlipRadius))
					point.x = (mOrigin.x+mFlipRadius);
				else if ( point.x < (mOrigin.x-mFlipRadius) )
					point.x = (mOrigin.x-mFlipRadius);
				point.y = (float) (Math.sin(Math.acos(Math.abs(point.x-mOrigin.x)/mFlipRadius))*mFlipRadius);
			}
		}
		return point;
	}
	
	/**
	 * Do a simple page curl effect
	 */
	private void doSimpleCurl() {
		int width = getWidth();
		int height = getHeight();
		
		// Calculate point A
		mA.x = width - mMovement.x;
		mA.y = height;

		// Calculate point D
		mD.x = 0;
		mD.y = 0;
		if (mA.x > width / 2) {
			mD.x = width;
			mD.y = height - (width - mA.x) * height / mA.x;
		} else {
			mD.x = 2 * mA.x;
			mD.y = 0;
		}
		
		// Now calculate E and F taking into account that the line
		// AD is perpendicular to FB and EC. B and C are fixed points.
		double angle = Math.atan((height - mD.y) / (mD.x + mMovement.x - width));
		double _cos = Math.cos(2 * angle);
		double _sin = Math.sin(2 * angle);

		// And get F
		mF.x = (float) (width - mMovement.x + _cos * mMovement.x);
		mF.y = (float) (height - _sin * mMovement.x);
		
		// If the x position of A is above half of the page we are still not
		// folding the upper-right edge and so E and D are equal.
		if (mA.x > width / 2) {
			mE.x = mD.x;
			mE.y = mD.y;
		}
		else
		{
			// So get E
			mE.x = (float) (mD.x + _cos * (width - mD.x));
			mE.y = (float) -(_sin * (width - mD.x));
		}
	}

	
	/**
	 * Called on the first draw event of the view
	 * @param canvas
	 */
	protected void onFirstDrawEvent(Canvas canvas) {
		
		mFlipRadius = getWidth();
		
		ResetClipEdge();
		doSimpleCurl();
	}
	
	/**
	 * Draw the foreground
	 * @param canvas
	 * @param rect
	 * @param paint
	 */
	private void drawForeground( Canvas canvas, Rect rect, Paint paint ) {		
		if ( ! finished || !bFlipRight ) {
			if ( ! mForeground.isRecycled() ) {
				canvas.drawBitmap(mForeground, null, rect, null);
			}
		}
	}
	
	/**
	 * Create a Path used as a mask to draw the background page
	 * @return
	 */
	private Path createBackgroundPath() {
		Path path = new Path();
		path.moveTo(mA.x, mA.y);
		path.lineTo(mB.x, mB.y);
		path.lineTo(mC.x, mC.y);
		path.lineTo(mD.x, mD.y);
		path.lineTo(mA.x, mA.y);
		return path;
	}
	
	/**
	 * Draw the background image.
	 * @param canvas
	 * @param rect
	 * @param paint
	 */
	private void drawBackground( Canvas canvas, Rect rect, Paint paint ) {
		
		if ( ! finished ) {
			Path mask = createBackgroundPath();

			// Save current canvas so we do not mess it up
			canvas.save();
			canvas.clipPath(mask);						
		}
		
		if ( ! (finished && !bFlipRight) ) { 
			
			if ( ! mBackground.isRecycled() ) {
				canvas.drawBitmap(mBackground, null, rect, paint);
			}
			
			canvas.restore();
		}
		
	}
	
	/**
	 * Creates a path used to draw the curl edge in.
	 * @return
	 */
	private Path createCurlEdgePath() {
		Path path = new Path();
		path.moveTo(mA.x, mA.y);
		path.lineTo(mD.x, mD.y);
		path.lineTo(mE.x, mE.y);
		path.lineTo(mF.x, mF.y);
		path.lineTo(mA.x, mA.y);
		return path;
	}
	
	/**
	 * Draw the curl page edge
	 * @param canvas
	 */
	private void drawCurlEdge( Canvas canvas )
	{
		if ( started && ! finished ) {
			
			Path path = createCurlEdgePath();
			
			mCurlEdgePaint.setColor(backgroundColor);
			mCurlEdgePaint.setStyle(Style.FILL);
			mCurlEdgePaint.setShadowLayer(10, -5, 5, edgeColor );
			
			canvas.drawPath(path, mCurlEdgePaint);
			
			mCurlEdgePaint.setColor(edgeColor);
			mCurlEdgePaint.setStyle(Style.STROKE);
			mCurlEdgePaint.setShadowLayer(0, 0, 0, edgeColor );
			
			canvas.drawPath(path, mCurlEdgePaint);
			
		}
	}	
	
	@Override
	public int getAnimationSpeed() {
		return 30;
	}
	
	@Override
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Inner class used to represent a 2D point.
	 */
	private class Vector2D
	{
		public float x,y;
		public Vector2D(float x, float y)
		{
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "("+this.x+","+this.y+")";
		}
		
		public boolean equals(Object o) {
			if (o instanceof Vector2D) {
				Vector2D p = (Vector2D) o;
				return p.x == x && p.y == y;
	        }
	        return false;
		}
		
		public Vector2D sum(Vector2D b) {
            return new Vector2D(x+b.x,y+b.y);
		}
		
		public Vector2D sub(Vector2D b) {
            return new Vector2D(x-b.x,y-b.y);
		}  
	    public float distanceSquared(Vector2D other) {    	
	    	
	    	float dx = other.x - x;
	    	float dy = other.y - y;

            return (dx * dx) + (dy * dy);
	    }
	
	    public float distance(Vector2D other) {
	            return (float) Math.sqrt(distanceSquared(other));
	    }
	    
	    public float dotProduct(Vector2D other) {
            return other.x * x + other.y * y;
	    }
		
		public Vector2D normalize() {
			float magnitude = (float) Math.sqrt(dotProduct(this));
            return new Vector2D(x / magnitude, y / magnitude);
		}
		
		public Vector2D mult(float scalar) {
	            return new Vector2D(x*scalar,y*scalar);
	    }
	}
	
	@Override
	public void stop() {
		this.finished = true;		
	}
	
}
