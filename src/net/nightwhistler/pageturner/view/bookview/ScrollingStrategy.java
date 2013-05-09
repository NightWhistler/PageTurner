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
package net.nightwhistler.pageturner.view.bookview;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;


public class ScrollingStrategy implements PageChangeStrategy {
	
	private BookView bookView;
	
	private TextView childView;
	private int storedPosition = -1;
	private double storedPercentage = -1;
	
	private Spanned text;
	
	private Context context;
	
	public ScrollingStrategy(BookView bookView, Context context) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();		
		this.context = context;
	}
	
	@Override
	public Spanned getNextPageText() {		
		return null;
	}
	
	@Override
	public Spanned getPreviousPageText() {		
		return null;
	}
	
	@Override
	public int getTopLeftPosition() {
		if ( childView.getText().length() == 0 ) {
			return storedPosition;
		} else {
			int yPos = bookView.getScrollY();
		
			return findTextOffset(findClosestLineBottom(yPos));
		}
	}

    public int getProgressPosition() {
        return getTopLeftPosition();
    }
	
	@Override
	public boolean isAtEnd() {
		int ypos = bookView.getScrollY() + bookView.getHeight();
		
		Layout layout = this.childView.getLayout();
		if ( layout == null ) {
			return false;
		}
		
		int line = layout.getLineForVertical(ypos);
		return line == layout.getLineCount() -1;
	}
	
	@Override
	public boolean isAtStart() {
		return getTopLeftPosition() == 0;
	}
	
	@Override
	public void loadText(Spanned text) {	
		
		this.text = addEndTag(text);
	}

    @Override
    public void updateGUI() {
		childView.setText(this.text);
		updatePosition();
    }
	
	private Spanned addEndTag(Spanned text) {
		
		//Don't add the tag to the last section.
		PageTurnerSpine spine = bookView.getSpine();
		
		if (spine == null || spine.getPosition() >= spine.size() -1 ) {
			return text;
		}
		
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		
		int length = builder.length();
		builder.append("\uFFFC");
		builder.append("\n");
		builder.append( context.getString(R.string.end_of_section));
		//If not, consider it an internal nav link.			
		ClickableSpan span = new ClickableSpan() {
				
			@Override
			public void onClick(View widget) {
				pageDown();					
			}
		};
		
		Drawable img = context.getResources().getDrawable(R.drawable.gateway);
		img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight() );
		builder.setSpan(new ImageSpan(img), length, length+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.setSpan(span, length, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.setSpan(new AlignmentSpan() {
			@Override
			public Alignment getAlignment() {
				return Alignment.ALIGN_CENTER;
			}
		}, length, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		return builder;		
	}
	
	@Override
	public void pageDown() {
		this.scroll( bookView.getHeight() - 2 * bookView.getVerticalMargin());
	}
	
	@Override
	public void pageUp() {
		this.scroll( (bookView.getHeight() - 2* bookView.getVerticalMargin() ) * -1);
	}
	
	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;
		updatePosition();
	}
	
	@Override
	public void clearText() {
		this.childView.setText("");	
		this.text = null;
	}
	
	@Override
	public void setRelativePosition(double position) {
		this.storedPercentage = position;
		updatePosition();
	}
	
	public void updatePosition() {


		if ( storedPosition == -1 && this.storedPercentage == -1d ) {
			return;  //Hopefully come back later
		}

		if ( childView.getText().length() == 0 ) {
			return;
		}

		if ( storedPercentage != -1d ) {
			this.storedPosition = (int) (this.childView.getText().length() * storedPercentage);
			this.storedPercentage = -1d;
		}

		Layout layout = this.childView.getLayout();

		if ( layout != null ) {
			int pos = Math.max(0, this.storedPosition);
			int line = layout.getLineForOffset(pos);

			if ( line > 0 ) {
				int newPos = layout.getLineBottom(line -1);
				bookView.scrollTo(0, newPos);
			} else {
				bookView.scrollTo(0, 0);
			}
		}						 
	}

	
	@Override
	public Spanned getText() {
		return text;
	}
	
	@Override
	public void reset() {
		this.storedPosition = -1;		
	}
	
	@Override
	public void clearStoredPosition() {
		this.storedPosition = -1;	
	}
	
	private void scroll( int delta ) {
		
		if ( this.bookView == null ) {
			return;
		}
		
		int currentPos = bookView.getScrollY();
		
		int newPos = currentPos + delta;
		
		bookView.scrollTo(0, findClosestLineBottom(newPos));
		
		if ( bookView.getScrollY() == currentPos ) {
						
			if ( delta < 0 ) {				
				if (bookView.getSpine() == null || ! bookView.getSpine().navigateBack() ) {					
					return;
				}
			} else {				
				if (bookView.getSpine() == null ||  ! bookView.getSpine().navigateForward() ) {
					return;
				}
			}
			
			this.childView.setText("");
			
			if ( delta > 0 ) {
				bookView.scrollTo(0,0);
				this.storedPosition = -1;
			} else {
				bookView.scrollTo(0, bookView.getHeight());
				
				//We scrolled back up, so we want the very bottom of the text.
				this.storedPosition = Integer.MAX_VALUE;
			}	
			
			bookView.loadText();
		}
	}
	
	private int findClosestLineBottom( int ypos ) {
				
		Layout layout = this.childView.getLayout();
		
		if ( layout == null ) {
			return ypos;
		}
		
		int currentLine = layout.getLineForVertical(ypos);
		
		//System.out.println("Returning line " + currentLine + " for ypos " + ypos);
		
		if ( currentLine > 0 ) {
			int height = layout.getLineBottom(currentLine -1);
			return height;
		} else {
			return 0;
		}		
	}
	
	private int findTextOffset(int ypos) {
		
		Layout layout = this.childView.getLayout();
		if ( layout == null ) {
			return 0;
		}
		
		return layout.getLineStart(layout.getLineForVertical(ypos));		
	}
	
	@Override
	public boolean isScrolling() {
		return true;
	}
	
}
