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


package net.nightwhistler.pageturner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import net.nightwhistler.pageturner.html.CleanHtmlParser;
import net.nightwhistler.pageturner.html.TagNodeHandler;
import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.StringUtil;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;


public class BookView extends ScrollView {

    private static final int PADDING = 15;    
    
	private int storedPosition;
	private int storedIndex;
	private String storedAnchor;
	
	private boolean enableScrolling;
	
	private TextView childView;
	
	private Set<BookViewListener> listeners;
	
	private HtmlCleaner htmlCleaner;
	private CleanHtmlParser parser;
	
	private OnTouchListener touchListener;
	
	private PageTurnerSpine spine;
	
	private String fileName;
	private Book book;	
	
	private Map<String, Integer> anchors;
	
	private int prevIndex = -1;
	private int prevPos = -1;
	
	public BookView(Context context, AttributeSet attributes) {
		super(context, attributes);		
		
		this.listeners = new HashSet<BookViewListener>();
				
		this.childView = new TextView(context) {
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				super.onSizeChanged(w, h, oldw, oldh);
				restorePosition();	
			}
			
			public boolean dispatchKeyEvent(KeyEvent event) {
				return BookView.this.dispatchKeyEvent(event);
			}
		};  
		
        this.setPadding(PADDING, PADDING, PADDING, PADDING);
        this.setBackgroundColor(Color.WHITE);
       
        this.setVerticalFadingEdgeEnabled(false);
        childView.setTextColor( Color.BLACK );
        childView.setFocusable(true);
        childView.setLinksClickable(true);
        
        MovementMethod m = childView.getMovementMethod();  
        if ((m == null) || !(m instanceof LinkMovementMethod)) {  
            if (childView.getLinksClickable()) {  
                childView.setMovementMethod(LinkMovementMethod.getInstance());  
            }  
        }  
        
        this.setSmoothScrollingEnabled(false);        
        this.addView(childView);
        
        this.htmlCleaner = createHtmlCleaner();   
        this.parser = new CleanHtmlParser();
        parser.registerHandler("img", new ImageTagHandler() );
        parser.registerHandler("a", new AnchorHandler(new LinkTagHandler()) );
        
        parser.registerHandler("h1", new AnchorHandler(parser.getHandlerFor("h1") ));
        parser.registerHandler("h2", new AnchorHandler(parser.getHandlerFor("h2") ));
        parser.registerHandler("h3", new AnchorHandler(parser.getHandlerFor("h3") ));
        parser.registerHandler("h4", new AnchorHandler(parser.getHandlerFor("h4") ));
        parser.registerHandler("h5", new AnchorHandler(parser.getHandlerFor("h5") ));
        parser.registerHandler("h6", new AnchorHandler(parser.getHandlerFor("h6") ));
        
        parser.registerHandler("p", new AnchorHandler(parser.getHandlerFor("p") ));
        
        this.anchors = new HashMap<String, Integer>();
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	@Override
	public void setOnTouchListener(OnTouchListener l) {		
		super.setOnTouchListener(l);
		this.childView.setOnTouchListener(l);
		this.touchListener = l;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		
		if ( this.touchListener != null ) {
			this.touchListener.onTouch(this, ev);
		}
		
		if ( enableScrolling ) {
			return super.onTouchEvent(ev);
		}
		
		return true;			
	}	
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if ( ! enableScrolling ) {
			//Consume key up and down
			if ( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP
					|| event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN ) {
				return true;
			}
		}		
		
