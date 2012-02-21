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

package net.nightwhistler.pageturner.activity;

import java.net.URLEncoder;
import java.util.List;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.AnimationStyle;
import net.nightwhistler.pageturner.Configuration.ColourProfile;
import net.nightwhistler.pageturner.Configuration.ScrollStyle;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.animation.Animations;
import net.nightwhistler.pageturner.animation.Animator;
import net.nightwhistler.pageturner.animation.PageCurlAnimator;
import net.nightwhistler.pageturner.animation.PageTimer;
import net.nightwhistler.pageturner.animation.RollingBlindAnimator;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.sync.AccessException;
import net.nightwhistler.pageturner.sync.BookProgress;
import net.nightwhistler.pageturner.sync.ProgressService;
import net.nightwhistler.pageturner.view.AnimatedImageView;
import net.nightwhistler.pageturner.view.BookView;
import net.nightwhistler.pageturner.view.BookViewListener;
import net.nightwhistler.pageturner.view.NavGestureDetector;
import net.nightwhistler.pageturner.view.ProgressListAdapter;
import nl.siegmann.epublib.domain.Book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.inject.Inject;

public class ReadingActivity extends RoboActivity implements BookViewListener {

	private static final String POS_KEY = "offset:";
	private static final String IDX_KEY = "index:";

	protected static final int REQUEST_CODE_GET_CONTENT = 2;
	
	public static final String PICK_RESULT_ACTION = "colordict.intent.action.PICK_RESULT";	
	public static final String SEARCH_ACTION = "colordict.intent.action.SEARCH";
	public static final String EXTRA_QUERY = "EXTRA_QUERY";
	public static final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
	public static final String EXTRA_HEIGHT = "EXTRA_HEIGHT";
	public static final String EXTRA_WIDTH = "EXTRA_WIDTH";
	public static final String EXTRA_GRAVITY = "EXTRA_GRAVITY";
	public static final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
	public static final String EXTRA_MARGIN_TOP = "EXTRA_MARGIN_TOP";
	public static final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
	public static final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";
	
	private static final Logger LOG = LoggerFactory.getLogger(ReadingActivity.class);
	
	@Inject	
	private ProgressService progressService;	
	
	@Inject 
	private LibraryService libraryService;
	
	@Inject
	private Configuration config;    
	
	@InjectView(R.id.mainContainer) 
	private ViewSwitcher viewSwitcher;	
	
	@InjectView(R.id.bookView) 
	private BookView bookView;
	
	@InjectView(R.id.myTitleBarTextView) 
	private TextView titleBar;
	
	@InjectView(R.id.myTitleBarLayout) 
	private LinearLayout titleBarLayout;
	
	@InjectView(R.id.dummyView)
	private AnimatedImageView dummyView;
	
	private ProgressDialog waitDialog;
	private AlertDialog tocDialog;		
	
    private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
		
	private String bookTitle;
	private String titleBase;
		
	private String fileName;
	private int progressPercentage;
	
	private boolean oldBrightness = false;
	private boolean oldStripWhiteSpace = false;
		
	private enum Orientation { HORIZONTAL, VERTICAL }
	
	private CharSequence selectedWord = null;
	
	private Handler uiHandler;
	private Handler backgroundHandler;
	
