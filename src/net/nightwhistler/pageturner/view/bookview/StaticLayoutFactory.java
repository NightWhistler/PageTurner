package net.nightwhistler.pageturner.view.bookview;

import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

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
		return new StaticLayout(source, paint, width, Alignment.ALIGN_NORMAL, 1.0f, spacingadd, false);
	}
	
}
