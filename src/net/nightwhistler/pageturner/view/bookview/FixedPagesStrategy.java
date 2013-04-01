package net.nightwhistler.pageturner.view.bookview;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import android.graphics.Canvas;
import android.text.Layout.Alignment;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;

public class FixedPagesStrategy implements PageChangeStrategy {

	private Spanned text;
	
	private int pageNum;
	
	private List<Integer> pageOffsets = new ArrayList<Integer>();
	
	private BookView bookView;
	private TextView childView;
	
	private Configuration config;
	
	private int storedPosition = -1;
	
	private StaticLayoutFactory layoutFactory;
		
	public FixedPagesStrategy(BookView bookView, Configuration config, StaticLayoutFactory layoutFactory) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();
		this.config = config;
		this.layoutFactory = layoutFactory;
	}
	
	@Override
	public void clearStoredPosition() {
		this.pageNum = 0;
		this.storedPosition = 0;
	}
	
	@Override
	public void clearText() {
		this.text = new SpannedString("");
		this.childView.setText(text);
		this.pageOffsets = new ArrayList<Integer>();
	}
	
	/**
	 * Returns the current page INSIDE THE SECTION.
	 * 
	 * @return
	 */
	public int getCurrentPage() {
		return this.pageNum;
	}
	
	public List<Integer> getPageOffsets(CharSequence text, boolean includePageNumbers ) {
		
		if ( text == null ) {
			return new ArrayList<Integer>();
		}
		
		List<Integer> pageOffsets = new ArrayList<Integer>();
		
		TextPaint textPaint = bookView.getInnerView().getPaint();
		int boundedWidth = bookView.getInnerView().getWidth();

		StaticLayout layout = layoutFactory.create(text, textPaint, boundedWidth, bookView.getLineSpacing() );
		
		layout.draw(new Canvas());
		
		int pageHeight = bookView.getHeight() - ( 2* bookView.getVerticalMargin() );

		String bottomSpace;
		
		if ( includePageNumbers ) {
			bottomSpace = "\n0\n";
		} else {
			bottomSpace = "\n";
		}
		
		StaticLayout numLayout = layoutFactory.create(bottomSpace, textPaint, boundedWidth , bookView.getLineSpacing());
		layout.draw(new Canvas());
		
		//Subtract the height needed to show page numbers
		pageHeight = pageHeight - numLayout.getHeight();				
		
		int totalLines = layout.getLineCount();				
		int topLineNextPage = 0;
		
		int pageStartOffset = 0;
		
		while ( topLineNextPage < totalLines -1 ) {	
			
			int topLine = layout.getLineForOffset(pageStartOffset);				
			topLineNextPage = layout.getLineForVertical( layout.getLineTop( topLine ) + pageHeight);
						
			int pageEnd = layout.getLineEnd(topLineNextPage -1);
			
			if (text.subSequence(pageStartOffset, pageEnd).toString().trim().length() > 0 ) {

				//Make sure we don't enter the same offset twice
				if (pageOffsets.isEmpty() ||  pageStartOffset != pageOffsets.get(pageOffsets.size() -1)) {			
					pageOffsets.add(pageStartOffset);
				}
				
				pageStartOffset = layout.getLineStart(topLineNextPage);
			}
		}	
		
		return pageOffsets;		
	}
	
	
	@Override
	public void reset() {
		clearStoredPosition();
		this.pageOffsets.clear();
		clearText();
	}
	
	private void updateStoredPosition() {
		for ( int i=0; i < this.pageOffsets.size(); i++ ) {
			if ( this.pageOffsets.get(i) > this.storedPosition ) {
				this.pageNum = i -1;
				this.storedPosition = -1;
				return;
			}
		}
		
		this.pageNum = this.pageOffsets.size() - 1;
		this.storedPosition = -1;
	}
	
	@Override
	public void updatePosition() {
		
		if ( pageOffsets.isEmpty() || text.length() == 0 || this.pageNum == -1) {
			return;
		}
		
		if ( storedPosition != -1 ) {
			updateStoredPosition();
		}
		
		if ( this.pageNum >= pageOffsets.size() -1 ) {
			childView.setText( this.text.subSequence(pageOffsets.get(pageOffsets.size() -1), text.length() ));
		} else {
			int start = this.pageOffsets.get(pageNum);
			int end = this.pageOffsets.get(pageNum +1 );
			childView.setText( this.text.subSequence(start, end));
		}		
	}
	
	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;		
	}
	
	@Override
	public void setRelativePosition(double position) {
		
		int intPosition = (int) (this.text.length() * position);
		setPosition(intPosition);
		
	}
	
	public int getPosition() {
		if (this.pageOffsets.isEmpty() ||  this.pageNum == -1 ) {
			return storedPosition;
		}
		
		if ( this.pageNum >= this.pageOffsets.size() ) {
			return this.pageOffsets.get( this.pageOffsets.size() -1 );
		}
		
		return this.pageOffsets.get(this.pageNum);
	}
	
	public android.text.Spanned getText() {
		return text;
	}
	
	public boolean isAtEnd() {
		return pageNum == this.pageOffsets.size() - 1;
	}
	
	public boolean isAtStart() {
		return this.pageNum == 0;
	}
	
	public boolean isScrolling() {
		return false;
	}
	
	@Override
	public void pageDown() {
	
		if ( isAtEnd() ) {
			PageTurnerSpine spine = bookView.getSpine();
		
			if ( spine == null || ! spine.navigateForward() ) {
				return;
			}
			
			this.clearText();
			this.pageNum = 0;
			bookView.loadText();
			
		} else {
			this.pageNum = Math.min(pageNum +1, this.pageOffsets.size() -1 );
			updatePosition();
		}
	}
	
	@Override
	public void pageUp() {
	
		if ( isAtStart() ) {
			PageTurnerSpine spine = bookView.getSpine();
		
			if ( spine == null || ! spine.navigateBack() ) {
				return;
			}
			
			this.clearText();
			this.storedPosition = Integer.MAX_VALUE;
			this.bookView.loadText();
		} else {
			this.pageNum = Math.max(pageNum -1, 0);
			updatePosition();
		}
	}
	
	@Override
	public void loadText(Spanned text) {
		this.text = text;
		this.pageNum = 0;
		this.pageOffsets = getPageOffsets(text, config.isShowPageNumbers() );
	}

    @Override
    public void updateGUI() {
        updatePosition();   
	}
    
}
