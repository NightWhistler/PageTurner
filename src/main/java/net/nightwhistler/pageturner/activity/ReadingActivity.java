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
import java.text.SimpleDateFormat;
import java.util.List;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.animation.Animations;
import net.nightwhistler.pageturner.animation.Animator;
import net.nightwhistler.pageturner.animation.PageCurlAnimator;
import net.nightwhistler.pageturner.animation.RollingBlindAnimator;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.sync.BookProgress;
import net.nightwhistler.pageturner.sync.ProgressService;
import net.nightwhistler.pageturner.view.AnimatedImageView;
import net.nightwhistler.pageturner.view.BookView;
import net.nightwhistler.pageturner.view.BookViewListener;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.globalmentor.android.widget.VerifiedFlingListener;
import com.google.inject.Inject;

public class ReadingActivity extends RoboActivity implements BookViewListener 
{
		
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
		
	private SharedPreferences settings;	
    
    private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
		
	private String bookTitle;
	private String titleBase;
	private String colourProfile;	
	private String fileName;
	private int progressPercentage;
	
	private boolean oldBrightness = false;
	private boolean oldStripWhiteSpace = false;
		
	private enum Orientation { HORIZONTAL, VERTICAL }
	
	private CharSequence selectedWord = null;
	
	private Handler handler;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Restore preferences
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.read_book);
        
        this.handler = new Handler();
        
        this.waitDialog = new ProgressDialog(this);
        this.waitDialog.setOwnerActivity(this);               
        
        this.gestureDetector = new GestureDetector(new SwipeListener());
        this.gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };   
        
    	this.viewSwitcher.setOnTouchListener(gestureListener);
    	this.bookView.setOnTouchListener(gestureListener);    	
    	this.bookView.addListener(this);
    	this.bookView.setSpanner(getInjector().getInstance(HtmlSpanner.class));
    	
    	this.oldBrightness = settings.getBoolean("set_brightness", false);
    	this.oldStripWhiteSpace = settings.getBoolean("strip_whitespace", false);
    	
    	registerForContextMenu(bookView);
    	
    	String file = getIntent().getStringExtra("file_name");
    	
        if ( file == null && getIntent().getData() != null ) {        
        	file = getIntent().getData().getPath();
        }
        
        if ( file == null ) {
        	file = settings.getString("last_file", "");
        }
        
        updateFromPrefs();
    	updateFileName(savedInstanceState, file); 
    	
    	if ( "".equals( fileName ) ) {        	
        	bookView.setText( getWelcomeText() );
        } else {        
        	String email = settings.getString("email", "").trim();
        	
        	if ( savedInstanceState == null &&  email.length() != 0 ) {
        		new DownloadProgressTask().execute();
        	} else {        	
        		bookView.restore();
        	}
        }
    	
    }
    
    private void updateFileName(Bundle savedInstanceState, String fileName) {
    	
    	this.fileName = fileName;
    	
    	int lastPos = -1;
    	int lastIndex = 0;        

    	if ( settings != null ) {
    		lastPos = settings.getInt(POS_KEY + fileName, -1 );
    		lastIndex = settings.getInt(IDX_KEY + fileName, -1 );    		
    	}   

    	if (savedInstanceState != null ) {
    		lastPos = savedInstanceState.getInt(POS_KEY, -1);
    		lastIndex = savedInstanceState.getInt(IDX_KEY, -1);
    	}       

    	this.bookView.setFileName(fileName);
    	this.bookView.setPosition(lastPos);
    	this.bookView.setIndex(lastIndex);
    	    	
    	//Slightly hacky
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_file", fileName);
        editor.commit();    

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
    	    	
    	this.progressService.setEmail( settings.getString("email", "") );
    	this.progressService.setDeviceName( settings.getString("device_name", Build.MODEL));
    	
    	Typeface face = Typeface.createFromAsset(getAssets(), "gen_bk_bas.ttf");
        this.bookView.setTypeface(face);
                
        int userTextSize = settings.getInt("itext_size", 16);                
        bookView.setTextSize( userTextSize );
        
        int marginH = settings.getInt("margin_h", 15);
        int marginV = settings.getInt("margin_v", 15);
        
        bookView.setHorizontalMargin(marginH);
        bookView.setVerticalMargin(marginV);
        
        if ( ! isRollingBlind() ) {
        	bookView.setEnableScrolling(settings.getBoolean("scrolling", false));
        }
        
        bookView.setStripWhiteSpace(settings.getBoolean("strip_whitespace", true));
        
        int lineSpacing = settings.getInt("line_spacing", 0);
        bookView.setLineSpacing(lineSpacing);             

        if ( settings.getBoolean("full_screen", false)) {
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
        if ( settings.getBoolean("set_brightness", false) != oldBrightness
        		|| settings.getBoolean("strip_whitespace", false) != oldStripWhiteSpace ) {
        	Intent intent = new Intent(this, ReadingActivity.class);
        	intent.setData(Uri.parse(this.fileName));
        	startActivity(intent);
        	finish();
        }
        
        String orientation = settings.getString("screen_orientation", "no_lock");
        
        if ( "portrait".equals(orientation) ) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if ( "landscape".endsWith(orientation) ) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
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
    public void bookOpened(Book book) {
    	
    	this.bookTitle = book.getTitle();
    	this.titleBase = this.bookTitle;
    	setTitle( titleBase );  
    	
    	new AddBookToLibraryTask().execute(book);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	
    	//This is a hack to give the longclick handler time
    	//to find the word the user long clicked on.    	
    	
    	if ( this.selectedWord != null ) {
    		
    		final CharSequence word = this.selectedWord;
    		menu.setHeaderTitle("You selected '" + selectedWord + "'");
    		
    		final Intent intent = new Intent(PICK_RESULT_ACTION);
        	intent.putExtra(EXTRA_QUERY, word.toString()); //Search Query
        	intent.putExtra(EXTRA_FULLSCREEN, false); //
        	intent.putExtra(EXTRA_HEIGHT, 400); //400pixel, if you don't specify, fill_parent"
        	intent.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM);
        	intent.putExtra(EXTRA_MARGIN_LEFT, 100);
        	
        	if ( isIntentAvailable(this, intent)) {
        		MenuItem item = menu.add("Look up in Dictionary");
        		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						startActivityForResult(intent, 5); 
						return true;
					}
				});
        	}
        	        
        	MenuItem newItem = menu.add("Search on Wikipedia");
        	newItem.setOnMenuItemClickListener(new BrowserSearchMenuItem(
					"http://en.wikipedia.org/wiki/Special:Search?search="
        			+ URLEncoder.encode( word.toString() )));
		            
        	MenuItem newItem2 = menu.add("Search on Google");
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

    	int brightness = 50;

    	if ( settings.getBoolean("night_mode", false)) {
    		this.colourProfile = "night";
    		brightness = settings.getInt("night_bright", 50);
    	} else {
    		this.colourProfile = "day";
    		brightness = settings.getInt("day_bright", 50);
    	}      


    	if ( "night".equals(this.colourProfile) ) {    		
    		this.bookView.setBackgroundColor(settings.getInt("night_bg", Color.BLACK));
    		this.viewSwitcher.setBackgroundColor(settings.getInt("night_bg", Color.BLACK));
    		this.bookView.setTextColor( settings.getInt("night_text", Color.GRAY));

    	} else {
    		this.bookView.setBackgroundColor(settings.getInt("day_bg", Color.WHITE));
    		this.viewSwitcher.setBackgroundColor(settings.getInt("day_bg", Color.WHITE));
    		this.bookView.setTextColor(settings.getInt("day_text", Color.BLACK));
    	}

    	if ( settings.getBoolean("set_brightness", false)) {
    		WindowManager.LayoutParams lp = getWindow().getAttributes();
    		lp.screenBrightness = (float) brightness / 100f;
    		getWindow().setAttributes(lp);
    	}    

    }
    
    @Override
    public void errorOnBookOpening(String errorMessage) {    	
    	this.waitDialog.hide();    	
    	String message = "Error opening book: " + errorMessage;
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
    	
    	this.waitDialog.setTitle("Loading, please wait");
    	this.waitDialog.show();
    }    
    
    
    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
	    int keyCode = event.getKeyCode();
	    
	    if ( isRollingBlind() ) {
	    	stopRollingBlind();
	    	return true;
	    }
	    
	    switch (keyCode) {
	    
	    	case KeyEvent.KEYCODE_VOLUME_DOWN:
	    		//Yes, this is nasty: if the setting is true, we fall through to the next case.
	    		if (! settings.getBoolean("nav_vol", false) ) { return false; }	    		
	        
	    	case KeyEvent.KEYCODE_DPAD_RIGHT:
	            
	        	if (action == KeyEvent.ACTION_DOWN) {
	                pageDown(Orientation.HORIZONTAL);	        		
	            }
	        	
	        	return true;	 

	        case KeyEvent.KEYCODE_VOLUME_UP:
		        //Same dirty trick.
	    		if (! settings.getBoolean("nav_vol", false) ) { return false; } 

	        case KeyEvent.KEYCODE_DPAD_LEFT:	
	            if (action == KeyEvent.ACTION_DOWN) {
	               pageUp(Orientation.HORIZONTAL);	            	
	            }
	            
	            return true;	
	            
	        case KeyEvent.KEYCODE_BACK:
	        	if ( action == KeyEvent.ACTION_DOWN && 
	        			bookView.hasPrevPosition() ) {
	        		bookView.goBackInHistory();
	        		
	        		return true;
	        	} else {
	        		this.finish();
	        	}
	        
	    }

	    return false;
    }   
    
    
    private boolean isRollingBlind() {
    	Animator anim = dummyView.getAnimator();
    	return anim != null && anim instanceof RollingBlindAnimator;
    }
    
    private void doRollingBlind() {
    	
    	if ( viewSwitcher.getCurrentView() == this.dummyView ) {
    		viewSwitcher.showNext();
    	}
    	
    	bookView.setKeepScreenOn(true);
    	
    	Bitmap before = getBookViewSnapshot();
    	
    	bookView.pageDown();
    	
    	Bitmap after = getBookViewSnapshot();    	
    	
    	RollingBlindAnimator anim = new RollingBlindAnimator();
    	anim.setAnimationSpeed( settings.getInt("blind_speed", 20) );
    	anim.setStepSize(2);
    	
    	anim.setBackgroundBitmap(before);
    	anim.setForegroudBitmap(after);
    	
    	dummyView.setAnimator(anim);
    	viewSwitcher.showNext();
    	
    	bookView.setEnableScrolling(false);
    	
    	handler.post( new RollerRunnable() );    	
    }
    
    private void doPageCurl(boolean flipRight) {
    	
    	if ( this.dummyView.getAnimator() == null ) {
    		
    		if ( viewSwitcher.getCurrentView() == this.dummyView ) {
        		viewSwitcher.showNext();
        	}
        	
    		Bitmap before = getBookViewSnapshot();
    		
    		PageCurlAnimator animator = new PageCurlAnimator(flipRight);    		
    		
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

        	handler.post( new PageCurlRunnable() );
    	} 
    	
    	dummyView.invalidate();    	
    	    	
    }
    
    
    private class PageCurlRunnable implements Runnable {
    	@Override
    	public void run() {
    		    		
    		if ( dummyView.getAnimator() == null ) {
    			LOG.debug( "BookView no longer has an animator. Aborting rolling blind." );
    			//stopRollingBlind();
    		} else {
    			
    			Animator anim = dummyView.getAnimator();
    			
    			if ( anim.isFinished() ) {
    				 				
    				if ( viewSwitcher.getCurrentView() == dummyView ) {
    					viewSwitcher.showNext();    					
    				}
    				
    				dummyView.setAnimator(null);   				
    				
    			} else {    			
    				anim.advanceOneFrame();
    				dummyView.invalidate();
    				
    				int delay = 1000 / anim.getAnimationSpeed();
    			
    				handler.postDelayed(this, delay);
    			} 
    		}    		
    	}
    }   
    
    private class RollerRunnable implements Runnable {
    	@Override
    	public void run() {
    		    		
    		if ( dummyView.getAnimator() == null ) {
    			LOG.debug( "BookView no longer has an animator. Aborting rolling blind." );
    			stopRollingBlind();
    		} else {
    			
    			Animator anim = dummyView.getAnimator();
    			
    			if ( anim.isFinished() ) {
    				doRollingBlind();
    			} else {    			
    				anim.advanceOneFrame();
    				dummyView.invalidate();
    				
    				int delay = 1000 / anim.getAnimationSpeed();
    			
    				handler.postDelayed(this, delay);
    			} 
    		}    		
    	}
    }
    
    private void stopRollingBlind() {
    	
    	this.dummyView.setAnimator(null);
    	
    	if ( viewSwitcher.getCurrentView() == this.dummyView ) {
    		viewSwitcher.showNext();
    	}
    	
    	bookView.setEnableScrolling( settings.getBoolean("scrolling", false));
    	
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
    		restoreBackgroundColour();	
    	}	
    	
    	return null;
    }
    
    private void prepareSlide(Animation inAnim, Animation outAnim) {
    	    	    	    	
    	dummyView.setVisibility(View.VISIBLE);
    	
    	Bitmap bitmap = getBookViewSnapshot();
    	
    	if ( bitmap != null ) {
    		dummyView.setImageBitmap(bitmap);
    	}    		
    	
    	viewSwitcher.layout(0, 0, viewSwitcher.getWidth(), viewSwitcher.getHeight() );
    	this.viewSwitcher.showNext();
    	
		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);		
    }
    
    private void restoreBackgroundColour() {    	
		if ( this.colourProfile.equals("day") ) {
			this.viewSwitcher.setBackgroundColor(settings.getInt("day_bg", Color.WHITE));
		} else {
			this.viewSwitcher.setBackgroundColor(settings.getInt("night_bg", Color.BLACK));
		}
    }
    
    private void pageDown(Orientation o) {
    	
    	stopRollingBlind();
    	
    	String animH = settings.getString("h_animation", "curl");
    	String animV = settings.getString("v_animation", "slide");
    	
    	if ( o == Orientation.HORIZONTAL  ) {
    		
    		if ( "curl".equals(animH) ) {
    			doPageCurl(true);
    		} else if ("slide".equals(animH) ) {
    			prepareSlide(Animations.inFromRightAnimation(), Animations.outToLeftAnimation());
        		this.viewSwitcher.showNext();
        		bookView.pageDown();
    		} else {
    			bookView.pageDown();
    		}
    		
    	} else if ( "slide".equals(animV) ){
    		prepareSlide(Animations.inFromBottomAnimation(), Animations.outToTopAnimation() );    		
    		this.viewSwitcher.showNext();
    		bookView.pageDown();
    	}    	    	
		
    }
    
    private void pageUp(Orientation o) {
    	
    	stopRollingBlind();

    	String animH = settings.getString("h_animation", "curl");
    	String animV = settings.getString("v_animation", "slide");
    	
    	if ( o == Orientation.HORIZONTAL  ) {
    		
    		if ( "curl".equals(animH) ) {
    			doPageCurl(false);
    		} else if ("slide".equals(animH) ) {
    			prepareSlide(Animations.inFromLeftAnimation(), Animations.outToRightAnimation()); 
        		this.viewSwitcher.showNext();
        		bookView.pageUp();
    		} else {
    			bookView.pageUp();
    		}
    		
    	} else if ( "slide".equals(animV) ){
    		prepareSlide(Animations.inFromTopAnimation(), Animations.outToBottomAnimation());    		
    		this.viewSwitcher.showNext();
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
    	sync.setEnabled( ! "".equals( settings.getString("email", "")) );
    	
    	if ( this.colourProfile.equals("day") ) {
    		dayMode.setVisible(false);
    		nightMode.setVisible(true);
    	} else {
    		dayMode.setVisible(true);
    		nightMode.setVisible(false);
    	}    
    	
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	this.titleBarLayout.setVisibility(View.VISIBLE);
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public void onOptionsMenuClosed(Menu menu) {
    	if ( settings.getBoolean("full_screen", false)) {
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
    	setTitle("PageTurner");
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
    	   progressService.storeProgress(this.fileName,
    			   this.bookView.getIndex(), this.bookView.getPosition(), this.progressPercentage );
    	   
    	   // We need an Editor object to make preference changes.
    	   // All objects are from android.context.Context    	   
    	   SharedPreferences.Editor editor = settings.edit();
    	   editor.putInt(POS_KEY + this.fileName, this.bookView.getPosition());
    	   editor.putInt(IDX_KEY + this.fileName, this.bookView.getIndex());    	   

    	   // Commit the edits!
    	   editor.commit();
       }
       
       this.waitDialog.dismiss();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reading_menu, menu);        
        
        return true;
    }   
    
    private void setProfile(String profileName) {    	
    	        
        SharedPreferences.Editor editor = this.settings.edit();
    	editor.putBoolean("night_mode", ! this.colourProfile.equals("night"));
    	editor.commit();   
    	
        this.restoreColorProfile();    	
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        // Handle item selection
        switch (item.getItemId()) {
     
        case R.id.profile_night:
        	setProfile("night");            
            return true;
            
        case R.id.profile_day:
        	setProfile("day");            
            return true;
            
        case R.id.manual_sync:
        	new ManualProgressSync().execute();
        	return true;
        	
        case R.id.preferences:
        	
        	oldBrightness = settings.getBoolean("set_brightness", false);
        	oldStripWhiteSpace = settings.getBoolean("strip_whitespace", false);
        	
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
        	doRollingBlind();
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void launchFileManager() {
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
                startActivityForResult(intent, REQUEST_CODE_GET_CONTENT);
        } catch (ActivityNotFoundException e) {
                // No compatible file manager was found.
                Toast.makeText(this, "Please install OI File Manager from the Android Market.", 
                                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void launchLibrary() {
    	Intent intent = new Intent(this, LibraryActivity.class);
    	startActivity(intent);    	
    }    
    
    private void showPickProgressDialog( final List<BookProgress> results ) {

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Stored sync points");    	
    	
    	ProgressListAdapter adapter = new ProgressListAdapter(results);
    	
    	builder.setAdapter(adapter, adapter);

    	AlertDialog dialog = builder.create();
    	dialog.setOwnerActivity(this);
    	dialog.show();
    }
    
    private class ProgressListAdapter extends ArrayAdapter<BookProgress> implements 
    	DialogInterface.OnClickListener {
    	
    	private List<BookProgress> books;
    	
    	public ProgressListAdapter(List<BookProgress> books) {
    		super(ReadingActivity.this, R.id.deviceName, books);
    		this.books = books;
		}
    	
    	@Override
    	public void onClick(DialogInterface dialog, int which) {
    		BookProgress progress = books.get(which);
			bookView.navigateTo(progress.getIndex(), progress.getProgress() );    		
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		
    		View rowView;
    		
    		if ( convertView == null ) {
    			LayoutInflater inflater = (LayoutInflater) 
    				getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			
    			rowView = inflater.inflate(R.layout.progress_row, parent, false);
    		} else {
    			rowView = convertView;
    		}
    		
    		TextView deviceView = (TextView) rowView.findViewById(R.id.deviceName);
    		TextView dateView = (TextView) rowView.findViewById(R.id.timeStamp );
    		
    		BookProgress progress = books.get(position);
    		
    		deviceView.setTextColor( Color.BLACK );
    		deviceView.setText( progress.getDeviceName() + " - " + progress.getPercentage() + "%" );
    		dateView.setText( new SimpleDateFormat().format(progress.getTimeStamp()) );
    		
    		return rowView;
    		
    	}
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
    	builder.setTitle("Table of contents");

    	builder.setItems(items, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int item) {
    			bookView.navigateTo( tocList.get(item).getHref() );
    		}
    	});

    	this.tocDialog = builder.create();
    	this.tocDialog.setOwnerActivity(this);

    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	if ( this.bookView != null ) {
    		progressService.storeProgress(this.fileName,
     			   this.bookView.getIndex(), this.bookView.getPosition(), this.progressPercentage);
     	   
    		outState.putInt(POS_KEY, this.bookView.getPosition() );  
    		outState.putInt(IDX_KEY, this.bookView.getIndex());
    		
    		this.libraryService.close();
    	}
    }
    
    private class AddBookToLibraryTask extends AsyncTask<Book, Integer, Void> {
    	@Override
    	protected Void doInBackground(Book... params) {
    		
    		Book book = params[0];		
        	libraryService.storeBook(fileName, book, true );
    		
    		return null;
    	}
    }
    
    private class ManualProgressSync extends AsyncTask<Void, Integer, List<BookProgress>> {
    	
    	@Override
    	protected void onPreExecute() {
    		waitDialog.setTitle("Syncing progress...");
    		waitDialog.show();
    	}
    	
    	@Override
    	protected List<BookProgress> doInBackground(Void... params) {    		
    		return progressService.getProgress(fileName);    		
    	}
    	
    	@Override
    	protected void onPostExecute(List<BookProgress> progress) {  
    		waitDialog.hide();    		
    		
    		if ( progress == null ) {
    			
    		    AlertDialog alertDialog = new AlertDialog.Builder(ReadingActivity.this).create();
   		        alertDialog.setTitle("Sync failed");
   		        alertDialog.setMessage("Could not connect to the sync server.");

   		        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
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
    		waitDialog.setTitle("Syncing progress...");
    		waitDialog.show();
    	}
    	
    	@Override
    	protected BookProgress doInBackground(Void... params) {    		
    		List<BookProgress> updates = progressService.getProgress(fileName);
    		
    		if ( updates != null && updates.size() > 0 ) {
    			return updates.get(0);
    		}
    		
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
    
    private Spanned getWelcomeText() {
    	SpannableStringBuilder builder = new SpannableStringBuilder();
    	
    	builder.append( Html.fromHtml("<h1>Welcome to PageTurner</h1>"));
    	builder.append("\uFFFC");
    	Drawable logo = getResources().getDrawable(R.drawable.page_turner);
    	
    	logo.setBounds(0, 0, logo.getIntrinsicWidth(), logo.getIntrinsicHeight() );
    	builder.setSpan(new ImageSpan(logo), 
    			builder.length() -1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    	builder.setSpan(new AlignmentSpan() {
    		@Override
    		public Alignment getAlignment() {
    			return Alignment.ALIGN_CENTER;
    		}
    	}, builder.length() -1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    	
    	builder.append("\n\nPlease select \"Open book\" from the menu to start reading.");
    	
    	return builder;
    }
    
    
    private class SwipeListener extends VerifiedFlingListener {
    	
    	public SwipeListener() {
    		super(ReadingActivity.this);
		}    	
      
    	@Override
    	public boolean onVerifiedFling(MotionEvent e1, MotionEvent e2,
    			float velocityX, float velocityY) {
    		
    		stopRollingBlind();
    		
    		boolean swipeH = settings.getBoolean("nav_swipe_h", true);
    		boolean swipeV = settings.getBoolean("nav_swipe_v", true) && ! settings.getBoolean("scrolling", true);
    		
    		if ( swipeH && velocityX > 0 ) {
    			pageUp(Orientation.HORIZONTAL);
    			return true;
    		} else if ( swipeH && velocityX < 0 ) {
    			pageDown(Orientation.HORIZONTAL);
    			return true;
    		} else if ( swipeV && velocityY < 0 ) {
    			pageDown( Orientation.VERTICAL );
    			return true;
    		} else if ( swipeV && velocityY > 0 ) {
    			pageUp( Orientation.VERTICAL );
    			return true;
    		}
    		
    		return false;
    	}
        
    	@Override
    	public boolean onSingleTapConfirmed(MotionEvent e) {
    		
    		stopRollingBlind();
    		
        	boolean tapH = settings.getBoolean("nav_tap_h", true);
        	boolean tapV = settings.getBoolean("nav_tap_v", true);
        	
        	final int TAP_RANGE_H = bookView.getWidth() / 5;
        	final int TAP_RANGE_V = bookView.getHeight() / 5;
        	if ( tapH ) {
        		if ( e.getX() < TAP_RANGE_H ) {
        			pageUp(Orientation.HORIZONTAL);
        			return true;
        		} else if (e.getX() > bookView.getWidth() - TAP_RANGE_H ) {
        			pageDown(Orientation.HORIZONTAL);
        			return true;
        		}
        	}
        	
        	int yBase = bookView.getScrollY();        	
        	
        	if ( tapV ) {
        		if ( e.getY() < TAP_RANGE_V + yBase ) {
        			pageUp(Orientation.VERTICAL);
        			return true;
        		} else if ( e.getY() > (yBase + bookView.getHeight()) - TAP_RANGE_V ) {
        			pageDown(Orientation.VERTICAL);
        			return true;
        		}
        	}
        	
        	return false;        	
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
        	CharSequence word = bookView.getWordAt(e.getX(), e.getY() );
        	selectedWord = word;

        	openContextMenu(bookView);
        }
        
        
	}

}
