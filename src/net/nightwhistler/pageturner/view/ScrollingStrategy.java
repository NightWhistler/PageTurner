package net.nightwhistler.pageturner.view;

import android.text.Layout;
import android.text.Spanned;
import android.widget.TextView;


public class ScrollingStrategy implements PageChangeStrategy {
	
	private BookView bookView;
	
	private TextView childView;
	private int storedPosition;
	
	public ScrollingStrategy(BookView bookView) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();		
	}

	@Override
	public int getPosition() {
		int yPos = bookView.getScrollY();
		
		return findTextOffset(findClosestLineBottom(yPos));
	}
	
	@Override
	public void loadText(Spanned text) {		
		childView.setText(text);
		updatePosition();
	}
	
	@Override
	public void pageDown() {
		this.scroll( bookView.getHeight() - 2 * BookView.PADDING);
	}
	
	@Override
	public void pageUp() {
		this.scroll( (bookView.getHeight() - 2* BookView.PADDING ) * -1);
	}
	
	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;
		updatePosition();
	}
	
	@Override
	public void clearText() {
		this.childView.setText("");		
	}
	
	public void updatePosition() {
		if ( this.storedPosition == -1 || this.childView.getText().length() == 0 ) {			
			return; //Hopefully come back later
		} else {
			
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
		
		int currentPos = bookView.getScrollY();
		
		int newPos = currentPos + delta;
		
		bookView.scrollTo(0, findClosestLineBottom(newPos));
		
		if ( bookView.getScrollY() == currentPos ) {
						
			if ( delta < 0 ) {				
				if (! bookView.getSpine().navigateBack() ) {					
					return;
				}
			} else {				
				if ( ! bookView.getSpine().navigateForward() ) {
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
