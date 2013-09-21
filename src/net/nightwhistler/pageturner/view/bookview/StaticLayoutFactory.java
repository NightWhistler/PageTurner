/*
 * Copyright (C) 2013 Alex Kuiper
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

package net.nightwhistler.pageturner.view.bookview;

import android.text.*;
import android.text.Layout.Alignment;
import android.text.style.StyleSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple factory to create StaticLayout objects.
 * 
 * This class mostly exists to facilitate unit-testing, but it
 * also simplifies creation of StaticLayout objects by providing
 * defaults for some of the parameters.
 * 
 * @author Alex Kuiper
 *
 */
public class StaticLayoutFactory {

	/**
	 * Creates a new StaticLayout object
	 * 
	 * @param source the text to create it from
	 * @param paint the TextPaint to use
	 * @param width the width to use for measurement
	 * @param spacingadd extra space to be added to each line
	 * @return a StaticLayout object
	 */
	public StaticLayout create(CharSequence source, TextPaint paint, int width, float spacingadd) {
        try{
            return doCreateLayout( source, paint, width, spacingadd );
        } catch (IndexOutOfBoundsException e){
            return attemptCorrection( source, paint, width, spacingadd );
        }
	}

    /**
     * This method tries to compensate for a Jelly-bean bug:
     * http://code.google.com/p/android/issues/detail?id=35466
     *
     * It strips out spans one by one until it is able to return a text or has
     * to give up, in which case it returns the plain-text version.
     */
    private StaticLayout attemptCorrection( CharSequence source, TextPaint paint, int width, float spacingadd ) {

        SpannableStringBuilder ss = new SpannableStringBuilder(source);
        StyleSpan[] spans = ss.getSpans(0, ss.length(), StyleSpan.class);

        for ( int i=0; i < spans.length; i++ ) {
            ss.removeSpan( spans[i] );

            try {
                return doCreateLayout( ss, paint, width, spacingadd );
            } catch ( IndexOutOfBoundsException ie ) {
                //Ignore and remove another span
            }
        }

        return doCreateLayout( ss.toString(), paint, width, spacingadd );
    }

    private StaticLayout doCreateLayout( CharSequence source, TextPaint paint, int width, float spacingadd ) {
        return new StaticLayout(source, paint, width, Alignment.ALIGN_NORMAL, 1.0f, spacingadd, true);
    }


	
}
