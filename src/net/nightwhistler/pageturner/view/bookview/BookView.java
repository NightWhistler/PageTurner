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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Handler;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.ActionMode;
import android.view.MotionEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.inject.Inject;
import com.google.inject.Provider;
import jedi.option.None;
import jedi.option.Option;
import jedi.tuple.Tuple2;
import net.nightwhistler.htmlspanner.FontFamily;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.handlers.TableHandler;
import net.nightwhistler.htmlspanner.spans.CenterSpan;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.dto.HighLight;
import net.nightwhistler.pageturner.dto.SearchResult;
import net.nightwhistler.pageturner.dto.TocEntry;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import net.nightwhistler.pageturner.epub.ResourceLoader;
import net.nightwhistler.pageturner.epub.ResourceLoader.ResourceCallback;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import net.nightwhistler.pageturner.scheduling.TaskQueue;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;
import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.StringUtil;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static jedi.functional.FunctionalPrimitives.*;
import static jedi.option.Options.*;

public class BookView extends ScrollView implements TextSelectionActions.SelectedTextProvider {

    private static final Logger LOG = LoggerFactory.getLogger("BookView");

	private int storedIndex;
	private String storedAnchor;

	private InnerView childView;

	private Set<BookViewListener> listeners;

	private TableHandler tableHandler;

	private PageTurnerSpine spine;

	private String fileName;
	private Book book;

	private int prevIndex = -1;
	private int prevPos = -1;

	private PageChangeStrategy strategy;
	private ResourceLoader loader;

	private int horizontalMargin = 0;
	private int verticalMargin = 0;
	private int lineSpacing = 0;

	private Handler scrollHandler;

    @Inject
    private Configuration configuration;

    @Inject
    private TextLoader textLoader;

    @Inject
    private EpubFontResolver fontResolver;

    @Inject
    private TaskQueue taskQueue;

    @Inject
    private Provider<FixedPagesStrategy> fixedPagesStrategyProvider;

    @Inject
    private Provider<ScrollingStrategy> scrollingStrategyProvider;

	public BookView(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.scrollHandler = new Handler();
        RoboGuice.injectMembers(context, this);
	}

    @TargetApi(Configuration.TEXT_SELECTION_PLATFORM_VERSION)
	public void init() {
		this.listeners = new HashSet<>();

		this.childView = (InnerView) this.findViewById(R.id.innerView);
		this.childView.setBookView(this);

		childView.setCursorVisible(false);
		childView.setLongClickable(true);
		this.setVerticalFadingEdgeEnabled(false);
		childView.setFocusable(true);
		childView.setLinksClickable(true);

		if (Build.VERSION.SDK_INT >= Configuration.TEXT_SELECTION_PLATFORM_VERSION ) {
			childView.setTextIsSelectable(true);
		}
		
		this.setSmoothScrollingEnabled(false);
		this.tableHandler = new TableHandler();
        this.textLoader.registerTagNodeHandler("table", tableHandler);

        ImageTagHandler imgHandler = new ImageTagHandler(false);
        this.textLoader.registerTagNodeHandler("img", imgHandler);
        this.textLoader.registerTagNodeHandler("image", imgHandler);

        this.textLoader.setLinkCallBack(this::onLinkClicked);
	}

	private void onInnerViewResize() {
		restorePosition();

		if ( this.tableHandler != null ) {
			int tableWidth = (int) (childView.getWidth() * 0.9);
			tableHandler.setTableWidth(tableWidth);
		}
	}

    public String getFileName() {
        return fileName;
    }

    public void onLinkClicked(String href) {
        navigateTo(spine.resolveHref(href));
    }