	private Toast brightnessToast;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Restore preferences
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.read_book);
        
        this.uiHandler = new Handler();
        
        HandlerThread bgThread = new HandlerThread("background");
        bgThread.start();
        this.backgroundHandler = new Handler(bgThread.getLooper());
        
        this.waitDialog = new ProgressDialog(this);
        this.waitDialog.setOwnerActivity(this);               
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        this.gestureDetector = new GestureDetector(new NavGestureDetector(
        		bookView, this, metrics));         
        /*
        final PinchZoomListener pinch = new PinchZoomListener( this,
        		new PinchZoomListener.FloatAdapter() {
        			
			@Override public void setValue(float value) { updateTextSize(value); }			
			@Override public float getValue() {	return bookView.getTextSize(); }
		});
		*/
        
        this.gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	//pinch.onTouch(v, event);
                return gestureDetector.onTouchEvent(event);
            }
        };           
        
    	this.viewSwitcher.setOnTouchListener(gestureListener);
    	this.bookView.setOnTouchListener(gestureListener);    	
        
    	this.bookView.addListener(this);
    	this.bookView.setSpanner(getInjector().getInstance(HtmlSpanner.class));
    	
    	this.oldBrightness = config.isBrightnessControlEnabled();
    	this.oldStripWhiteSpace = config.isStripWhiteSpaceEnabled();
    	
    	registerForContextMenu(bookView);
    	
    	String file = getIntent().getStringExtra("file_name");
    	
        if ( file == null && getIntent().getData() != null ) {        
        	file = getIntent().getData().getPath();
        }
        
        if ( file == null ) {
        	file = config.getLastOpenedFile();
        }
        
        updateFromPrefs();
    	updateFileName(savedInstanceState, file); 
    	
    	if ( "".equals( fileName ) ) {        	
        	
    		Intent intent = new Intent(this, LibraryActivity.class);
        	startActivity(intent);
        	finish();
        	return;
        	
        } else { 
        	
        	if ( savedInstanceState == null &&  config.isSyncEnabled() ) {
        		new DownloadProgressTask().execute();
        	} else {        	
        		bookView.restore();
        	}
        }
    	
    }    
    
    private void updateFileName(Bundle savedInstanceState, String fileName) {
    	
    	this.fileName = fileName;
    	
    	int lastPos = config.getLastPosition(fileName);
    	int lastIndex = config.getLastIndex(fileName);

    	if (savedInstanceState != null ) {
    		lastPos = savedInstanceState.getInt(POS_KEY, -1);
    		lastIndex = savedInstanceState.getInt(IDX_KEY, -1);
    	}       

    	this.bookView.setFileName(fileName);
    	this.bookView.setPosition(lastPos);
    	this.bookView.setIndex(lastIndex);
    	
    	config.setLastOpenedFile(fileName);
    }           
    
    /**
     * Immediately updates the text size in the BookView,
     * and saves the preference in the background.
     * 
     * @param textSize
     */
    private void updateTextSize( final float textSize ) {
    	bookView.setTextSize(textSize);
    	backgroundHandler.post(new Runnable() {
			
			@Override
			public void run() {
				config.setTextSize( (int) textSize );
			}
		});
    }
    
    @Override
    public void progressUpdate(int progressPercentage) {
    	if ( titleBase == null ) {
    		return;
    	}
    	
    	this.progressPercentage = progressPercentage;
    	
    	String title = this.titleBase;
    	
    	SpannableStringBuilder spannedTitle = new SpannableStringBuilder();
    	spannedTitle.append(title);
    	spannedTitle.append(" " + progressPercentage + "%");
    	    	
    	this.titleBar.setTextColor(Color.WHITE);
    	this.titleBar.setText(spannedTitle);
    }
    
    private void updateFromPrefs() {
    	    	
    	this.progressService.setConfig(this.config);
    	                      
        bookView.setTextSize( config.getTextSize() );
        
        int marginH = config.getHorizontalMargin();
        int marginV = config.getVerticalMargin();
        
        this.bookView.setTypeface(config.getTypeface());
        
        bookView.setHorizontalMargin(marginH);
        bookView.setVerticalMargin(marginV);
        
        if ( ! isAnimating() ) {
        	bookView.setEnableScrolling(config.isScrollingEnabled());
        }
        
        bookView.setStripWhiteSpace(config.isStripWhiteSpaceEnabled()); 
        bookView.setLineSpacing(config.getLineSpacing());             

        if ( config.isFullScreenEnabled() ) {
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            this.titleBarLayout.setVisibility(View.GONE);
        } else {    
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	this.titleBarLayout.setVisibility(View.VISIBLE);
    	}
        
        restoreColorProfile();
        
        //Check if we need a restart
        if ( config.isBrightnessControlEnabled() != oldBrightness
        		|| config.isStripWhiteSpaceEnabled() != oldStripWhiteSpace ) {
        	Intent intent = new Intent(this, ReadingActivity.class);
        	intent.setData(Uri.parse(this.fileName));
        	startActivity(intent);
        	finish();
        }
        
        Configuration.OrientationLock orientation = config.getScreenOrientation(); 
        
        switch ( orientation ) {
        case PORTRAIT:
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        	break;
        case LANDSCAPE:
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        	break;
        default:
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	if ( hasFocus ) {
    		updateFromPrefs();
    	}
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {    	
    	return bookView.onTouchEvent(event);
    }
    
    @Override
    public void bookOpened(final Book book) {
    	
    	this.bookTitle = book.getTitle();
    	this.titleBase = this.bookTitle;
    	setTitle( titleBase );  
    	
    	backgroundHandler.post(new Runnable() {
			
			@Override
			public void run() {
						    		
	        	try {
	        		libraryService.storeBook(fileName, book, true, config.isCopyToLibrayEnabled() );
	        	} catch (Exception io) {
	        		LOG.error("Copy to library failed.", io);
	        	}	
			}
		});    	
    	
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	
    	//This is a hack to give the longclick handler time
    	//to find the word the user long clicked on.    	
    	
    	if ( this.selectedWord != null ) {
    		
    		final CharSequence word = this.selectedWord;
    		
    		String header = String.format(getString(R.string.word_select), selectedWord);    		
    		menu.setHeaderTitle(header);
    		
    		final Intent intent = new Intent(PICK_RESULT_ACTION);
        	intent.putExtra(EXTRA_QUERY, word.toString()); //Search Query
        	intent.putExtra(EXTRA_FULLSCREEN, false); //
        	intent.putExtra(EXTRA_HEIGHT, 400); //400pixel, if you don't specify, fill_parent"
        	intent.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM);
        	intent.putExtra(EXTRA_MARGIN_LEFT, 100);
        	
        	if ( isIntentAvailable(this, intent)) {
        		MenuItem item = menu.add(getString(R.string.dictionary_lookup));
        		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						startActivityForResult(intent, 5); 
						return true;
					}
				});
        	}
        	        
        	MenuItem newItem = menu.add(getString(R.string.wikipedia_lookup));
        	newItem.setOnMenuItemClickListener(new BrowserSearchMenuItem(
					"http://en.wikipedia.org/wiki/Special:Search?search="
        			+ URLEncoder.encode( word.toString() )));
		            
        	MenuItem newItem2 = menu.add(getString(R.string.google_lookup));
        	newItem2.setOnMenuItemClickListener(new BrowserSearchMenuItem(
					"http://www.google.com/search?q="
        			+ URLEncoder.encode( word.toString() )));
        	
        	this.selectedWord = null;
    	}    	 
    }    
    
    private class BrowserSearchMenuItem implements OnMenuItemClickListener {
    	
    	private String launchURL;
    	
    	public BrowserSearchMenuItem(String url) {
    		this.launchURL = url;
		}
    	
    	@Override
    	public boolean onMenuItemClick(MenuItem item) {
    		Intent i = new Intent(Intent.ACTION_VIEW);  
            i.setData(Uri.parse(this.launchURL));  
            startActivity(i);  
            
            return true;
    	}
    }
    
    public static boolean isIntentAvailable(Context context, Intent intent) {
    	final PackageManager packageManager = context.getPackageManager();
    	List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    	return list.size() > 0;
    }
    
    private void restoreColorProfile() {

    	this.bookView.setBackgroundColor(config.getBackgroundColor());
    	this.viewSwitcher.setBackgroundColor(config.getBackgroundColor());
    	this.bookView.setTextColor(config.getTextColor());   
    	this.bookView.setLinkColor(config.getLinkColor());

    	int brightness = config.getBrightNess();
    	
    	if ( config.isBrightnessControlEnabled() ) {
    		setScreenBrightnessLevel(brightness);
    	}  
    }    
    
    private void setScreenBrightnessLevel( int level ) {
    	WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = (float) level / 100f;
		getWindow().setAttributes(lp);
    }
  
    
    @Override
    public void errorOnBookOpening(String errorMessage) {    	
    	this.waitDialog.hide();    	
    	String message = String.format(getString(R.string.error_open_bk),  errorMessage);
        bookView.setText(new SpannedString(message));    	
    }
    
    @Override
    public void parseEntryComplete(int entry, String name) {
    	if (name != null && ! name.equals(this.bookTitle) ) {
    		this.titleBase = this.bookTitle + " - " + name;    		
    	} else {
    		this.titleBase = this.bookTitle;
    	}
    	
    	setTitle(this.titleBase);
    	this.waitDialog.hide();
    }
    
    @Override
    public void parseEntryStart(int entry) {
    	this.viewSwitcher.clearAnimation();
    	this.viewSwitcher.setBackgroundDrawable(null);
    	restoreColorProfile();
    	
    	this.waitDialog.setTitle(getString(R.string.loading_wait));
    	this.waitDialog.show();
    }    
    
    
    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
	    int keyCode = event.getKeyCode();
	    
	    if ( isAnimating() && action == KeyEvent.ACTION_DOWN) {
	    	stopAnimating();
	    	return true;
	    }	    
	    
	    switch (keyCode) {
	    
	    	case KeyEvent.KEYCODE_VOLUME_DOWN:
	    		//Yes, this is nasty: if the setting is true, we fall through to the next case.
	    		if (! config.isVolumeKeyNavEnabled() ) { return false; }	    		
	        
	    	case KeyEvent.KEYCODE_DPAD_RIGHT:
	            
	        	if (action == KeyEvent.ACTION_DOWN) {
	                pageDown(Orientation.HORIZONTAL);	        		
	            }
	        	
	        	return true;	 

	        case KeyEvent.KEYCODE_VOLUME_UP:
		        //Same dirty trick.
	    		if (! config.isVolumeKeyNavEnabled() ) { return false; } 

	        case KeyEvent.KEYCODE_DPAD_LEFT:	
	            if (action == KeyEvent.ACTION_DOWN) {
	               pageUp(Orientation.HORIZONTAL);	            	
	            }
	            
	            return true;	
	            
	        case KeyEvent.KEYCODE_BACK:
	        	if ( action == KeyEvent.ACTION_DOWN ) { 
	        		if ( bookView.hasPrevPosition() ) {
	        			bookView.goBackInHistory();
	        		
	        			return true;
	        		} else {
	        			this.finish();
	        		}
	        	}
	        
	    }

	    return false;
    }   
    
    
    private boolean isAnimating() {
    	Animator anim = dummyView.getAnimator();
    	return anim != null && ! anim.isFinished();
    }
    
    private void startAutoScroll() {
    	
    	if ( viewSwitcher.getCurrentView() == this.dummyView ) {
    		viewSwitcher.showNext();
    	}
    	
    	this.viewSwitcher.setInAnimation(null);
    	this.viewSwitcher.setOutAnimation(null);
    	
    	bookView.setKeepScreenOn(true);
    	
    	ScrollStyle style = config.getAutoScrollStyle();
    	
    	if ( style == ScrollStyle.ROLLING_BLIND ) {
    		prepareRollingBlind();
    	} else {
    		preparePageTimer();
    	}    	
    	
    	viewSwitcher.showNext();
    	
    	uiHandler.post( new AutoScrollRunnable() );    	
    }
    
    private void prepareRollingBlind() {
    	
    	Bitmap before = getBookViewSnapshot();
    	
    	bookView.pageDown();
    	Bitmap after = getBookViewSnapshot();
    	    	
    	RollingBlindAnimator anim = new RollingBlindAnimator();
    	anim.setAnimationSpeed( config.getScrollSpeed() );
    	    	
    	anim.setBackgroundBitmap(before);
    	anim.setForegroundBitmap(after);
    	
    	dummyView.setAnimator(anim);
    }
    
    private void preparePageTimer() {
    	bookView.pageDown();
    	Bitmap after = getBookViewSnapshot();    	
    	
    	PageTimer timer = new PageTimer();
    	timer.setBackgroundImage(after);
    	timer.setSpeed( config.getScrollSpeed() );
    	
    	dummyView.setAnimator(timer);
    }
    
    private void doPageCurl(boolean flipRight) {

    	if ( isAnimating() ) {
    		return;
    	}
    	
    	this.viewSwitcher.setInAnimation(null);
    	this.viewSwitcher.setOutAnimation(null);

    	if ( viewSwitcher.getCurrentView() == this.dummyView ) {
    		viewSwitcher.showNext();
    	}

    	Bitmap before = getBookViewSnapshot();

    	PageCurlAnimator animator = new PageCurlAnimator(flipRight);    
    	
    	//Pagecurls should only take a few frames. When the screen gets
    	//bigger, so do the frames.
    	animator.SetCurlSpeed( bookView.getWidth() / 12 );    	
    	
    	animator.setBackgroundColor(config.getBackgroundColor());    		

    	if ( flipRight ) {
    		bookView.pageDown();
    		Bitmap after = getBookViewSnapshot();
    		animator.setBackgroundBitmap(after);
    		animator.setForegroundBitmap(before);
    	} else {
    		bookView.pageUp();
    		Bitmap after = getBookViewSnapshot();
    		animator.setBackgroundBitmap(before);
    		animator.setForegroundBitmap(after);
    	}    		


    	dummyView.setAnimator(animator);

    	this.viewSwitcher.showNext();

    	uiHandler.post( new PageCurlRunnable(animator) );

    	dummyView.invalidate();    	

    }    
    
    private class PageCurlRunnable implements Runnable {

    	private PageCurlAnimator animator;

    	public PageCurlRunnable(PageCurlAnimator animator) {
    		this.animator = animator;
    	}

    	@Override
    	public void run() {

    		if ( this.animator.isFinished() ) {

    			if ( viewSwitcher.getCurrentView() == dummyView ) {
    				viewSwitcher.showNext();    					
    			}

    			dummyView.setAnimator(null);   				

    		} else {    			
    			this.animator.advanceOneFrame();
    			dummyView.invalidate();

    			int delay = 1000 / this.animator.getAnimationSpeed();

    			uiHandler.postDelayed(this, delay);
    		} 
    	}    		
    }   
    
    private class AutoScrollRunnable implements Runnable {
    	@Override
    	public void run() {
    		    		
    		if ( dummyView.getAnimator() == null ) {
    			LOG.debug( "BookView no longer has an animator. Aborting rolling blind." );
    			stopAnimating();
    		} else {
    			
    			Animator anim = dummyView.getAnimator();
    			
    			if ( anim.isFinished() ) {
    				startAutoScroll();
    			} else {    			
    				anim.advanceOneFrame();
    				dummyView.invalidate();   				
    			
    				uiHandler.postDelayed(this, anim.getAnimationSpeed() * 2);
    			} 
    		}    		
    	}
    }
    
    private void stopAnimating() {
    	
    	if ( dummyView.getAnimator() != null ) {
    		dummyView.getAnimator().stop();
    		this.dummyView.setAnimator(null);
    	}
    	
    	if ( viewSwitcher.getCurrentView() == this.dummyView ) {
    		viewSwitcher.showNext();
    	}    	
    	
    	bookView.setKeepScreenOn(false);    	 	
    }
    
    private Bitmap getBookViewSnapshot() {
    	
    	bookView.layout(0, 0, viewSwitcher.getWidth(), viewSwitcher.getHeight());
    	
    	try {
    		bookView.buildDrawingCache(false);
    		Bitmap drawingCache = bookView.getDrawingCache();		
		  		
    		if ( drawingCache != null ) {					
    			Bitmap copy = drawingCache.copy(drawingCache.getConfig(), false);
    			bookView.destroyDrawingCache();
    			return copy;
    		}
    		
    	} catch (OutOfMemoryError out) {
    		viewSwitcher.setBackgroundColor(config.getBackgroundColor());	
    	}	
    	
    	return null;
    }
    
    private void prepareSlide(Animation inAnim, Animation outAnim) {
    	    	    	    	
    	dummyView.setVisibility(View.VISIBLE);
    	dummyView.setImageBitmap(null);
    	
    	Bitmap bitmap = getBookViewSnapshot();
    	
    	if ( bitmap != null ) {
    		dummyView.setImageBitmap(bitmap);
    	}    		
    	
    	viewSwitcher.layout(0, 0, viewSwitcher.getWidth(), viewSwitcher.getHeight() );
    	this.viewSwitcher.showNext();
    	
		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);		
    }   
    
    private void pageDown(Orientation o) {
    	
    	if ( bookView.isAtEnd() ) {
    		return;
    	}
    	
    	stopAnimating();    
    	
    	if ( o == Orientation.HORIZONTAL  ) {
    		
    		AnimationStyle animH = config.getHorizontalAnim();
    		
    		if ( animH == AnimationStyle.CURL ) {
    			doPageCurl(true);
    		} else if (animH == AnimationStyle.SLIDE ) {
    			prepareSlide(Animations.inFromRightAnimation(), Animations.outToLeftAnimation());
        		this.viewSwitcher.showNext();
        		bookView.pageDown();
    		} else {
    			bookView.pageDown();
    		}
    		
    	} else {
    		if ( config.getVerticalAnim() == AnimationStyle.SLIDE ){    	
    			prepareSlide(Animations.inFromBottomAnimation(), Animations.outToTopAnimation() );    		
    			this.viewSwitcher.showNext();
    		}
    		
    		bookView.pageDown();
    	}    	    	
		
    }
    
    private void pageUp(Orientation o) {
    	
    	if ( bookView.isAtStart() ) {
    		return;
    	}
    	
    	stopAnimating();   	
    	
    	if ( o == Orientation.HORIZONTAL  ) {
    		
    		AnimationStyle animH = config.getHorizontalAnim();
    		
    		if ( animH == AnimationStyle.CURL ) {
    			doPageCurl(false);
    		} else if ( animH == AnimationStyle.SLIDE ) {
    			prepareSlide(Animations.inFromLeftAnimation(), Animations.outToRightAnimation()); 
        		this.viewSwitcher.showNext();
        		bookView.pageUp();
    		} else {
    			bookView.pageUp();
    		}
    		
    	} else {
    		
    		if ( config.getVerticalAnim() == AnimationStyle.SLIDE ){    	
    			prepareSlide(Animations.inFromTopAnimation(), Animations.outToBottomAnimation());    		
    			this.viewSwitcher.showNext();
    		}
    		
    		bookView.pageUp();
    	}    	
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	if ( this.tocDialog == null ) {
    		initTocDialog();
    	}
    	
    	MenuItem nightMode = menu.findItem(R.id.profile_night);
    	MenuItem dayMode = menu.findItem(R.id.profile_day);
    	
    	MenuItem showToc = menu.findItem(R.id.show_toc );
    	MenuItem sync = menu.findItem(R.id.manual_sync );
    	
    	showToc.setEnabled( this.tocDialog != null );
    	sync.setEnabled( config.isSyncEnabled() );
    	
    	if ( config.getColourProfile() == ColourProfile.DAY ) {
    		dayMode.setVisible(false);
    		nightMode.setVisible(true);
    	} else {
    		dayMode.setVisible(true);
    		nightMode.setVisible(false);
    	}  
    	
    	//Only show open file item if we have a file manager installed
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    	intent.setType("file/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        if (! isIntentAvailable(this, intent)) {
        	menu.findItem(R.id.open_file).setVisible(false);
        }
    	
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	this.titleBarLayout.setVisibility(View.VISIBLE);
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public void onOptionsMenuClosed(Menu menu) {
    	if ( config.isFullScreenEnabled() ) {
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        			WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	
        	this.titleBarLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * This is called after the file manager finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if ( resultCode == RESULT_OK && data != null) {
    		// obtain the filename
    		Uri fileUri = data.getData();
    		if (fileUri != null) {
    			String filePath = fileUri.getPath();
    			if (filePath != null) {
    				loadNewBook(filePath);  
    			}
    		}
    	}    	
    }
    
    private void loadNewBook( String fileName ) {
    	setTitle(R.string.app_name);
    	this.tocDialog = null;
    	this.bookTitle = null;
    	this.titleBase = null;
    	
		bookView.clear();
		
		updateFileName(null, fileName);
		new DownloadProgressTask().execute();   
    }
    
    @Override
    protected void onStop(){
       super.onStop();

       if ( this.bookView != null ) {    	   
    	  
    	   config.setLastPosition(this.fileName, this.bookView.getPosition() );
    	   config.setLastIndex(this.fileName, this.bookView.getIndex() );    	   
       }
       
       this.waitDialog.dismiss();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reading_menu, menu);        
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        // Handle item selection
        switch (item.getItemId()) {
     
        case R.id.profile_night:
        	config.setColourProfile(ColourProfile.NIGHT);   
        	this.restoreColorProfile();  
            return true;
            
        case R.id.profile_day:
        	config.setColourProfile(ColourProfile.DAY);   
        	this.restoreColorProfile();  
            return true;
            
        case R.id.manual_sync:
        	new ManualProgressSync().execute();
        	return true;
        	
        case R.id.preferences:
        	//Cache old settings to check if we'll need a restart later
        	oldBrightness = config.isBrightnessControlEnabled();
        	oldStripWhiteSpace = config.isStripWhiteSpaceEnabled();
        	
        	Intent i = new Intent(this, PageTurnerPrefsActivity.class);
        	startActivity(i);
        	return true;
        	
        case R.id.show_toc:   
        	this.tocDialog.show();
        	return true;
        	
        case R.id.open_file:
        	launchFileManager();
        	return true;        	
        
        case R.id.open_library:
        	launchLibrary();
        	return true;  	
        	
        case R.id.rolling_blind:
        	startAutoScroll();
        	return true;
        	
        case R.id.about:
        	Dialogs.showAboutDialog(this);
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
      

    @Override
    public boolean onSwipeDown() {
    	if ( config.isVerticalSwipeEnabled() ) {
    		pageDown(Orientation.VERTICAL);
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public boolean onSwipeUp() {
    	if ( config.isVerticalSwipeEnabled() ) {
    		pageUp(Orientation.VERTICAL);
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public void onScreenTap() {
    	stopAnimating();
    }
    
    @Override
    public boolean onSwipeLeft() {    			
    	if ( config.isHorizontalSwipeEnabled() ) {    		
    		pageDown(Orientation.HORIZONTAL);
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public boolean onSwipeRight() {
    	if ( config.isHorizontalSwipeEnabled() ) {    		
    		pageUp(Orientation.HORIZONTAL);
    		return true;
    	}
    	
    	return false;
    }   
    
    @Override
    public boolean onTapLeftEdge() {
    	if ( config.isHorizontalTappingEnabled() ) {
    		pageUp(Orientation.HORIZONTAL);
    		return true;
    	}
    		
    	return false;
    }
    
    @Override
    public boolean onTapRightEdge() {
    	if ( config.isHorizontalTappingEnabled() ) {
    		pageDown(Orientation.HORIZONTAL);
    		return true;
    	}
    		
    	return false;
    }
    
    @Override
    public boolean onTapTopEdge() {
    	if ( config.isVerticalTappingEnabled() ) {
    		pageUp(Orientation.VERTICAL);
    		return true;
    	}
    		
    	return false;
    }
    
    @Override
    public boolean onTopBottomEdge() {
    	if ( config.isVerticalTappingEnabled() ) {
    		pageDown(Orientation.VERTICAL);
    		return true;
    	}
    		
    	return false;
    }
    
    @Override
    public boolean onLeftEdgeSlide(int value) {
    	
    	if ( config.isBrightnessControlEnabled() && value != 0 ) {
    		int baseBrightness = config.getBrightNess();
    		
    		int brightnessLevel = Math.min(99, value + baseBrightness);
			brightnessLevel = Math.max(1, brightnessLevel);
			
			final int level = brightnessLevel;
			
			String brightness = getString(R.string.brightness);
			setScreenBrightnessLevel(brightnessLevel);
			
			if ( brightnessToast == null ) {    				
				brightnessToast = Toast.makeText(ReadingActivity.this, brightness + ": " + brightnessLevel, Toast.LENGTH_SHORT);
			} else {
				brightnessToast.setText(brightness + ": " + brightnessLevel);
			}
			
			brightnessToast.show();
			
			backgroundHandler.post(new Runnable() {
				
				@Override
				public void run() {
					config.setBrightness(level);					
				}
			});
			
			return true;
    	}
    	
    	return false;
    }
    
    @Override
    public boolean onRightEdgeSlide(int value) {
    	return false;
    }
    
    @Override
    public void onWordLongPressed(CharSequence word) {
    	this.selectedWord = word;
		openContextMenu(bookView);
    }
    
    private void launchFileManager() {
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
                startActivityForResult(intent, REQUEST_CODE_GET_CONTENT);
        } catch (ActivityNotFoundException e) {
                // No compatible file manager was found.
                Toast.makeText(this, getString(R.string.install_oi), 
                                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void launchLibrary() {
    	Intent intent = new Intent(this, LibraryActivity.class);
    	startActivity(intent);    	
    }    
    
    private void showPickProgressDialog( final List<BookProgress> results ) {

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(getString(R.string.cloud_bm));    	
    	
    	ProgressListAdapter adapter = new ProgressListAdapter(this, 
    			bookView, results);    	
    	builder.setAdapter(adapter, adapter);

    	AlertDialog dialog = builder.create();
    	dialog.setOwnerActivity(this);
    	dialog.show();
    } 
    
    
    private void initTocDialog() {

    	if ( this.tocDialog != null ) {
    		return;
    	}

    	final List<BookView.TocEntry> tocList = this.bookView.getTableOfContents();

    	if ( tocList == null || tocList.isEmpty() ) {
    		return;
    	}

    	final CharSequence[] items = new CharSequence[ tocList.size() ];

    	for ( int i=0; i < items.length; i++ ) {
    		items[i] = tocList.get(i).getTitle();
    	}

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.toc_label);

    	builder.setItems(items, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int item) {
    			bookView.navigateTo( tocList.get(item).getHref() );
    		}
    	});

    	this.tocDialog = builder.create();
    	this.tocDialog.setOwnerActivity(this);
    }
    
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
    	if ( this.bookView != null ) {

    		outState.putInt(POS_KEY, this.bookView.getPosition() );  
    		outState.putInt(IDX_KEY, this.bookView.getIndex());

    		libraryService.updateReadingProgress(fileName, progressPercentage);			    		
    		libraryService.close();
    		
    		backgroundHandler.post(new Runnable() {
    			@Override
    			public void run() {
    				try {
    					progressService.storeProgress(fileName,
    	    				bookView.getIndex(), bookView.getPosition(), 
    	    				progressPercentage);
    				} catch (AccessException a) {}
    			}
    		});
    	}
    }    
    
    private class ManualProgressSync extends AsyncTask<Void, Integer, List<BookProgress>> {
    	
    	private boolean accessDenied = false;
    	
    	@Override
    	protected void onPreExecute() {
    		waitDialog.setTitle(R.string.syncing);
    		waitDialog.show();
    	}
    	
    	@Override
    	protected List<BookProgress> doInBackground(Void... params) { 
    		try {
    			return progressService.getProgress(fileName);
    		} catch (AccessException e) {
    			accessDenied = true;
    			return null;
    		}
    	}
    	
    	@Override
    	protected void onPostExecute(List<BookProgress> progress) {  
    		waitDialog.hide();    		
    		
    		if ( progress == null ) {
    			
    		    AlertDialog.Builder alertDialog = new AlertDialog.Builder(ReadingActivity.this);
    		    
   		        alertDialog.setTitle(R.string.sync_failed);
   		        
   		        if ( accessDenied ) {
   		        	alertDialog.setMessage(R.string.access_denied);
   		        } else {
   		        	alertDialog.setMessage(R.string.connection_fail);
   		        }
   		        
   		        alertDialog.setNeutralButton(android.R.string.ok, new OnClickListener() {
	              public void onClick(DialogInterface dialog, int which) {
	            	  dialog.dismiss();
    		      } });
   		        
   		        alertDialog.show();   		        
    			
    		} else {
    			showPickProgressDialog(progress);
    		}
    	}
    }
    
    private class DownloadProgressTask extends AsyncTask<Void, Integer, BookProgress> {
    	    	    	
    	@Override
    	protected void onPreExecute() {
    		waitDialog.setTitle(R.string.syncing);
    		waitDialog.show();
    	}
    	
    	@Override
    	protected BookProgress doInBackground(Void... params) {  
    		try {
    			List<BookProgress> updates = progressService.getProgress(fileName);
    			
    			if ( updates != null && updates.size() > 0 ) {
        			return updates.get(0);
        		}
    		} catch (AccessException e) {}
    		
    		return null;
    	}
    	
    	@Override
    	protected void onPostExecute(BookProgress progress) {  
    		waitDialog.hide();    		
    		
    		int index = bookView.getIndex();
    		int pos = bookView.getPosition();
    		
            if ( progress != null ) {            	
            	
            	if ( progress.getIndex() > index ) {
            		bookView.setIndex(progress.getIndex());
            		bookView.setPosition( progress.getProgress() );
            	} else if ( progress.getIndex() == index) {
            		pos = Math.max(pos, progress.getProgress());
            		bookView.setPosition(pos);
            	}            	
            	
            } 
            
            bookView.restore();            
    	}
    }    
}
