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
package net.nightwhistler.pageturner.prefs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;

public class ColourChooserPref extends DialogPreference implements OnAmbilWarnaListener {

	private static final String androidns="http://schemas.android.com/apk/res/android";
	
	int defaultColour;	
	
	public ColourChooserPref(Context context, AttributeSet attributes) {
		super(context, attributes);		
		this.defaultColour = attributes.getAttributeIntValue(androidns,"defaultValue", Color.BLACK);		
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
