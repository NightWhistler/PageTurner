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

import java.util.List;

import net.nightwhistler.pageturner.sync.BookProgress;
import net.nightwhistler.pageturner.sync.OpenKeyvalProgressService;
import net.nightwhistler.pageturner.sync.ProgressService;
import nl.siegmann.epublib.domain.Book;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.Layout.Alignment;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class ReadingActivity extends Activity implements BookViewListener 
{
		
	private static final String POS_KEY = "offset:";
	private static final String IDX_KEY = "index:";
	
	protected static final int REQUEST_CODE_GET_CONTENT = 2;
	
	private String colourProfile;
		
	private String fileName;
	
	private ProgressService progressService;	
	private ProgressDialog waitDialog;
	private AlertDialog tocDialog;
	
	private ViewSwitcher viewSwitcher;
	private BookView bookView;
	
	private SharedPreferences settings;
		
	private static final int SWIPE_MIN_DISTANCE = 100;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
	
	private boolean animatePageChanges;
	
	private String bookTitle;
	private String titleBase;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Restore preferences
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        if ( settings.getBoolean("full_screen", false)) {
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } 
        
        setContentView(R.layout.read_book);
        
        this.waitDialog = new ProgressDialog(this);
        this.waitDialog.setOwnerActivity(this);
        this.viewSwitcher = (ViewSwitcher) findViewById(R.id.mainContainer);
        
        this.progressService = new OpenKeyvalProgressService( settings.getString("email", "") );  
        
        this.gestureDetector = new GestureDetector(new SwipeListener());
        this.gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
       
    	this.bookView = (BookView) this.findViewById(R.id.bookView);
    	
    	this.viewSwitcher.setOnTouchListener(gestureListener);
    	this.bookView.setOnTouchListener(gestureListener);    	
    	this.bookView.addListener(this);
    	
    	String file = getIntent().getStringExtra("file_name");
    	
        if ( file == null && getIntent().getData() != null ) {        
        	file = getIntent().getData().getPath();
        }
        
        if ( file == null ) {
        	file = settings.getString("last_file", "");
        }
        
    	updateFileName(savedInstanceState, file);
    	updateFromPrefs();
        
        new DownloadProgressTask().execute();        
    }
    
    private void updateFileName(Bundle savedInstanceState, String fileName) {
    	
    	this.fileName = fileName;
    	
    	int lastPos = -1;
    	int lastIndex = 0;        

    	if ( settings != null ) {
    		lastPos = settings.getInt(POS_KEY + fileName, -1 );
    		lastIndex = settings.getInt(IDX_KEY + fileName, -1 );
    		this.colourProfile = settings.getString("profile", "day");
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
    	
    	String title = this.titleBase;
    	    	
    	title = title + " " + progressPercentage + "%";    	
    	
    	setTitle(title);
    }
    
    private void updateFromPrefs() {
    	Typeface face = Typeface.createFromAsset(getAssets(), "gen_bk_bas.ttf");
        this.bookView.setTypeface(face);
                
        String userTextSize = settings.getString("text_size", "16");
        
        float textSize = 16f;
        
        try {
        	textSize = Float.parseFloat( userTextSize );
        }catch (NumberFormatException nfe){}
        
        bookView.setTextSize( textSize );
        
        bookView.setEnableScrolling(settings.getBoolean("scrolling", false));
        
        if ( settings.getBoolean("night_mode", false)) {
        	this.colourProfile = "night";
        } else {
        	this.colourProfile = "day";
        }
        
        this.animatePageChanges = settings.getBoolean("animations", false);
        
        restoreColorProfile();
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
    }
    
    private void restoreColorProfile() {
    	if ( "night".equals(this.colourProfile) ) {
    		this.viewSwitcher.setBackgroundColor(Color.BLACK);
    		this.bookView.setBackgroundColor(Color.BLACK);
    		this.bookView.setTextColor( Color.GRAY );
    		
    	} else {
    		this.viewSwitcher.setBackgroundColor(Color.WHITE);
    		this.bookView.setBackgroundColor(Color.WHITE);
    		this.bookView.setTextColor(Color.BLACK);
    	}    	 	
    }
    
    @Override
    public void errorOnBookOpening(String errorMessage) {    	
    	this.waitDialog.hide();    	
    	String message = "Error opening book: " + errorMessage;
        bookView.setText(message);    	
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
	    
	    switch (keyCode) {
	        
	    	case KeyEvent.KEYCODE_VOLUME_DOWN:	        
	        case KeyEvent.KEYCODE_DPAD_RIGHT:
	            
	        	if (action == KeyEvent.ACTION_DOWN) {
	                slideToLeft();	                
	            }
	        	
	        	return true;	            
	            
	        	
	        case KeyEvent.KEYCODE_VOLUME_UP:	        
	        case KeyEvent.KEYCODE_DPAD_LEFT:	
	            if (action == KeyEvent.ACTION_DOWN) {
	               slideToRight();	                
	            }
	            
	            return true;	
	            
	        case KeyEvent.KEYCODE_BACK:
	        	if ( action == KeyEvent.ACTION_DOWN && 
	        			bookView.hasPrevPosition() ) {
	        		bookView.goBackInHistory();
	        		
	        		return true;
	        	}
	        
	    }

	    return super.dispatchKeyEvent(event);    	
    }
    
    private void prepareSlide(Animation inAnim, Animation outAnim) {
    	
    	View otherView = findViewById(R.id.dummyView);
    	otherView.setVisibility(View.INVISIBLE);
    	
    	bookView.layout(0, 0, viewSwitcher.getWidth(), viewSwitcher.getHeight());
    	
    	bookView.buildDrawingCache(false);
		Bitmap drawingCache = bookView.getDrawingCache();		
		  		
		Bitmap copy = drawingCache.copy(drawingCache.getConfig(), false);
		bookView.destroyDrawingCache();
		
				
		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);
		
		this.viewSwitcher.setBackgroundDrawable( new BitmapDrawable(copy) );
		
		//Set the second child forward, which is an empty TextView (i.e. invisible)
		//this.viewFlipper.setDisplayedChild(1);
		this.viewSwitcher.reset();
    }
    
    private void slideToLeft() {
    	if ( animatePageChanges ) {
    		prepareSlide(inFromRightAnimation(), outToLeftAnimation());		
    		this.viewSwitcher.showNext();
    		this.viewSwitcher.showNext();
    	}
    	
		bookView.pageDown();
    }
    
    private void slideToRight() {
    	if ( animatePageChanges ) {
    		prepareSlide(inFromLeftAnimation(), outToRightAnimation());		
    		this.viewSwitcher.showNext(); 
    		this.viewSwitcher.showNext();
    	}
    	
		bookView.pageUp();
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem nightMode = menu.findItem(R.id.profile_night);
    	MenuItem dayMode = menu.findItem(R.id.profile_day);
    	
    	if ( this.colourProfile.equals("day") ) {
    		dayMode.setVisible(false);
    		nightMode.setVisible(true);
    	} else {
    		dayMode.setVisible(true);
    		nightMode.setVisible(false);
    	}
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
    /**
     * This is called after the file manager finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if (resultCode == RESULT_OK && data != null) {
    		// obtain the filename
    		Uri fileUri = data.getData();
    		if (fileUri != null) {
    			String filePath = fileUri.getPath();
    			if (filePath != null) {
    				setTitle("PageTurner");
    				bookView.clear();
    				updateFileName(null, filePath);
    				new DownloadProgressTask().execute();     
    			}
    		}
    	}    	
    }
    
    @Override
    protected void onStop(){
       super.onStop();

       if ( this.bookView != null ) {
    	   progressService.storeProgress(this.fileName,
    			   this.bookView.getIndex(), this.bookView.getPosition());
    	   
    	   // We need an Editor object to make preference changes.
    	   // All objects are from android.context.Context    	   
    	   SharedPreferences.Editor editor = settings.edit();
    	   editor.putInt(POS_KEY + this.fileName, this.bookView.getPosition());
    	   editor.putInt(IDX_KEY + this.fileName, this.bookView.getIndex());    	   

    	   // Commit the edits!
    	   editor.commit();
       }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reading_menu, menu);
        return true;
    }   
    
    private void setProfile(String profileName) {    	
    	
        this.colourProfile = profileName;
        
        SharedPreferences.Editor editor = this.settings.edit();
    	editor.putBoolean("night_mode", this.colourProfile.equals("night"));
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
        	new DownloadProgressTask().execute();
        	return true;
        	
        case R.id.preferences:
        	Intent i = new Intent(this, PageTurnerPrefsActivity.class);
        	startActivity(i);
        	return true;
        	
        case R.id.show_toc:
        	initTocDialog();
        	this.tocDialog.show();
        	return true;
        	
        case R.id.open_file:
        	launchFileManager();
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
    
    private void initTocDialog() {

    	if ( this.tocDialog != null ) {
    		return;
    	}

    	final List<BookView.TocEntry> tocList = this.bookView.getTableOfContents();

    	if ( tocList == null ) {
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
     			   this.bookView.getIndex(), this.bookView.getPosition());
     	   
    		outState.putInt(POS_KEY, this.bookView.getPosition() );  
    		outState.putInt(IDX_KEY, this.bookView.getIndex());
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
    		return progressService.getProgress(fileName);
    	}
    	
    	@Override
    	protected void onPostExecute(BookProgress progress) {  
    		waitDialog.hide();
    		
            if ( progress != null ) {
            	bookView.setIndex(  progress.getIndex() );
            	bookView.setPosition( progress.getProgress() );
            }
            
            if ( ! "".equals( fileName ) ) {
            	bookView.restore();
            } else {
            	bookView.setText( getWelcomeText() );
            }
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
    
    private Animation inFromRightAnimation() {

    	Animation inFromRight = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  +1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	inFromRight.setDuration(500);
    	inFromRight.setInterpolator(new AccelerateInterpolator());
    	return inFromRight;
    }
    
    private Animation outToLeftAnimation() {
    	Animation outtoLeft = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  -1.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	outtoLeft.setDuration(500);
    	outtoLeft.setInterpolator(new AccelerateInterpolator());
    	return outtoLeft;
    }

    private Animation inFromLeftAnimation() {
    	Animation inFromLeft = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  -1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	inFromLeft.setDuration(500);
    	inFromLeft.setInterpolator(new AccelerateInterpolator());
    	return inFromLeft;
    }
    
    private Animation outToRightAnimation() {
    	Animation outtoRight = new TranslateAnimation(
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  +1.0f,
    			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	outtoRight.setDuration(500);
    	outtoRight.setInterpolator(new AccelerateInterpolator());
    	return outtoRight;
    }
    
    private class SwipeListener extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) < SWIPE_MAX_OFF_PATH) {                   
                	// right to left swipe
                	if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                		slideToLeft();
                		return true;
                	}  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                		slideToRight();
                		return true;
                	}
                }     
                               
            } catch (Exception e) {
                // nothing
            }
            
            return false;
        }
        
        
	}

}
