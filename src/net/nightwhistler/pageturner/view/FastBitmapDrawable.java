/*
 * Copyright (C) 2008 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nightwhistler.pageturner.view;

import android.graphics.*;
import android.graphics.drawable.Drawable;

public class FastBitmapDrawable extends Drawable {
    
	private Bitmap mBitmap;
	
	private int width;
	private int height;
	
	private Paint paint = new Paint();

    public FastBitmapDrawable(Bitmap b) {
        mBitmap = b;

        if ( b != null ) {
            this.width = b.getWidth();
            this.height = b.getHeight();
        }
    }

    @Override
    public void draw(Canvas canvas) {
    	if ( mBitmap != null ) {
    		canvas.drawBitmap(mBitmap, 0.0f, 0.0f, null);
    	} else {
    		paint.setColor(Color.GRAY);
    		canvas.drawRect(0, 0, width, height, paint);
    	}
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public int getMinimumWidth() {
        return width;
    }

    @Override
    public int getMinimumHeight() {
        return height;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }
    
    public void destroy() {
        if ( this.mBitmap != null ) {
    	    this.mBitmap.recycle();
        }

    	this.mBitmap = null;
    	this.setCallback(null);
    }
}