    public PageChangeStrategy getStrategy() {
        return this.strategy;
    }

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		progressUpdate();
	}

	/**
	 * Returns if we're at the start of the book, i.e. displaying the title
	 * page.
	 * 
	 * @return
	 */
	public boolean isAtStart() {

		if (spine == null) {
			return true;
		}

		return spine.getPosition() == 0 && strategy.isAtStart();
	}

	public boolean isAtEnd() {
		if (spine == null) {
			return false;
		}

		return spine.getPosition() >= spine.size() - 1 && strategy.isAtEnd();
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


    public List<HighlightSpan> getHighlightsAt( float x, float y ) {
        return getSpansAt(x, y, HighlightSpan.class );
    }

	public List<ClickableSpan> getLinkAt(float x, float y) {
        return getSpansAt(x, y, ClickableSpan.class );
	}

    /**
     * Returns all the spans of a specific class at a specific location.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param spanClass the class of span to filter for
     * @param <A>
     * @return a List of spans of type A, may be empty.
     */
    private <A> List<A> getSpansAt( float x, float y, Class<A> spanClass) {

        Option<Integer> offsetOption = findOffsetForPosition(x, y);

        CharSequence text = childView.getText();

        if ( isEmpty(offsetOption) || ! (text instanceof Spanned) ) {
            return new ArrayList<>();
        }

        int offset = offsetOption.getOrElse(0);

        return asList( ((Spanned) text).getSpans(offset, offset, spanClass));
    }

    /**
     * Blocks the inner-view from creating action-modes for a given amount of time.
     *
     * @param time
     */
    public void blockFor( long time ) {
        this.childView.setBlockUntil( System.currentTimeMillis() + time );
    }

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		if (strategy.isScrolling()) {
			return super.onTouchEvent(ev);
		} else {
			return childView.onTouchEvent(ev);
		}
	}

	public boolean hasPrevPosition() {
		return this.prevIndex != -1 && this.prevPos != -1;
	}

	public void setLineSpacing(int lineSpacing) {
		if (lineSpacing != this.lineSpacing) {
			this.lineSpacing = lineSpacing;
			this.childView.setLineSpacing(lineSpacing, 1);

			if (strategy != null) {
				strategy.updatePosition();
			}
		}
	}

	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setTextSelectionCallback(TextSelectionCallback callback) {
		if (Build.VERSION.SDK_INT >= Configuration.TEXT_SELECTION_PLATFORM_VERSION) {
			this.childView
					.setCustomSelectionActionModeCallback(new TextSelectionActions(
                            getContext(), callback, this));
		}
	}	

	public int getLineSpacing() {
		return lineSpacing;
	}

	public void setHorizontalMargin(int horizontalMargin) {

		if (horizontalMargin != this.horizontalMargin) {
			this.horizontalMargin = horizontalMargin;
			setPadding(this.horizontalMargin, this.verticalMargin,
					this.horizontalMargin, this.verticalMargin);
			if (strategy != null) {
				strategy.updatePosition();
			}
		}
	}

	public void releaseResources() {
		this.strategy.clearText();
		this.textLoader.closeCurrentBook();
        this.taskQueue.clear();
	}

	public void setLinkColor(int color) {
		this.childView.setLinkTextColor(color);
	}

	public void setVerticalMargin(int verticalMargin) {
		if (verticalMargin != this.verticalMargin) {
			this.verticalMargin = verticalMargin;
			setPadding(this.horizontalMargin, this.verticalMargin,
                    this.horizontalMargin, this.verticalMargin);
			if (strategy != null) {
				strategy.updatePosition();
			}
		}
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

	public Option<String> getSelectedText() {

        int start = getSelectionStart();
        int end = getSelectionEnd();

        if ( start > 0 && end > 0 && end > start ) {
            return some(childView.getText()
                    .subSequence(getSelectionStart(), getSelectionEnd()).toString());
        } else {
            return none();
        }
	}

	public void goBackInHistory() {

		if (this.prevIndex == this.getIndex()) {
			strategy.setPosition(prevPos);

			this.storedAnchor = null;
			this.prevIndex = -1;
			this.prevPos = -1;

			restorePosition();

		} else {
			this.strategy.clearText();
			this.spine.navigateByIndex(this.prevIndex);
			strategy.setPosition(this.prevPos);

			this.storedAnchor = null;
			this.prevIndex = -1;
			this.prevPos = -1;

			loadText();
		}
	}

	public void clear() {
		this.childView.setText("");
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

        if ( spine == null && ! textLoader.hasCachedBook( this.fileName ) ) {
            taskQueue.executeTask( new OpenFileTask() );
            taskQueue.executeTask( new LoadTextTask() );
            taskQueue.executeTask( new PreLoadTask(spine, textLoader) );
            taskQueue.executeTask( new CalculatePageNumbersTask() );
        } else {

            if ( spine == null ) {
                try {
                    Book book = initBookAndSpine();

                    if ( book != null ) {
                        bookOpened( book );
                    }

                } catch ( IOException io ) {
                    errorOnBookOpening( io.getMessage() );
                    return;
                } catch ( OutOfMemoryError e ) {
                    errorOnBookOpening( getContext().getString( R.string.out_of_memory ) );
                    return;
                }
            }

            //TODO: what if the resource is None?
            spine.getCurrentResource().forEach( this::loadText );
        }
	}

    private void loadText( Resource resource ) {

        LOG.debug( "Trying to load text for resource " + resource );

        Option<Spannable> cachedText = textLoader.getCachedTextForResource( resource );

        //Start by clearing the queue
        taskQueue.clear();

        if ( ! isEmpty(cachedText) && getInnerView().getWidth() > 0  ) {

            LOG.debug( "Text is cached, loading on UI Thread.");
            loadText(cachedText.unsafeGet());
        } else {

            LOG.debug( "Text is NOT cached, loading in background.");
            taskQueue.executeTask(new LoadTextTask(), resource);
        }

        taskQueue.executeTask( new PreLoadTask(spine, textLoader) );

        if ( needsPageNumberCalculation() ) {
            taskQueue.executeTask( new CalculatePageNumbersTask() );
        }
    }

    private void loadText( Spanned text ) {

        strategy.loadText( text );

        restorePosition();
        strategy.updateGUI();
        progressUpdate();
        parseEntryComplete( spine.getCurrentTitle().getOrElse("") );
    }

	private void loadText( String searchTerm ) {
        spine.getCurrentResource().forEach( res ->
                this.taskQueue.jumpQueueExecuteTask(new LoadTextTask(searchTerm), res ));
	}

    private Book initBookAndSpine() throws IOException {

        Book book = textLoader.initBook(fileName);

        this.book = book;
        this.spine = new PageTurnerSpine(book);

        this.spine.navigateByIndex(BookView.this.storedIndex);

        if (configuration.isShowPageNumbers()) {

            Option<List<List<Integer>>> offsets = configuration
                    .getPageOffsets(fileName);

            offsets.filter( o -> o.size() > 0 ).forEach(
                    o -> spine.setPageOffsets( o ) );
        }

        return book;
    }

    public void setFontFamily(FontFamily family) {
        this.childView.setTypeface(family.getDefaultTypeface());
        this.tableHandler.setTypeFace(family.getDefaultTypeface());
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

	private Option<Integer> findOffsetForPosition(float x, float y) {

		if (childView == null || childView.getLayout() == null) {
			return none();
		}

		Layout layout = this.childView.getLayout();
		int line = layout.getLineForVertical((int) y);

		return option(layout.getOffsetForHorizontal(line, x));
	}

	/**
	 * Returns the full word containing the character at the selected location.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public Option<SelectedWord> getWordAt(float x, float y) {

		if (childView == null) {
			return none();
		}

		CharSequence text = this.childView.getText();

		if (text.length() == 0) {
			return none();
		}

		Option<Integer> offsetOption = findOffsetForPosition(x, y);

		if ( isEmpty( offsetOption )) {
			return none();
		}

        int offset = offsetOption.unsafeGet();

		if (offset < 0 || offset > text.length() - 1 || isBoundaryCharacter(text.charAt(offset))) {
			return none();
		}

		int left = Math.max(0, offset - 1);
		int right = Math.min(text.length(), offset);

		CharSequence word = text.subSequence(left, right);
		while (left > 0 && !isBoundaryCharacter(word.charAt(0))) {
			left--;
			word = text.subSequence(left, right);
		}

		if (word.length() == 0) {
			return none();
		}

		while (right < text.length()
				&& !isBoundaryCharacter(word.charAt(word.length() - 1))) {
			right++;
			word = text.subSequence(left, right);
		}

		int start = 0;
		int end = word.length();

		if (isBoundaryCharacter(word.charAt(0))) {
			start = 1;
            left++;
		}

		if (isBoundaryCharacter(word.charAt(word.length() - 1))) {
			end = word.length() - 1;
            right--;
		}

		if (start > 0 && start < word.length() && end < word.length()) {
            word = word.subSequence(start, end);

            return some(new SelectedWord( left, right, word ));
		}

		return none();
	}

	private static boolean isBoundaryCharacter(char c) {
		char[] boundaryChars = { ' ', '.', ',', '\"', '\'', '\n', '\t', ':',
				'!', '\'' };

		for (int i = 0; i < boundaryChars.length; i++) {
			if (boundaryChars[i] == c) {
				return true;
			}
		}

		return false;
	}

    public void navigateTo( TocEntry tocEntry ) {
        navigateTo( tocEntry.getHref() );
    }

	public void navigateTo(String rawHref) {

		this.prevIndex = this.getIndex();
		this.prevPos = this.getProgressPosition();

        // Default Charset for android is UTF-8
        // http://developer.android.com/reference/java/nio/charset/Charset.html#defaultCharset()
        String charsetName = Charset.defaultCharset().name();

        if (!Charset.isSupported(charsetName)) {
            LOG.warn("{} is not a supported Charset. Will fall back to UTF-8", charsetName);
            charsetName = "UTF-8";
        }

		// URLDecode the href, so it does not contain %20 etc.
        String href;
        try {
            href = URLDecoder.decode(StringUtil.substringBefore(rawHref,
                    Constants.FRAGMENT_SEPARATOR_CHAR), charsetName);

        } catch (UnsupportedEncodingException e) {
            // Won't ever be reached
            throw new AssertionError(e);
        }

        // Don't decode the anchor.
		String anchor = StringUtil.substringAfterLast(rawHref,
				Constants.FRAGMENT_SEPARATOR_CHAR);

		if (!"".equals(anchor)) {
			this.storedAnchor = anchor;
		}

		// Just an anchor and no href; resolve it on this page
		if (href.length() == 0) {
			restorePosition();
		} else {

			this.strategy.clearText();
			this.strategy.setPosition(0);

			if (this.spine.navigateByHref(href)) {
				loadText();
			} else {

                Resource resource = book.getResources().getByHref(href);

                if ( resource != null ) {
                    loadText( resource );
                } else {
                    loadText( new SpannedString( getContext().getString(R.string.dead_link ) ) );
                }
			}
		}
	}

	public void navigateToPercentage(int percentage) {

		if (spine == null) {
			return;
		}
		
		int index = 0;
		
		if ( percentage > 0 ) {

			double targetPoint = (double) percentage / 100d;
			List<Double> percentages = this.spine.getRelativeSizes();

			if (percentages == null || percentages.isEmpty()) {
				return;
			}
			
			double total = 0;

			for (; total < targetPoint && index < percentages.size(); index++) {
				total = total + percentages.get(index);
			}

			index--;

			// Work-around for when we get multiple events.
			if (index < 0 || index >= percentages.size()) {
				return;
			}

			double partBefore = total - percentages.get(index);
			double progressInPart = (targetPoint - partBefore)
					/ percentages.get(index);
			
			this.strategy.setRelativePosition(progressInPart);
		} else {
			
			//Simply jump to titlepage			
			this.strategy.setPosition(0);
		}

		this.prevPos = this.getProgressPosition();
		doNavigation(index);
	}

	public void navigateBySearchResult( SearchResult searchResult ) {

		this.prevPos = this.getProgressPosition();
		this.strategy.setPosition( searchResult.getStart() );

		this.prevIndex = this.getIndex();

		this.storedIndex = searchResult.getIndex();
		this.strategy.clearText();
		this.spine.navigateByIndex(searchResult.getIndex());

		loadText( searchResult.getQuery() );
	}

	private void doNavigation(int index) {

		// Check if we're already in the right part of the book
		if (index == this.getIndex()) {
			restorePosition();
			progressUpdate();
			return;
		}

		this.prevIndex = this.getIndex();

		this.storedIndex = index;
		this.strategy.clearText();
		this.spine.navigateByIndex(index);

		loadText();
	}

	public void navigateTo(int index, int position) {

		this.prevPos = this.getProgressPosition();
		this.strategy.setPosition(position);

		doNavigation(index);
	}

	public Option<List<TocEntry>> getTableOfContents() {

        if ( this.book != null ) {
			List<TocEntry> result = new ArrayList<>();
            flatten(book.getTableOfContents().getTocReferences(), result, 0);
            return some(result);
        } else {
            return none();
        }
	}

	private void flatten(List<TOCReference> refs, List<TocEntry> entries,
			int level) {

		if (spine == null || refs == null || refs.isEmpty()) {
			return;
		}

		for (TOCReference ref : refs) {

			String title = "";

			for (int i = 0; i < level; i++) {
				title += "  ";
			}

			title += ref.getTitle();

			if (ref.getResource() != null) {
				entries.add(new TocEntry(title, spine.resolveTocHref(ref
						.getCompleteHref())));
			}

			flatten(ref.getChildren(), entries, level + 1);
		}
	}


	@Override
	public void fling(int velocityY) {
		strategy.clearStoredPosition();
		super.fling(velocityY);
	}

	public int getIndex() {
		if (this.spine == null) {
			return storedIndex;
		}

		return this.spine.getPosition();
	}

    public int getStartOfCurrentPage() {
        return strategy.getTopLeftPosition();
    }

	public int getProgressPosition() {
		return strategy.getProgressPosition();
	}

	public void setPosition(int pos) {
		this.strategy.setPosition(pos);
	}

    public void update() {
        strategy.updateGUI();
    }

    public String getFirstLine() {
        int topLeft = strategy.getTopLeftPosition();

        String plainText = "";

        if ( getStrategy().getText() != null ) {
            plainText = getStrategy().getText().toString();
        }

        if ( plainText.length() == 0 ) {
            return plainText;
        }

        plainText = plainText.substring( topLeft, plainText.length() ).trim();

        int firstNewLine = plainText.indexOf( '\n' );

        if ( firstNewLine == -1 ) {
            return plainText;
        }

        return plainText.substring(0, firstNewLine);
    }

	/**
	 * Scrolls to a previously stored point.
	 * 
	 * Call this after setPosition() to actually go there.
	 */
	private void restorePosition() {

        if (this.storedAnchor != null  ) {

            spine.getCurrentHref().forEach( href -> {
                Option<Integer> anchorValue = this.textLoader.getAnchor(
                        href, storedAnchor );

                if ( !isEmpty(anchorValue) ) {
                    strategy.setPosition(anchorValue.getOrElse(0));
                    this.storedAnchor = null;
                }
            });
		}

		this.strategy.updatePosition();
	}

	private void setImageSpan(SpannableStringBuilder builder,
			Drawable drawable, int start, int end) {
		builder.setSpan(new ImageSpan(drawable), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		if (spine != null && spine.isCover()) {
			builder.setSpan(new CenterSpan(), start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	private class ImageCallback implements ResourceCallback {

		private SpannableStringBuilder builder;
		private int start;
		private int end;

		private String storedHref;
		
		private boolean fakeImages;

		public ImageCallback(String href, SpannableStringBuilder builder,
				int start, int end, boolean fakeImages) {
			this.builder = builder;
			this.start = start;
			this.end = end;
			this.storedHref = href;
			this.fakeImages = fakeImages;
		}

		@Override
		public void onLoadResource(String href, InputStream input) {

			if ( fakeImages ) {
				LOG.debug("Faking image for href: " + href);
				setFakeImage(input);
			} else {
				LOG.debug("Loading real image for href: " + href);
				setBitmapDrawable(input);
			}

		}
		
		private void setFakeImage(InputStream input) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(input, null, options);
			
			Tuple2<Integer, Integer> sizes = calculateSize(options.outWidth, options.outHeight );
			
			ShapeDrawable draw = new ShapeDrawable( new RectShape() );
			draw.setBounds(0, 0, sizes.a(), sizes.b());
			
			setImageSpan(builder, draw, start, end);
		}
		
		private void setBitmapDrawable(InputStream input) {
			Bitmap bitmap = null;
			try {
				bitmap = getBitmap(input);

				if (bitmap == null || bitmap.getHeight() < 1
						|| bitmap.getWidth() < 1) {
					return;
				}

			} catch (OutOfMemoryError outofmem) {
				LOG.error("Could not load image", outofmem);
			}

			if (bitmap != null) {
				
				FastBitmapDrawable drawable = new FastBitmapDrawable(bitmap);
				
				drawable.setBounds(0, 0, bitmap.getWidth() - 1,
						bitmap.getHeight() - 1);
				setImageSpan(builder, drawable, start, end);

				LOG.debug("Storing image in cache: " + storedHref);

                textLoader.storeImageInChache(storedHref, drawable);
			}
		}		
		

		private Bitmap getBitmap(InputStream input) {
			if(Configuration.IS_NOOK_TOUCH) {
				// Workaround for skia failing to load larger (>8k?) JPEGs on Nook Touch and maybe other older Eclair devices (unknown!)
				// seems the underlying problem is the ZipInputStream returning data in chunks,
				// may be as per http://code.google.com/p/android/issues/detail?id=6066
				// workaround is to stream the whole image out of the Zip to a ByteArray, then pass that on to the bitmap decoder
				try {
					input = new ByteArrayInputStream(IOUtil.toByteArray(input));
				} catch(IOException ex) {
					LOG.error("Failed to extract full image from epub stream: " + ex.toString());
				}
			}

			Bitmap originalBitmap = BitmapFactory.decodeStream(input);

			if (originalBitmap != null) {
				int originalWidth = originalBitmap.getWidth();
				int originalHeight = originalBitmap.getHeight();

				Tuple2<Integer, Integer> targetSizes = calculateSize(originalWidth, originalHeight);
				int targetWidth = targetSizes.a();
				int targetHeight = targetSizes.b();
				
				if ( targetHeight != originalHeight || targetWidth != originalWidth ) {					
					return Bitmap.createScaledBitmap(originalBitmap,
							targetWidth, targetHeight, true);
				}
			}

			return originalBitmap;
		}
	}
	
	private Tuple2<Integer,Integer> calculateSize(int originalWidth, int originalHeight ) {

		int screenHeight = getHeight() - (verticalMargin * 2);
		int screenWidth = getWidth() - (horizontalMargin * 2);
		
		// We scale to screen width for the cover or if the image is too
		// wide.
		if (originalWidth > screenWidth
				|| originalHeight > screenHeight || spine.isCover()) {

			float ratio = (float) originalWidth
					/ (float) originalHeight;

			int targetHeight = screenHeight - 1;
			int targetWidth = (int) (targetHeight * ratio);

			if (targetWidth > screenWidth - 1) {
				targetWidth = screenWidth - 1;
				targetHeight = (int) (targetWidth * (1 / ratio));
			}

			LOG.debug("Rescaling from " + originalWidth + "x"
					+ originalHeight + " to " + targetWidth + "x"
					+ targetHeight);

			if (targetWidth > 0 || targetHeight > 0) {
				return new Tuple2<>(targetWidth, targetHeight);
			}
		}

        return new Tuple2<>(originalWidth, originalHeight);
	}

	private class ImageTagHandler extends TagNodeHandler {

		private boolean fakeImages;
		
		public ImageTagHandler(boolean fakeImages) {
			this.fakeImages = fakeImages;			
		}

		@TargetApi(Build.VERSION_CODES.FROYO)
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end, SpanStack span) {

            String src = node.getAttributeByName("src");

			if (src == null) {
				src = node.getAttributeByName("href");
			}

			if (src == null) {
				src = node.getAttributeByName("xlink:href");
			}

            if ( src == null ) {
                return;
            }

			builder.append("\uFFFC");
			
			if (src.startsWith("data:image")) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {

                    try {
                        String dataString = src.substring(src
                            .indexOf(',') + 1);

                        byte[] binData = Base64.decode(dataString,
							Base64.DEFAULT);
					
					    setImageSpan(builder, new BitmapDrawable(
							getContext().getResources(),
							BitmapFactory.decodeByteArray(binData, 0, binData.length )),
							start, builder.length());
                    } catch ( OutOfMemoryError | IllegalArgumentException ia ) {
                        //Out of memory or invalid Base64, ignore
                    }
					
				}

			} else if ( spine != null ) {

				String resolvedHref = spine.resolveHref(src);

                if ( textLoader.hasCachedImage(resolvedHref) && ! fakeImages ) {
                    Drawable drawable = textLoader.getCachedImage(resolvedHref);
					setImageSpan(builder, drawable, start, builder.length());
					LOG.debug("Got cached href: " + resolvedHref);
				} else {
					LOG.debug("Loading href: " + resolvedHref);
					this.registerCallback(resolvedHref, new ImageCallback(
							resolvedHref, builder, start, builder.length(), fakeImages));
				}
			}
		}
		
		protected void registerCallback(String resolvedHref, ImageCallback callback ) {
			BookView.this.loader.registerCallback(resolvedHref, callback);
		}

	}

	@Override
	public void setBackgroundColor(int color) {
		super.setBackgroundColor(color);

		if (this.childView != null) {
			this.childView.setBackgroundColor(color);
		}
	}

    public void highlightClicked( HighLight highLight ) {
        for ( BookViewListener listener: listeners ) {
            listener.onHighLightClick(highLight);
        }
    }

	public void setTextColor(int color) {
		if (this.childView != null) {
			this.childView.setTextColor(color);
		}

		this.tableHandler.setTextColor(color);
	}
	
	public Book getBook() {
		return book;
	}

	public void setTextSize(float textSize) {
		this.childView.setTextSize(textSize);
		this.tableHandler.setTextSize(textSize);
	}

	public void addListener(BookViewListener listener) {
		this.listeners.add(listener);
	}

	private void bookOpened(Book book) {
		for (BookViewListener listener : this.listeners) {
			listener.bookOpened(book);
		}
	}

	private void errorOnBookOpening(String errorMessage) {
		for (BookViewListener listener : this.listeners) {
			listener.errorOnBookOpening(errorMessage);
		}
	}

	private void parseEntryStart(int entry) {
		for (BookViewListener listener : this.listeners) {
			listener.parseEntryStart(entry);
		}
	}

	private void parseEntryComplete( String name) {
		for (BookViewListener listener : this.listeners) {
			listener.parseEntryComplete(name);
		}
	}

	private void fireOpenFile() {
		for (BookViewListener listener : this.listeners) {
			listener.readingFile();
		}
	}

	private void fireRenderingText() {
		for (BookViewListener listener : this.listeners) {
			listener.renderingText();
		}
	}

    public int getPercentageFor( int index, int offset ) {
        if ( spine != null ) {
            return spine.getProgressPercentage(index, offset);
        }

        return -1;
    }

	private void progressUpdate() {

		if ( this.spine == null ) {
			return;
		}

		this.strategy.getText().filter( t -> t.length() > 0 ).forEach( text -> {

			double progressInPart = (double) this.getProgressPosition()
					/ (double) text.length();

			if (text.length() > 0 && strategy.isAtEnd()) {
				progressInPart = 1d;
			}

			int progress = spine.getProgressPercentage(progressInPart);

			if (progress != -1) {

				int pageNumber = getPageNumberFor(getIndex(),
						getProgressPosition());

				for (BookViewListener listener : this.listeners) {
					listener.progressUpdate(progress, pageNumber,
							spine.getTotalNumberOfPages());
				}
			}
		});
	}


    public int getTotalNumberOfPages() {
        if ( spine != null ) {
            return spine.getTotalNumberOfPages();
        }

        return -1;
    }

	public int getPageNumberFor( int index, int position ) {

        if ( spine == null ) {
            return -1;
        }

        LOG.debug( "Looking for pageNumber for index=" + index + ", position=" + position );
		
		int pageNum = -1;
		
		List<List<Integer>> pageOffsets = spine.getPageOffsets();
		
		if ( pageOffsets == null || index >= pageOffsets.size() ) {
			return -1;
		}
		
		for ( int i=0; i < index; i++ ) {

            int pages = pageOffsets.get(i).size();

			pageNum += pages;

            LOG.debug("Index " + i + ": pages=" + pages);
		}

        List<Integer> offsets = pageOffsets.get(index);

        LOG.debug("Pages before this index: " + pageNum );

        LOG.debug( "Offsets according to spine: " + asString(offsets) );

        if ( this.strategy instanceof FixedPagesStrategy ) {
            List<Integer> strategyOffsets = ( (FixedPagesStrategy) this.strategy ).getPageOffsets();
            LOG.debug("Offsets according to strategy: " + asString(strategyOffsets) );
        }

		for ( int i=0; i < offsets.size() && offsets.get(i) <= position; i++ ) {
			pageNum++;
		}

        LOG.debug( "Calculated pageNumber=" + pageNum );
		return pageNum;
	}

    private static String asString( List<Integer> offsets ) {

        StringBuilder stringBuilder = new StringBuilder("[ ");

        forEach( offsets, o -> stringBuilder.append( o + " ") );

        stringBuilder.append(" ]");

        return stringBuilder.toString();
    }

    private boolean needsPageNumberCalculation() {
        if ( ! configuration.isShowPageNumbers() ) {
            return false;
        }

        Option<List<List<Integer>>> offsets = configuration
                .getPageOffsets(fileName);

        return isEmpty( offsets ) ||
                offsets.unsafeGet().size() == 0;
    }

	public void setEnableScrolling(boolean enableScrolling) {

		if (this.strategy == null
				|| this.strategy.isScrolling() != enableScrolling) {

			int pos = -1;
			boolean wasNull = true;

			Spanned text = null;

			if (this.strategy != null) {
				pos = this.strategy.getTopLeftPosition();
				text = this.strategy.getText().unsafeGet();
				this.strategy.clearText();
				wasNull = false;
			}

            if (enableScrolling) {
                this.strategy = scrollingStrategyProvider.get();
            } else {
                this.strategy = fixedPagesStrategyProvider.get();
            }

            strategy.setBookView(this);

			if (!wasNull) {
				this.strategy.setPosition(pos);
			}

			if (text != null && text.length() > 0) {
				this.strategy.loadText(text);
			}
		}
	}

	public static class InnerView extends TextView {

		private BookView bookView;

        private long blockUntil = 0l;

		public InnerView(Context context, AttributeSet attributes) {
			super(context, attributes);
		}

		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			bookView.onInnerViewResize();
		}

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            /*
            We override this method to do nothing, since the base
            implementation closes the ActionMode.

            This means that when the user clicks the overflow menu,
            the ActionMode is stopped and text selection is ended.
             */
        }

		public void setBookView(BookView bookView) {
			this.bookView = bookView;
		}

        public void setBlockUntil( long blockUntil ) {
            this.blockUntil = blockUntil;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }

        @TargetApi( Build.VERSION_CODES.HONEYCOMB )
        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {

            if ( System.currentTimeMillis() > blockUntil ) {

                LOG.debug("InnerView starting action-mode");
                return super.startActionMode(callback);

            } else {
                LOG.debug("Not starting action-mode yet, since block time hasn't expired.");
                clearFocus();
                return null;
            }
        }
    }

	private static enum BookReadPhase {
		START, OPEN_FILE, PARSE_TEXT, DONE
	}

    private static class SearchResultSpan extends BackgroundColorSpan {
        public SearchResultSpan() {
            super( Color.YELLOW );
        }
    }

    private class OpenFileTask extends
            QueueableAsyncTask<None, BookReadPhase, Book> {

        private String error;

        @Override
        public void doOnPreExecute() {
            fireOpenFile();
        }

        @Override
        public Option<Book> doInBackground(None... args) {
            try {
                return some(initBookAndSpine());
            } catch ( IOException io ) {
                this.error = io.getLocalizedMessage();
            } catch ( OutOfMemoryError e ) {
                this.error = getContext().getString( R.string.out_of_memory );
            }

            return none();
        }

        @Override
        public void doOnPostExecute(Option<Book> book) {

            book.match(
                    BookView.this::bookOpened, //some
                    () -> errorOnBookOpening( this.error) //none
            );
        }
    }

	private class LoadTextTask extends
			QueueableAsyncTask<Resource, BookReadPhase, Spanned> {

		private String name;

		private String searchTerm = null;

		public LoadTextTask() {

		}

		LoadTextTask(String searchTerm) {
			this.searchTerm = searchTerm;
		}

        public Option<Spanned> doInBackground(Resource... resources) {

            publishProgress(BookReadPhase.START);

            if (loader != null) {
                loader.clear();
            }

            try {

                this.name = spine.getCurrentTitle().getOrElse("");

                Resource resource;

                if ( resources != null && resources.length > 0 ) {
                    resource = resources[0];
                } else {
                    resource = spine.getCurrentResource().getOrElse( new Resource("") );
                }

                publishProgress(BookReadPhase.PARSE_TEXT);

                Spannable result = textLoader.getText(resource, this::isCancelled );
                loader.load(); // Load all image resources.

                //Clear any old highlighting spans

                SearchResultSpan[] spans = result.getSpans(0, result.length(), SearchResultSpan.class);
                for ( BackgroundColorSpan span: spans ) {
                    result.removeSpan(span);
                }

                // Highlight search results (if any)
                if ( searchTerm != null ) {
                    Pattern pattern = Pattern.compile(Pattern.quote((searchTerm)),Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(result);

                    while ( matcher.find() ) {
                        result.setSpan(new SearchResultSpan(),
                                matcher.start(), matcher.end(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                //If the view isn't ready yet, wait a bit.
                while ( getInnerView().getWidth() == 0 ) {
                    Thread.sleep(100);
                }

                strategy.loadText(result);

                return option(result);
            } catch (Exception | OutOfMemoryError io ) {
                LOG.error( "Error loading text", io );

                //FIXME: actually use this error
                //this.error = String.format( getContext().getString(R.string.could_not_load),
                  //      io.getMessage());

                //this.error = getContext().getString(R.string.out_of_memory);
            }

            return none();
        }

        @Override
        public void doOnProgressUpdate(BookReadPhase... values) {

			BookReadPhase phase = values[0];

			switch (phase) {
			case START:
				parseEntryStart(getIndex());
				break;
			case PARSE_TEXT:
				fireRenderingText();
				break;
			case DONE:
				parseEntryComplete(this.name);
				break;
			}
		}

        @Override
        public void doOnPostExecute(Option<Spanned> result) {

			restorePosition();
			strategy.updateGUI();
			progressUpdate();

			onProgressUpdate(BookReadPhase.DONE);

			/**
			 * This is a hack for scrolling not updating to the right position 
			 * on Android 4+
			 */
			if ( strategy.isScrolling() ) {
				scrollHandler.postDelayed( BookView.this::restorePosition, 100 );
			}
        }
	}

	private class CalculatePageNumbersTask extends
			QueueableAsyncTask<Object, Void, List<List<Integer>>> {

        @Override
        public void doOnPreExecute() {
            for ( BookViewListener listener: listeners ) {
                listener.onStartCalculatePageNumbers();
            }
        }

        @Override
		public Option<List<List<Integer>>> doInBackground(Object... params) {

            if ( ! needsPageNumberCalculation() ) {
                return none();
            }

			try {

				Option<List<List<Integer>>> offsets = getOffsets();
                offsets.forEach(o -> configuration.setPageOffsets(fileName, o));

                LOG.debug("Calculated offsets: " + offsets );
				return offsets;

			} catch ( OutOfMemoryError | Exception e ) {
                LOG.error("Could not read page-numbers", e);
            }

			return none();
		}

        private void checkForCancellation() {
            if ( isCancelRequested() ) {
                throw new IllegalStateException("Cancel requested");
            }
        }

        /**
         * Loads the text offsets for the whole book,
         * with minimal use of resources.
         *
         * @return
         * @throws IOException
         */
        private Option<List<List<Integer>>> getOffsets()
                throws IOException {

            List<List<Integer>> result = new ArrayList<>();
            final ResourceLoader imageLoader = new ResourceLoader(fileName);
            final ResourceLoader textResourceLoader = new ResourceLoader(fileName);


            //Image loader which only loads image dimensions
            ImageTagHandler tagHandler = new ImageTagHandler(true) {
                protected void registerCallback(String resolvedHref, ImageCallback callback) {
                    imageLoader.registerCallback(resolvedHref, callback);
                }
            };

            //Private spanner
            final HtmlSpanner mySpanner = new HtmlSpanner();

            mySpanner.setAllowStyling( configuration.isAllowStyling() );
            mySpanner.setFontResolver( fontResolver );

            mySpanner.registerHandler("table", tableHandler );
            mySpanner.registerHandler("img", tagHandler);
            mySpanner.registerHandler("image", tagHandler);
            mySpanner.registerHandler("link", new CSSLinkHandler(textLoader));

            final Map<String, List<Integer>> offsetsPerSection = new HashMap<>();

            //We use the ResourceLoader here to load all the text in the book in 1 pass,
            //but we only keep a single section in memory at each moment
            ResourceCallback callback = (String href, InputStream stream) -> {

                try {

                    checkForCancellation();
                    LOG.debug("CalculatePageNumbersTask: loading text for: " + href );
                    InputStream input = new ByteArrayInputStream(IOUtil.toByteArray(stream));

                    checkForCancellation();
                    Spannable text = mySpanner.fromHtml(input, this::isCancelled );

                    checkForCancellation();
                    imageLoader.load();

                    checkForCancellation();
                    FixedPagesStrategy fixedPagesStrategy = getFixedPagesStrategy();
                    fixedPagesStrategy.setBookView(BookView.this);

                    offsetsPerSection.put(href, fixedPagesStrategy.getPageOffsets(text, true));

                } catch ( IOException io ) {
                    LOG.error( "CalculatePageNumbersTask: failed to load text for " + href, io );
                }
            };

            //Do first pass: grab either cached text, or schedule a callback
            for ( PageTurnerSpine.SpineEntry spineEntry: spine ) {

                checkForCancellation();
                Resource res = spineEntry.getResource();

                Option<Spannable> cachedText = textLoader.getCachedTextForResource( res );

                if ( ! isEmpty(cachedText) ) {
                    LOG.debug("CalculatePageNumbersTask: Got cached text for href: " + res.getHref() );

                    FixedPagesStrategy fixedPagesStrategy = getFixedPagesStrategy();
                    fixedPagesStrategy.setBookView(BookView.this);

                    offsetsPerSection.put(res.getHref(), fixedPagesStrategy.getPageOffsets(
                            cachedText.unsafeGet(), true));

                } else {
                    LOG.debug("CalculatePageNumbersTask: Registering callback for href: " + res.getHref() );
                    textResourceLoader.registerCallback( res.getHref(), callback );
                }
            }

            //Load callbacks, this will fill renderedText
            textResourceLoader.load();
            imageLoader.load();

            //Do a second pass and order the offsets correctly
            for (PageTurnerSpine.SpineEntry spineEntry: spine ) {

                checkForCancellation();

                Resource res = spineEntry.getResource();

                Option<List<Integer>> offsets = none();

                //Scan for the full href
                for ( String href: offsetsPerSection.keySet() ) {
                    if ( href.endsWith( res.getHref() )) {
                        offsets = some(offsetsPerSection.get(href));
                        break;
                    }
                }

                if ( isEmpty(offsets) ) {
                    LOG.error( "CalculatePageNumbersTask: Missing text for href " + res.getHref() );
                    return none();
                }

                result.add( offsets.getOrElse( new ArrayList<>() ) );
            }

            return some(result);
        }

        //Injection doesn't work from inner classes, so we construct it ourselves.
        private FixedPagesStrategy getFixedPagesStrategy() {
            FixedPagesStrategy fixedPagesStrategy =  new FixedPagesStrategy();
            fixedPagesStrategy.setConfig( configuration );
            fixedPagesStrategy.setLayoutFactory( new StaticLayoutFactory() );

            return fixedPagesStrategy;
        }

        @Override
        public void doOnCancelled(Option<List<List<Integer>>> lists) {
            if ( taskQueue.isEmpty() ) {
                for ( BookViewListener listener: listeners ) {
                    listener.onCalculatePageNumbersComplete();
                }
            }
        }

        @Override
        public void doOnPostExecute(Option<List<List<Integer>>> result) {

            LOG.debug("Pagenumber calculation completed.");

            for ( BookViewListener listener: listeners ) {
                listener.onCalculatePageNumbersComplete();
            }

            result.filter( r -> r.size() > 0 ).forEach(r -> {
                spine.setPageOffsets(r);
                progressUpdate();
            });
		}
	}

}
