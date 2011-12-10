/*
 * Copyright (C) 2011 Alex Kuiper
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
package net.nightwhistler.pageturner.prefs;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class ColourChooserPref extends DialogPreference implements OnAmbilWarnaListener {

	int defaultColour;	
	
	public ColourChooserPref(Context context, AttributeSet attributes) {
		super(context, attributes);
	}
	
	@Override
	public void setDefaultValue(Object defaultValue) {		
		super.setDefaultValue(defaultValue);
		
		this.defaultColour = (Integer) defaultValue;
	}
	
	@Override
	public void onCancel(AmbilWarnaDialog dialog) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onOk(AmbilWarnaDialog dialog, int color) {
		persistInt(color);		
	}
	
	@Override
	protected void onClick() {
		getDialog().show();
	}
	
	@Override
	public Dialog getDialog() {		
		
		int colour = this.getPersistedInt( this.defaultColour );
		
		return new AmbilWarnaDialog(this.getContext(), colour, this ).getDialog();
		
	}
	
	public class Square extends Drawable
	{
	    private final Paint mPaint;
	    private final RectF mRect;

	    public Square()
	    {
	        mPaint = new Paint();
	        mRect = new RectF();
	    }

	    @Override
	    public void draw(Canvas canvas)
	    {
	        // Set the correct values in the Paint
	        mPaint.setColor( getPersistedInt(defaultColour) );
	    	
	        mPaint.setStrokeWidth(2);
	        mPaint.setStyle(Style.FILL);

	        // Adjust the rect
	        mRect.left = 15.0f;
	        mRect.top = 50.0f;
	        mRect.right = 55.0f;
	        mRect.bottom = 75.0f;

	        // Draw it
	        canvas.drawRoundRect(mRect, 0.5f, 0.5f, mPaint);
	    }

	    @Override
	    public int getOpacity()
	    {
	        return PixelFormat.OPAQUE;
	    }

	    @Override
	    public void setAlpha(int arg0)
	    {
	    }

	    @Override
	    public void setColorFilter(ColorFilter arg0)
	    {
	    }
	}

	
}
