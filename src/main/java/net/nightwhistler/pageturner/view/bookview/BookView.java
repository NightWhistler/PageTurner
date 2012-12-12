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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.nightwhistler.htmlspanner.FontFamily;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.handlers.TableHandler;
import net.nightwhistler.htmlspanner.spans.CenterSpan;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import net.nightwhistler.pageturner.epub.ResourceLoader;
import net.nightwhistler.pageturner.epub.ResourceLoader.ResourceCallback;
import net.nightwhistler.pageturner.tasks.SearchTextTask;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;
import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.StringUtil;

import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BookView extends ScrollView {
    	
	private int storedIndex;
	private String storedAnchor;
	
	private InnerView childView;
	
	private Set<BookViewListener> listeners;
	
	private HtmlSpanner spanner;		
	private TableHandler tableHandler;
	
	private PageTurnerSpine spine;
	
	private String fileName;
	private Book book;	
	
	private Map<String, Integer> anchors;
	
	private int prevIndex = -1;
	private int prevPos = -1;
	
	private PageChangeStrategy strategy;
	private ResourceLoader loader;		
	
	private int horizontalMargin = 0;
	private int verticalMargin = 0;
	private int lineSpacing = 0;	
	
	private Configuration configuration;
	
	private static final Logger LOG = LoggerFactory.getLogger(BookView.class);

	private Map<String, FastBitmapDrawable> imageCache = new HashMap<String, FastBitmapDrawable>();
	
	@SuppressLint("NewApi")
	public BookView(Context context, AttributeSet attributes) {
		super(context, attributes);		
		
		
	}	
	
	public void init() {
		this.listeners = new HashSet<BookViewListener>();
		
		this.childView = (InnerView) this.findViewById( R.id.innerView );				
		this.childView.setBookView(this);		
		
		childView.setCursorVisible(false);		
		childView.setLongClickable(true);	        
        this.setVerticalFadingEdgeEnabled(false);
        childView.setFocusable(true);
        childView.setLinksClickable(true);     
        
        if ( Build.VERSION.SDK_INT >= 11 ) {
        	childView.setTextIsSelectable(true);
        }
        
        this.setSmoothScrollingEnabled(false);
        
        this.anchors = new HashMap<String, Integer>();
        this.tableHandler = new TableHandler();
	}
	
	
	
	private void onInnerViewResize() {
		restorePosition();	
		
		int tableWidth = (int) ( this.getWidth() * 0.9 );
		tableHandler.setTableWidth( tableWidth );
	}
	
	public void setSpanner(HtmlSpanner spanner) {
		this.spanner = spanner;
		
		ImageTagHandler imgHandler = new ImageTagHandler();
        spanner.registerHandler("img", imgHandler );
        spanner.registerHandler("image", imgHandler );
        
        spanner.registerHandler("a", new AnchorHandler(new LinkTagHandler()) );
        
        spanner.registerHandler("h1", new AnchorHandler(spanner.getHandlerFor("h1") ));
        spanner.registerHandler("h2", new AnchorHandler(spanner.getHandlerFor("h2") ));
        spanner.registerHandler("h3", new AnchorHandler(spanner.getHandlerFor("h3") ));
        spanner.registerHandler("h4", new AnchorHandler(spanner.getHandlerFor("h4") ));
        spanner.registerHandler("h5", new AnchorHandler(spanner.getHandlerFor("h5") ));
        spanner.registerHandler("h6", new AnchorHandler(spanner.getHandlerFor("h6") ));
        
        spanner.registerHandler("p", new AnchorHandler(spanner.getHandlerFor("p") ));
        spanner.registerHandler("table", tableHandler);
	}
	
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	
	private void clearImageCache() {
		for ( Map.Entry<String, FastBitmapDrawable> draw: imageCache.entrySet() ) {
			draw.getValue().destroy();
		}
		
		imageCache.clear();
	}
	
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {		
		super.onScrollChanged(l, t, oldl, oldt);
		progressUpdate();
	}
	
	
	/**
	 * Returns if we're at the start of the book, i.e. displaying the title page.
	 * @return
	 */
	public boolean isAtStart() {
		
		if ( spine == null ) {
			return true;
		}
		
		return spine.getPosition() == 0
			&& strategy.isAtStart();
	}
	
	public boolean isAtEnd() {
		if ( spine == null ) {
			return false;
		}
		
		return spine.getPosition() >= spine.size() -1
			&& strategy.isAtEnd();
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
		this.loader = new ResourceLoader(fileName);
	}
	
	
	@Override
	public void setOnTouchListener(OnTouchListener l) {		
		super.setOnTouchListener(l);
		this.childView.setOnTouchListener(l);		
	}
	
	public void setStripWhiteSpace(boolean stripWhiteSpace) {
		this.spanner.setStripExtraWhiteSpace(stripWhiteSpace);
	}	
	
	public ClickableSpan[] getLinkAt( float x, float y ) {
		Integer offset = findOffsetForPosition(x, y);
		
		if ( offset == null ) {
			return null;
		}
		
		Spanned text = (Spanned) childView.getText();
		ClickableSpan[] spans = text.getSpans(offset, offset, ClickableSpan.class );
		
		return spans;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		
		if ( strategy.isScrolling() ) {
			return super.onTouchEvent(ev);
		} else {
			return childView.onTouchEvent(ev);	
		}		
	}	
	
	public boolean hasPrevPosition() {
		return this.prevIndex != -1 && this.prevPos != -1;
	}

	public void setLineSpacing( int lineSpacing ) {
		if ( lineSpacing != this.lineSpacing ) {
			this.lineSpacing = lineSpacing;
			this.childView.setLineSpacing(lineSpacing, 1);
			
			if ( strategy != null ) {
				strategy.updatePosition();
			}
		}
	}	
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setTextSelectionCallback(TextSelectionCallback callback ) {
		if ( Build.VERSION.SDK_INT >= 11 ) {
			this.childView.setCustomSelectionActionModeCallback(
				new TextSelectionActions(callback, this));
		}
	}
	
	public int getLineSpacing() {
		return lineSpacing;
	}
	
	public void setHorizontalMargin(int horizontalMargin) {
		
		if ( horizontalMargin != this.horizontalMargin ) {
			this.horizontalMargin = horizontalMargin;
			setPadding(this.horizontalMargin, this.verticalMargin, this.horizontalMargin, this.verticalMargin);
			if ( strategy != null ) {
				strategy.updatePosition();
			}
		}		
	}
	
	public void releaseResources() {
		this.strategy.clearText();
		this.clearImageCache();
	}
	
	public void setLinkColor(int color) {
		this.childView.setLinkTextColor(color);
	}
	
	public void setVerticalMargin(int verticalMargin) {
		if ( verticalMargin != this.verticalMargin ) {
			this.verticalMargin = verticalMargin;
			setPadding(this.horizontalMargin, this.verticalMargin, this.horizontalMargin, this.verticalMargin);
			if ( strategy != null ) {
				strategy.updatePosition();
			}
		}		
	}	
	
	public int getHorizontalMargin() {
		return horizontalMargin;
	}
	
	public int getVerticalMargin() {
		return verticalMargin;
	}
	
	public int getSelectionStart() {
		return childView.getSelectionStart();
	}
	
	public int getSelectionEnd() {
		return childView.getSelectionEnd();
	}
	
	public String getSelectedText() {
		return childView.getText().subSequence(
				getSelectionStart(), getSelectionEnd() ).toString();
	}
	
	public void goBackInHistory() {
		
		if ( this.prevIndex == this.getIndex() ) {
			strategy.setPosition(prevPos);
			
			this.storedAnchor = null;
			this.prevIndex = -1;
			this.prevPos = -1;

			restorePosition();
			
		} else {		
			this.strategy.clearText();
			this.spine.navigateByIndex( this.prevIndex );
			strategy.setPosition(this.prevPos);
		
			this.storedAnchor = null;
			this.prevIndex = -1;
			this.prevPos = -1;
		
			loadText();
		}
	}
	
	public void clear() {
		this.childView.setText("");
		this.anchors.clear();
		this.storedAnchor = null;
		this.storedIndex = -1;		
		this.book = null;
		this.fileName = null;
		
		this.strategy.reset();
	}
	
	/**
	 * Loads the text and saves the restored position.
	 */
	public void restore() {
		strategy.clearText();
		loadText();
	}
	
	public void setIndex(int index) {
		this.storedIndex = index;
	}
	
	void loadText() {		
        new LoadTextTask().execute();        
	}
	
	private void loadText(List<SearchTextTask.SearchResult> hightListResults ) {
		new LoadTextTask(hightListResults).execute();
	}
	
	public void setFontFamily(FontFamily family) {
		this.childView.setTypeface( family.getDefaultTypeface() );		
		this.tableHandler.setTypeFace(family.getDefaultTypeface());
		
		this.spanner.setFontFamily(family);
	}	
	
	public void pageDown() {		
		strategy.pageDown();
		progressUpdate();
	}
	
	public void pageUp() {
		strategy.pageUp();
		progressUpdate();
	}	
	
	TextView getInnerView() {
		return childView;
	}
	
	PageTurnerSpine getSpine() {
		return this.spine;
	}		
	
	private Integer findOffsetForPosition(float x, float y) {
		
		if ( childView == null || childView.getLayout() == null ) {
			return null;
		}				
		
		Layout layout = this.childView.getLayout();
		int line = layout.getLineForVertical( (int) y);
		
		return layout.getOffsetForHorizontal(line, x);
	}
	
	/**
	 * Returns the full word containing the character at the selected location.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public CharSequence getWordAt( float x, float y ) {		
		
		if ( childView == null ) {
			return null;
		}
		
		CharSequence text = this.childView.getText();
		
		if ( text.length() == 0 ) {
			return null;
		}
		
		Integer offset = findOffsetForPosition(x, y);
		
		if ( offset == null ) {
			return null;
		}
		
		if ( offset < 0 || offset > text.length() -1 ) {
			return null;
		}		
		
		if ( isBoundaryCharacter(text.charAt(offset)) ) {
			return null;
		}
		
		int left = Math.max(0,offset -1);
		int right = Math.min( text.length(), offset );
		
		CharSequence word = text.subSequence(left, right);
		while ( left > 0 && ! isBoundaryCharacter(word.charAt(0))) {
			left--;
			word = text.subSequence(left, right);
		}
		
		if ( word.length() == 0 ) {
			return null;
		}
		
		while ( right < text.length() && ! isBoundaryCharacter(word.charAt(word.length() -1))) {
			right++;
			word = text.subSequence(left, right);
		}
		
		int start = 0;
		int end = word.length();
		
		if ( isBoundaryCharacter(word.charAt(0))) {
			start = 1;
		}
		
		if ( isBoundaryCharacter(word.charAt(word.length() - 1))) {
			end = word.length() - 1;
		}
		
		if ( start > 0 && start < word.length() && end < word.length() ) {
			return word.subSequence(start, end );
		} 
		
		return null;
	}
	
	private static boolean isBoundaryCharacter( char c ) {
		char[] boundaryChars = { ' ', '.', ',','\"',
				'\'', '\n', '\t', ':', '!','\''
		};
		
		for ( int i=0; i < boundaryChars.length; i++ ) {
			if (boundaryChars[i] == c) {
				return true;
			}		
		}
		
		return false;
	}
	
	public void navigateTo( String rawHref ) {
				
		this.prevIndex = this.getIndex();
		this.prevPos = this.getPosition();
		
		//URLDecode the href, so it does not contain %20 etc.
		String href = URLDecoder.decode( 
			StringUtil.substringBefore(rawHref, 
			Constants.FRAGMENT_SEPARATOR_CHAR) );
		
		//Don't decode the anchor.
		String anchor = StringUtil.substringAfterLast(rawHref, 
				Constants.FRAGMENT_SEPARATOR_CHAR); 
		
		if ( ! "".equals(anchor) ) {
			this.storedAnchor = anchor;
		}		
		
		//Just an anchor and no href; resolve it on this page
		if ( href.length() == 0 ){
			restorePosition();
		} else {

			this.strategy.clearText();
			this.strategy.setPosition(0);
			
			if (this.spine.navigateByHref(href) ) {		
				loadText();
			} else {			
				new LoadTextTask().execute(href);
			}
		}
	}	
	
	public void navigateToPercentage( int percentage ) {
		
		if ( spine == null ) {
			return;
		}
		
		double targetPoint = (double) percentage / 100d;
		List<Double> percentages = this.spine.getRelativeSizes();
				
		if ( percentages == null || percentages.isEmpty() ) {
			return;
		}
		
		int index = 0;		
		double total = 0;
		
		for ( ; total < targetPoint && index < percentages.size() ; index++ ) {
			total = total + percentages.get(index);
		}
		
		index--;
		
		//Work-around for when we get multiple events.
		if ( index < 0 || index >= percentages.size() ) {
			return;
		}
		
		double partBefore = total - percentages.get(index);
		double progressInPart = (targetPoint - partBefore) / percentages.get(index);		
		
		this.prevPos = this.getPosition();
		this.strategy.setRelativePosition(progressInPart);
		
		doNavigation(index);
	}	
	
	public void navigateBySearchResult( List<SearchTextTask.SearchResult> result, int selectedResultIndex  ) {
		SearchTextTask.SearchResult searchResult = result.get(selectedResultIndex);
		//navigateTo(progress.getIndex(), progress.getOffset() );
		
		this.prevPos = this.getPosition();
		this.strategy.setPosition(searchResult.getStart());
		
		this.prevIndex = this.getIndex();
		
		this.storedIndex = searchResult.getIndex();
		this.strategy.clearText();
		this.spine.navigateByIndex(searchResult.getIndex());

		loadText(result);
	}
	
	private void doNavigation( int index ) {

		//Check if we're already in the right part of the book
		if ( index == this.getIndex() ) {
			restorePosition();
			return;
		} 
		
		this.prevIndex = this.getIndex();
		
		this.storedIndex = index;
		this.strategy.clearText();
		this.spine.navigateByIndex(index);

		loadText();
	}
	
	public void navigateTo( int index, int position ) {
		
		this.prevPos = this.getPosition();
		this.strategy.setPosition(position);
		
		doNavigation(index);
	}
	
	public List<TocEntry> getTableOfContents() {
		if ( this.book == null ) {
			return null;
		}
		
		List<TocEntry> result = new ArrayList<BookView.TocEntry>();
		
		flatten( book.getTableOfContents().getTocReferences(), result, 0 );
		
		return result;
	}
	
	private void flatten( List<TOCReference> refs, List<TocEntry> entries, int level ) {
		
		if ( refs == null || refs.isEmpty() ) {
			return;
		}		
		
		for ( TOCReference ref: refs ) {
			
			String title = "";
			
			for ( int i = 0; i < level; i ++ ) {
				title += "-";
			}			
			
			title += ref.getTitle();
			
			if ( ref.getResource() != null ) {
				entries.add( new TocEntry(title, spine.resolveTocHref( ref.getCompleteHref() )));
			}
			
			flatten( ref.getChildren(), entries, level + 1 );
		}
	}
	
	@Override
	public void fling(int velocityY) {
		strategy.clearStoredPosition();
		super.fling(velocityY);
	}
	
	public int getIndex() {
		if ( this.spine == null ) {
			return storedIndex;
		}
		
		return this.spine.getPosition();
	}	
	
	public int getPosition() {
		return strategy.getPosition();		
	}
	
	public void setPosition(int pos) {
		this.strategy.setPosition(pos);	
	}
	
	/**
	 * Scrolls to a previously stored point.
	 * 
	 * Call this after setPosition() to actually go there.
	 */
	private void restorePosition() {				
	
		if ( this.storedAnchor != null && this.anchors.containsKey(storedAnchor) ) {
			strategy.setPosition( anchors.get(storedAnchor) );
			this.storedAnchor = null;
		}
		
		this.strategy.updatePosition();
	}
	
	/**
	 * Many books use <p> and <h1> tags as anchor points.
	 * This class harvests those point by wrapping the original
	 * handler.
	 * 
	 * @author Alex Kuiper
	 *
	 */
	private class AnchorHandler extends TagNodeHandler {
		
		private TagNodeHandler wrappedHandler;
		
		public AnchorHandler(TagNodeHandler wrappedHandler) {
			this.wrappedHandler = wrappedHandler;
		}
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			String id = node.getAttributeByName("id");
			if ( id != null ) {
				anchors.put(id, start);
			}
			
			wrappedHandler.handleTagNode(node, builder, start, end);
		}
	}
	
	/**
	 * Creates clickable links.
	 * 
	 * @author work
	 *
	 */
	private class LinkTagHandler extends TagNodeHandler {
		
		private List<String> externalProtocols;
		
		public LinkTagHandler() {
			this.externalProtocols = new ArrayList<String>();
			externalProtocols.add("http://");
			externalProtocols.add("epub://");
			externalProtocols.add("https://");
			externalProtocols.add("http://");
			externalProtocols.add("ftp://");
			externalProtocols.add("mailto:");
		}
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			String href = node.getAttributeByName("href");
			
			if ( href == null ) {
				return;
			}			
			
			final String linkHref = href;
			
			//First check if it should be a normal URL link
			for ( String protocol: this.externalProtocols ) {
				if ( href.toLowerCase(Locale.US).startsWith(protocol)) {
					builder.setSpan(new URLSpan(href), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					return;
				}
			}
			
			//If not, consider it an internal nav link.			
			ClickableSpan span = new ClickableSpan() {
					
				@Override
				public void onClick(View widget) {
					navigateTo(spine.resolveHref(linkHref));					
				}
			};
				
			builder.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);			 
		}
	}
	
	private void setImageSpan( SpannableStringBuilder builder, Drawable drawable, int start, int end ) {
		builder.setSpan( new ImageSpan(drawable), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		if ( spine.isCover() ) {
			builder.setSpan(new CenterSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}
	
	private class ImageCallback implements ResourceCallback {
		
		private SpannableStringBuilder builder;
		private int start;
		private int end;
		
		private String storedHref;
		
		public ImageCallback(String href, SpannableStringBuilder builder, int start, int end) {
			this.builder = builder;
			this.start = start;
			this.end = end;
			this.storedHref = href;
		}
		
		@Override
		public void onLoadResource(String href, InputStream input) {
			
			Bitmap bitmap = null;
			try {				
				bitmap = getBitmap(input);
				
				if ( bitmap == null || bitmap.getHeight() <1 || bitmap.getWidth() < 1 ) {
					return;
				}
				
			} catch (OutOfMemoryError outofmem) {
				LOG.error("Could not load image", outofmem);
				clearImageCache();
			}
			
			if ( bitmap != null ) {
				FastBitmapDrawable drawable = new FastBitmapDrawable( bitmap );				
				drawable.setBounds(0,0, bitmap.getWidth() - 1, bitmap.getHeight() - 1);
				setImageSpan(builder, drawable, start, end);
				
				LOG.debug("Storing image in cache: " + storedHref );
				imageCache.put(storedHref, drawable);
			}
						
		}
		
		
		
		private Bitmap getBitmap(InputStream input) {
			
			//BitmapDrawable draw = new BitmapDrawable(getResources(), input);
			Bitmap originalBitmap = BitmapFactory.decodeStream(input);
			
			int screenHeight = getHeight() - ( verticalMargin * 2);
			int screenWidth = getWidth() - ( horizontalMargin * 2 );
			
			if ( originalBitmap != null ) {
				int originalWidth = originalBitmap.getWidth();
				int originalHeight = originalBitmap.getHeight();
				
				//We scale to screen width for the cover or if the image is too wide.
				if ( originalWidth > screenWidth || originalHeight > screenHeight || spine.isCover() ) {
					
					float ratio = (float) originalWidth / (float) originalHeight;
					
					int targetHeight = screenHeight - 1;
					int targetWidth = (int)(targetHeight * ratio);					
					
					if ( targetWidth > screenWidth - 1 ) {
						targetWidth = screenWidth - 1;
						targetHeight = (int) (targetWidth * (1/ratio));
					}				
					
					LOG.debug("Rescaling from " + originalWidth + "x" + originalHeight + " to " + targetWidth + "x" + targetHeight );
					
					if ( targetWidth <= 0 || targetHeight <= 0 ) {
						return null;
					}
					
					//android.graphics.Bitmap.createScaledBitmap should do the same.					
					return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
				}									
			}
			
			return originalBitmap;
		}		
	}
	
	private class ImageTagHandler extends TagNodeHandler {
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {						
			String src = node.getAttributeByName("src");
			
			if ( src == null ) {
				src = node.getAttributeByName("href");
			} 
			
			if ( src == null ) {
				src = node.getAttributeByName("xlink:href");
			}
	        builder.append("\uFFFC");
	        
	        String resolvedHref = spine.resolveHref(src);
	        
	        if ( imageCache.containsKey(resolvedHref) ) {
	        	Drawable drawable = imageCache.get(resolvedHref);
	        	setImageSpan(builder, drawable, start, builder.length());
	        	LOG.debug("Got cached href: " + resolvedHref );
	        } else {
	        	LOG.debug("Loading href: " + resolvedHref );
	        	loader.registerCallback(resolvedHref, 
	        		new ImageCallback(resolvedHref, builder, start, builder.length()));
	        }
		}				
		
	}
	
	@Override
	public void setBackgroundColor(int color) {
		super.setBackgroundColor(color);
		
		if ( this.childView != null ) {
			this.childView.setBackgroundColor(color);
		}		
	}
	
	public void setTextColor( int color ) {
		if ( this.childView != null ) {
			this.childView.setTextColor(color);
		}
		
		this.tableHandler.setTextColor(color);
	}
	
	public static class TocEntry {
		private String title;
		private String href;
		
		public TocEntry(String title, String href) {
			this.title = title;
			this.href = href;
		}
		
		public String getHref() {
			return href;
		}
		
		public String getTitle() {
			return title;
		}
	}	
	
	/**
	 * Sets the given text to be displayed, overriding the book.
	 * 
	 * @param text
	 */
	public void setText(Spanned text) {
		this.strategy.loadText(text);
		this.strategy.updatePosition();
	}
	
	public Book getBook() {
		return book;
	}
	
	public float getTextSize() {
		return childView.getTextSize();
	}
	
	public void setTextSize(float textSize) {
		this.childView.setTextSize(textSize);
		this.tableHandler.setTextSize(textSize);
	}
	
	public void addListener(BookViewListener listener) {
		this.listeners.add( listener );
	}
	
	private void bookOpened( Book book ) {
		for ( BookViewListener listener: this.listeners ) {
			listener.bookOpened(book);
		}
	}	
	
	private void errorOnBookOpening( String errorMessage ) {
		for ( BookViewListener listener: this.listeners ) {
			listener.errorOnBookOpening(errorMessage);
		}
	}	 
	
	private void parseEntryStart( int entry) {
		for ( BookViewListener listener: this.listeners ) {
			listener.parseEntryStart(entry);
		}
	}	
	
	private void parseEntryComplete( int entry, String name ) {
		for ( BookViewListener listener: this.listeners ) {
			listener.parseEntryComplete(entry, name);
		}
	}
	
	private void fireCalculatinPageNumbers() {
		for ( BookViewListener listener: this.listeners ) {
			listener.calculatingPageNumbers();
		}
	}
	
	private void fireOpenFile() {
		for ( BookViewListener listener: this.listeners ) {
			listener.readingFile();
		}
	}
	
	private void fireRenderingText() {
		for ( BookViewListener listener: this.listeners ) {
			listener.renderingText();
		}
	}
	
	private void progressUpdate() {		
		
		if ( this.spine != null && this.strategy.getText() != null && this.strategy.getText().length() > 0) {
			
			double progressInPart = (double) this.getPosition() / (double) this.strategy.getText().length(); 
			
			if ( strategy.getText().length() > 0 && strategy.isAtEnd() ) {
				progressInPart = 1d;
			}			
			
			int progress = spine.getProgressPercentage( progressInPart );
		
			if ( progress != -1 ) {
				
				int pageNumber = spine.getPageNumberFor(getIndex(), getPosition() );
				
				for ( BookViewListener listener: this.listeners ) {
					listener.progressUpdate(progress, pageNumber, spine.getTotalNumberOfPages() );
				}		
			}
		}
	}
	
	
	public void setEnableScrolling(boolean enableScrolling) {
		
		if ( this.strategy == null || this.strategy.isScrolling() != enableScrolling ) {

			int pos = -1;
			boolean wasNull = true;
			
			Spanned text = null;
			
			if ( this.strategy != null ) {
				pos = this.strategy.getPosition();
				text = this.strategy.getText();
				this.strategy.clearText();
				wasNull = false;
			}			

			if ( enableScrolling ) {
				this.strategy = new ScrollingStrategy(this, this.getContext());
			} else {				
				this.strategy = new FixedPagesStrategy(this);
			}

			if ( ! wasNull ) {				
				this.strategy.setPosition( pos );				 
			}
			
			if ( text != null && text.length() > 0 ) {
				this.strategy.loadText(text);
			}
		}
	}	
	
	private List<Integer> getOffsetsForResource( Resource res ) throws IOException { 		
		CharSequence text = spanner.fromHtml( res.getReader() );
		loader.load();
		
		return FixedPagesStrategy.getPageOffsets(this, text);			
	}
	
	
	public static class InnerView extends TextView {
		
		private BookView bookView;
		
		public InnerView(Context context, AttributeSet attributes) {
			super(context, attributes);
		}
		
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			bookView.onInnerViewResize();			
		}
		
		public boolean dispatchKeyEvent(KeyEvent event) {
			return bookView.dispatchKeyEvent(event);
		}
		
		public void setBookView(BookView bookView) {
			this.bookView = bookView;
		}
	}
	
	
	private static enum BookReadPhase { START, OPEN_FILE, CALC_PAGE_NUM, PARSE_TEXT, DONE };
	
	private class LoadTextTask extends AsyncTask<String, BookReadPhase, Spanned> {		
		
		private String name;		
		
		private boolean wasBookLoaded;
		
		private String error;
		
		private List<SearchTextTask.SearchResult> searchResults = new ArrayList<SearchTextTask.SearchResult>();
		
		public LoadTextTask() {
		
		}
		
		LoadTextTask(List<SearchTextTask.SearchResult> searchResults ) {
			this.searchResults = searchResults;
		}		
		
		@Override
		protected void onPreExecute() {
			this.wasBookLoaded = book != null;
			clearImageCache();
		}
		
		private void setBook( Book book ) {
			
			BookView.this.book = book;
			BookView.this.spine = new PageTurnerSpine(book);	   
			
			String file = StringUtil.substringAfterLast(fileName, 
					'/' );
			
			BookView.this.spine.navigateByIndex( BookView.this.storedIndex );	
		    
			if ( configuration.isShowPageNumbers() ) {
				
				List<List<Integer>> offsets = configuration.getPageOffsets(file);

				if ( offsets == null || offsets.isEmpty() ) {

					publishProgress(BookReadPhase.CALC_PAGE_NUM);

					try {
						offsets = new ArrayList<List<Integer>>();	    

						for ( int i=0; i < spine.size(); i++ ) {	    	
							offsets.add( getOffsetsForResource(spine.getResourceForIndex(i)));
							clearImageCache();
						}

						configuration.setPageOffsets(file, offsets);

					} catch (IOException io) {
						LOG.error("Could not read pagenumers", io );
					}

				}

				spine.setPageOffsets(offsets);
			}
		    	    
		}	
		
		private void initBook() throws IOException {	
			
			publishProgress(BookReadPhase.OPEN_FILE);
			
			if ( BookView.this.fileName == null ) {
				throw new IOException("No file-name specified.");
			}
							
			// read epub file
	        EpubReader epubReader = new EpubReader();	
	       
	        MediaType[] lazyTypes = {
	        		MediatypeService.CSS, //We don't support CSS yet 
	        		
	        		MediatypeService.GIF, MediatypeService.JPG,
	        		MediatypeService.PNG, MediatypeService.SVG, //Handled by the ResourceLoader
	        		
	        		MediatypeService.OPENTYPE, MediatypeService.TTF, //We don't support custom fonts either
	        		MediatypeService.XPGT,
	        		
	        		MediatypeService.MP3, MediatypeService.MP4, //And no audio either
	        		MediatypeService.SMIL, MediatypeService.XPGT,
	        		MediatypeService.PLS
	        };
	        
	       	Book newBook = epubReader.readEpubLazy(fileName, "UTF-8", Arrays.asList(lazyTypes));
	        setBook( newBook );	        
	        
		}	
		
		protected Spanned doInBackground(String...hrefs) {	
			
			publishProgress(BookReadPhase.START);
			
			if ( loader != null ) {
				loader.clear();
			}
			
			if ( BookView.this.book == null ) {
				try {
					initBook();
				} catch (IOException io ) {
					this.error = io.getMessage();
					return null;
				}
			}			
									
			this.name = spine.getCurrentTitle();	
						
			Resource resource;
			
			if ( hrefs.length == 0 ) {
				resource = spine.getCurrentResource();
			} else {
				resource = book.getResources().getByHref(hrefs[0]);
			}
			
			if ( resource == null ) {
				return new SpannedString("Sorry, it looks like you clicked a dead link.\nEven books have 404s these days." );
			}			
			
			publishProgress(BookReadPhase.PARSE_TEXT);
			
			try {
				Spannable result = spanner.fromHtml(resource.getReader());
				loader.load(); //Load all image resources.
				
				//Highlight search results (if any)
				for ( SearchTextTask.SearchResult searchResult: this.searchResults ) {
					if ( searchResult.getIndex() == spine.getPosition() ) {
						result.setSpan(new BackgroundColorSpan(Color.YELLOW),
								searchResult.getStart(), searchResult.getEnd(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );								
					}			
				}
				
				publishProgress(BookReadPhase.DONE);
				
				return result;
			} catch (IOException io ) {
				return new SpannableString( "Could not load text: " + io.getMessage() );
			}			
	        
			
		}
		
		@Override
		protected void onProgressUpdate(BookReadPhase... values) {
			
			BookReadPhase phase = values[0];
			
			switch (phase) {
			case START:
				parseEntryStart(getIndex());
				break;
			case OPEN_FILE:
				fireOpenFile();				
				break;
			case CALC_PAGE_NUM:
				fireCalculatinPageNumbers();
				break;
			case PARSE_TEXT:
				fireRenderingText();
				break;				
			case DONE:
				parseEntryComplete(spine.getPosition(), this.name);
				break;
			}
		}		
		
		@Override
		protected void onPostExecute(final Spanned result) {
			
			if ( ! wasBookLoaded ) {
				if ( book != null ) {
					bookOpened(book);		
				} else {
					errorOnBookOpening(this.error);
					return;
				}
			}			
			
			restorePosition();
			strategy.loadText( result );
			progressUpdate();
		}
	}	
}