		return super.dispatchKeyEvent(event);
	}
	
	public boolean hasPrevPosition() {
		return this.prevIndex != -1 && this.prevPos != -1;
	}
	
	public void goBackInHistory() {
		
		this.spine.navigateByIndex( this.prevIndex );
		this.storedPosition = prevPos;
		
		this.storedAnchor = null;
		this.prevIndex = -1;
		this.prevPos = -1;
		
		loadText();
	}
	
	public void clear() {
		this.childView.setText("");
		this.anchors.clear();
		this.storedAnchor = null;
		this.storedIndex = -1;
		this.storedPosition = -1;
		this.book = null;
		this.fileName = null;
	}
	
	/**
	 * Loads the text and saves the restored position.
	 */
	public void restore() {
		loadText();
	}
	
	public void setIndex(int index) {
		this.storedIndex = index;
	}
	
	private void loadText() {		
        new LoadTextTask().execute();        
	}
	
	public void setTypeface(Typeface typeFace) {
		this.childView.setTypeface( typeFace );
	}	
	
	public void pageDown() {		
		this.scroll( getHeight() - 2 * PADDING);		
	}
	
	public void pageUp() {
		this.scroll( (getHeight() - 2* PADDING ) * -1);		
	}
	
	@Override
	public void scrollBy(int x, int y) {		
		super.scrollBy(x, y);
		
		progressUpdate();
	}
	
	@Override
	public void scrollTo(int x, int y) {		
		super.scrollTo(x, y);
		progressUpdate();
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
		
		this.storedPosition = 0;
		
		if ( this.spine.navigateByHref(href) ) {
			loadText();
		} else {			
			new LoadTextTask().execute(href);
		}
	}
	
	private void scroll( int delta ) {
		
		int currentPos = this.getScrollY();
		
		int newPos = currentPos + delta;
		
		this.scrollTo(0, findClosestLineBottom(newPos));
		
		if ( this.getScrollY() == currentPos ) {
						
			if ( delta < 0 ) {				
				if (! spine.navigateBack() ) {					
					return;
				}
			} else {				
				if ( ! spine.navigateForward() ) {
					return;
				}
			}
			
			this.childView.setText("");
			
			if ( delta > 0 ) {
				scrollTo(0,0);
				this.storedPosition = -1;
			} else {
				scrollTo(0, getHeight());
				
				//We scrolled back up, so we want the very bottom of the text.
				this.storedPosition = Integer.MAX_VALUE;
			}	
			
			loadText();
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
				entries.add( new TocEntry(title, ref.getCompleteHref() ));
			}
			
			flatten( ref.getChildren(), entries, level + 1 );
		}
	}
	
	@Override
	public void fling(int velocityY) {
		storedPosition = -1;
		super.fling(velocityY);
	}
	
	public int getIndex() {
		if ( this.spine == null ) {
			return storedIndex;
		}
		
		return this.spine.getPosition();
	}	
	
	public int getPosition() {
			
		int yPos = this.getScrollY();
						
		return findTextOffset(findClosestLineBottom(yPos));
	}
	
	public void setPosition(int pos) {
		this.storedPosition = pos;	
	}
	
	/**
	 * Scrolls to a previously stored point.
	 * 
	 * Call this after setPosition() to actually go there.
	 */
	private void restorePosition() {				
	
		if ( this.storedAnchor != null && this.anchors.containsKey(storedAnchor) ) {
			this.storedPosition = anchors.get(storedAnchor);
			this.storedAnchor = null;
		}
		
		if ( this.storedPosition == -1 || "".equals( this.childView.getText() )) {
			return; //Hopefully come back later
		} else {
			
			Layout layout = this.childView.getLayout();
			
			if ( layout != null ) {
				
				int line = layout.getLineForOffset(this.storedPosition);
				
				if ( line > 0 ) {
					int newPos = layout.getLineBottom(line -1);
					scrollTo(0, newPos);
				} else {
					scrollTo(0, 0);
				}
			}						 
		}
		
	}
	
	/**
	 * Many books use <p> and <h1> tags as anchor points.
	 * This class harvests those point by wrapping the original
	 * handler.
	 * 
	 * @author Alex Kuiper
	 *
	 */
	private class AnchorHandler implements TagNodeHandler {
		
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
	private class LinkTagHandler implements TagNodeHandler {
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			final String href = node.getAttributeByName("href");
			if ( href != null ) {			
				
				ClickableSpan span = new ClickableSpan() {
					
					@Override
					public void onClick(View widget) {
						navigateTo(href);					
					}
				};
				
				builder.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} 
		}
	}
	
	private class ImageTagHandler implements TagNodeHandler {
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {						
			String src = node.getAttributeByName("src");
			
	        builder.append("\uFFFC");
	        
	        Drawable drawable = getDrawable(src);
			
	        if ( drawable != null ) {
				builder.setSpan( new ImageSpan(drawable), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}		
		
		private Drawable getDrawable(String source) {
			
			Resource imgRes = book.getResources().getByHref(source);
			
			BitmapDrawable draw = null;
			
			try {
				if ( imgRes != null ) {					
					draw = new BitmapDrawable(getResources(), imgRes.getInputStream() );
				} 
			} catch (IOException io) {
				//Just leave draw null
			}
			
			if ( draw != null ) {
				int targetWidth = draw.getBitmap().getWidth();
				int targetHeight = draw.getBitmap().getHeight();

				//We scale to screen width for the cover or if the image is too wide.
				if ( targetWidth > getWidth() || spine.isCover() ) {
					double ratio = (double) draw.getBitmap().getHeight() / (double) draw.getBitmap().getWidth();

					targetWidth = getWidth() - ( PADDING * 2 );
					targetHeight = (int) (targetWidth * ratio);				
				}

				draw.setBounds(0, 0, targetWidth, targetHeight);					
			}
			
			return draw;					
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
	
	private TagNode processHtml(Resource resource, String outputEncoding) throws IOException {		
		return this.htmlCleaner.clean(resource.getReader());
	}
	
	private static HtmlCleaner createHtmlCleaner() {
		HtmlCleaner result = new HtmlCleaner();
		CleanerProperties cleanerProperties = result.getProperties();
		cleanerProperties.setOmitXmlDeclaration(true);
		cleanerProperties.setOmitDoctypeDeclaration(false);
		cleanerProperties.setRecognizeUnicodeChars(true);
		cleanerProperties.setTranslateSpecialEntities(true);
		cleanerProperties.setIgnoreQuestAndExclam(true);
		cleanerProperties.setUseEmptyElementTags(false);
		
		cleanerProperties.setPruneTags("script,style,title");
		
		return result;
	}	
	
	/**
	 * Sets the given text to be displayed, overriding the book.
	 * @param text
	 */
	public void setText(CharSequence text) {
		this.childView.setText(text);
	}
	
	public float getTextSize() {
		return childView.getTextSize();
	}
	
	public void setTextSize(float textSize) {
		this.childView.setTextSize(textSize);
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
	
	private void progressUpdate() {		
		
		if ( this.spine != null ) {
			int progress = spine.getProgressPercentage(this.getPosition() );
		
			if ( progress != -1 ) {
				for ( BookViewListener listener: this.listeners ) {
					listener.progressUpdate(progress);
				}		
			}
		}
	}
	
	public void setEnableScrolling(boolean enableScrolling) {
		this.enableScrolling = enableScrolling;
	}
	
	private void initBook() throws IOException {
		// read epub file
        EpubReader epubReader = new EpubReader();
	
         	
        File file = new File(fileName);        	
	    book = epubReader.readEpub( new FileInputStream(file) );
	    
	    this.spine = new PageTurnerSpine(book);	   
	    this.spine.navigateByIndex( this.storedIndex );
	}	
	
	private class LoadTextTask extends AsyncTask<String, Integer, Spanned> {
		
		private String name;		
		
		private boolean wasBookLoaded;
		
		private String error;
		
		@Override
		protected void onPreExecute() {
			this.wasBookLoaded = book != null;
			parseEntryStart(getIndex());
		}
		
		protected Spanned doInBackground(String...hrefs) {	
			
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
			
			try {
				return parser.fromTagNode( processHtml(resource, "UTF-8") );			
			} catch (IOException io ) {
				return new SpannableString( "Could not load text: " + io.getMessage() );
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
			
			childView.setText( result ); 
			restorePosition();
			
			parseEntryComplete(spine.getPosition(), this.name);
		}
	}	
}
