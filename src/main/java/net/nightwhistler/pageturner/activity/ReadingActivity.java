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
import net.nightwhistler.htmlspanner.spans.CenterSpan;
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
import net.nightwhistler.pageturner.tasks.SearchTextTask;
import net.nightwhistler.pageturner.view.AnimatedImageView;
import net.nightwhistler.pageturner.view.NavGestureDetector;
import net.nightwhistler.pageturner.view.ProgressListAdapter;
import net.nightwhistler.pageturner.view.SearchResultAdapter;
import net.nightwhistler.pageturner.view.bookview.BookView;
import net.nightwhistler.pageturner.view.bookview.BookViewListener;
import net.nightwhistler.pageturner.view.bookview.TextSelectionCallback;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import com.google.inject.Inject;

public class ReadingActivity extends RoboSherlockActivity implements
		BookViewListener, TextSelectionCallback {

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

	private static final Logger LOG = LoggerFactory
			.getLogger(ReadingActivity.class);

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
	private RelativeLayout titleBarLayout;

	@InjectView(R.id.titleProgress)
	private SeekBar progressBar;

	@InjectView(R.id.percentageField)
	private TextView percentageField;

	@InjectView(R.id.authorField)
	private TextView authorField;

	@InjectView(R.id.dummyView)
	private AnimatedImageView dummyView;
	
	@InjectView(R.id.pageNumberView)
	private TextView pageNumberView;

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
	private String oldFontName = "";
	private boolean oldUsePageNum = false;
	private boolean oldFullscreen = false;

	private enum Orientation {
		HORIZONTAL, VERTICAL
	}

	private CharSequence selectedWord = null;

	private Handler uiHandler;
	private Handler backgroundHandler;

	private Toast brightnessToast;

	private BroadcastReceiver mReceiver = new ScreenReceiver();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(RoboGuice.getInjector(this).getInstance(Configuration.class)
				.getTheme());
		super.onCreate(savedInstanceState);

		// Restore preferences
		setContentView(R.layout.read_book);
		this.uiHandler = new Handler();
		
		this.bookView.init();

		HandlerThread bgThread = new HandlerThread("background");
		bgThread.start();
		this.backgroundHandler = new Handler(bgThread.getLooper());

		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		this.waitDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				// This just consumes all key events and does nothing.
				return true;
			}
		});

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		this.gestureDetector = new GestureDetector(new NavGestureDetector(
				bookView, this, metrics));

		this.gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};

		this.progressBar.setFocusable(true);
		this.progressBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					private int seekValue;

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						bookView.navigateToPercentage(this.seekValue);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (fromUser) {
							seekValue = progress;
							percentageField.setText(progress + "% ");
						}
					}
				});

		this.viewSwitcher.setOnTouchListener(gestureListener);
		this.bookView.setOnTouchListener(gestureListener);
		this.bookView.setConfiguration(config);

		this.bookView.addListener(this);
		this.bookView.setSpanner(RoboGuice.getInjector(this).getInstance(
				HtmlSpanner.class));
		this.bookView.setTextSelectionCallback(this);

		this.oldBrightness = config.isBrightnessControlEnabled();
		this.oldStripWhiteSpace = config.isStripWhiteSpaceEnabled();
		this.oldFontName = config.getFontFamily().getName();
		this.oldUsePageNum = config.isShowPageNumbers();
		this.oldFullscreen = config.isFullScreenEnabled();

		registerForContextMenu(bookView);

		String file = getIntent().getStringExtra("file_name");

		if (file == null && getIntent().getData() != null) {
			file = getIntent().getData().getPath();
		}

		if (file == null) {
			file = config.getLastOpenedFile();
		}

		updateFromPrefs();
		updateFileName(savedInstanceState, file);

		if ("".equals(fileName)) {

			Intent intent = new Intent(this, LibraryActivity.class);
			startActivity(intent);
			finish();
			return;

		} else {

			if (savedInstanceState == null && config.isSyncEnabled()) {
				new DownloadProgressTask().execute();
			} else {
				bookView.restore();
			}

		}

	}

	/*
	 * @see roboguice.activity.RoboActivity#onPause()
	 */
	@Override
	protected void onPause() {
		// when the screen is about to turn off
		if (ScreenReceiver.wasScreenOn) {
			// this is the case when onPause() is called by the system due to a
			// screen state change
			sendProgressUpdateToServer();
		} else {
			// this is when onPause() is called when the screen state has not
			// changed
		}

		unregisterReceiver(mReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		registerReceiver();
		super.onResume();
	}

	private void registerReceiver() {
		// initialize receiver
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);

		registerReceiver(mReceiver, filter);
	}

	private void updateFileName(Bundle savedInstanceState, String fileName) {

		this.fileName = fileName;

		int lastPos = config.getLastPosition(fileName);
		int lastIndex = config.getLastIndex(fileName);

		if (savedInstanceState != null) {
			lastPos = savedInstanceState.getInt(POS_KEY, lastPos);
			lastIndex = savedInstanceState.getInt(IDX_KEY, lastIndex);
		}

		this.bookView.setFileName(fileName);
		this.bookView.setPosition(lastPos);
		this.bookView.setIndex(lastIndex);

		config.setLastOpenedFile(fileName);
	}

	/**
	 * Immediately updates the text size in the BookView, and saves the
	 * preference in the background.
	 * 
	 * @param textSize
	 */
	private void updateTextSize(final float textSize) {
		bookView.setTextSize(textSize);
		backgroundHandler.post(new Runnable() {

			@Override
			public void run() {
				config.setTextSize((int) textSize);
			}
		});
	}

	@Override
	public void progressUpdate(int progressPercentage, int pageNumber,
			int totalPages) {

		// Work-around for calculation errors and weird values.
		if (progressPercentage < 0 || progressPercentage > 100) {
			return;
		}
		
		this.progressPercentage = progressPercentage;
		
		if ( config.isShowPageNumbers() ) {		
			percentageField.setText("" + progressPercentage + "%  " + pageNumber
				+ " / " + totalPages);
			displayPageNumber(pageNumber);
			
		} else {
			percentageField.setText("" + progressPercentage + "%" );
		}

		this.progressBar.setProgress(progressPercentage);
		this.progressBar.setMax(100);
	}
	
	private void displayPageNumber(int pageNumber) {
		SpannableStringBuilder builder = new SpannableStringBuilder( Integer.toString(pageNumber) );
		builder.setSpan(new CenterSpan(), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
		pageNumberView.setTextColor(config.getTextColor());
		pageNumberView.setTextSize(config.getTextSize());
		
		pageNumberView.setTypeface(config.getFontFamily().getDefaultTypeface());
		
		pageNumberView.setText(builder);
	}

	private void updateFromPrefs() {

		this.progressService.setConfig(this.config);

		bookView.setTextSize(config.getTextSize());

		int marginH = config.getHorizontalMargin();
		int marginV = config.getVerticalMargin();

		this.bookView.setFontFamily(config.getFontFamily());

		bookView.setHorizontalMargin(marginH);
		bookView.setVerticalMargin(marginV);

		if (!isAnimating()) {
			bookView.setEnableScrolling(config.isScrollingEnabled());
		}

		bookView.setStripWhiteSpace(config.isStripWhiteSpaceEnabled());
		bookView.setLineSpacing(config.getLineSpacing());

		if (config.isFullScreenEnabled()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getSupportActionBar().hide();
		} else {
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getSupportActionBar().show();
		}

		if (config.isKeepScreenOn()) {
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		restoreColorProfile();

		// Check if we need a restart
		if (	config.isFullScreenEnabled() != oldFullscreen
				|| config.isShowPageNumbers() != oldUsePageNum
				|| config.isBrightnessControlEnabled() != oldBrightness
				|| config.isStripWhiteSpaceEnabled() != oldStripWhiteSpace
				|| !this.oldFontName.equalsIgnoreCase(config.getFontFamily()
						.getName())) {
			restartActivity();
		}

		Configuration.OrientationLock orientation = config
				.getScreenOrientation();

		switch (orientation) {
		case PORTRAIT:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case LANDSCAPE:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case REVERSE_LANDSCAPE:
			setRequestedOrientation(8); // Android 2.3+ value
			break;
		case REVERSE_PORTRAIT:
			setRequestedOrientation(9); // Android 2.3+ value
			break;
		default:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
	}

	private void restartActivity() {

		onStop();
		Intent intent = new Intent(this, ReadingActivity.class);
		intent.setData(Uri.parse(this.fileName));
		startActivity(intent);
		finish();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus) {
			updateFromPrefs();
		} else {
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
		setTitle(titleBase);
		this.titleBar.setText(titleBase);

		if (book.getMetadata() != null
				&& !book.getMetadata().getAuthors().isEmpty()) {
			Author author = book.getMetadata().getAuthors().get(0);
			this.authorField.setText(author.getFirstname() + " "
					+ author.getLastname());
		}

		backgroundHandler.post(new Runnable() {

			@Override
			public void run() {

				try {
					libraryService.storeBook(fileName, book, true,
							config.isCopyToLibrayEnabled());
				} catch (Exception io) {
					LOG.error("Copy to library failed.", io);
				}
			}
		});

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {

		// This is a hack to give the longclick handler time
		// to find the word the user long clicked on.

		if (this.selectedWord != null) {

			final CharSequence word = this.selectedWord;

			String header = String.format(getString(R.string.word_select),
					selectedWord);
			menu.setHeaderTitle(header);

			if (isDictionaryAvailable()) {
				android.view.MenuItem item = menu
						.add(getString(R.string.dictionary_lookup));
				item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(android.view.MenuItem item) {
						lookupDictionary(word.toString());
						return true;
					}
				});
			}

			android.view.MenuItem newItem = menu
					.add(getString(R.string.wikipedia_lookup));
			newItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(android.view.MenuItem item) {
					lookupWikipedia(word.toString());
					return true;
				}
			});

			android.view.MenuItem newItem2 = menu
					.add(getString(R.string.google_lookup));
			newItem2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(android.view.MenuItem item) {
					lookupGoogle(word.toString());
					return true;
				}
			});

			this.selectedWord = null;
		}
	}

	@Override
	public void highLight(int from, int to, Color color) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDictionaryAvailable() {
		return isIntentAvailable(this, getDictionaryIntent());
	}

	@Override
	public void lookupDictionary(String text) {
		Intent intent = getDictionaryIntent();
		intent.putExtra(EXTRA_QUERY, text); // Search Query
		startActivityForResult(intent, 5);
	}

	@Override
	public void lookupWikipedia(String text) {
		openBrowser("http://en.wikipedia.org/wiki/Special:Search?search="
				+ URLEncoder.encode(text));
	}

	@Override
	public void lookupGoogle(String text) {
		openBrowser("http://www.google.com/search?q=" + URLEncoder.encode(text));
	}

	private Intent getDictionaryIntent() {
		final Intent intent = new Intent(PICK_RESULT_ACTION);

		intent.putExtra(EXTRA_FULLSCREEN, false); //
		intent.putExtra(EXTRA_HEIGHT, 400); // 400pixel, if you don't specify,
											// fill_parent"
		intent.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM);
		intent.putExtra(EXTRA_MARGIN_LEFT, 100);

		return intent;
	}

	private void openBrowser(String url) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	public static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void restoreColorProfile() {

		this.bookView.setBackgroundColor(config.getBackgroundColor());
		this.viewSwitcher.setBackgroundColor(config.getBackgroundColor());
		this.bookView.setTextColor(config.getTextColor());
		this.bookView.setLinkColor(config.getLinkColor());

		int brightness = config.getBrightNess();

		if (config.isBrightnessControlEnabled()) {
			setScreenBrightnessLevel(brightness);
		}
	}

	private void setScreenBrightnessLevel(int level) {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = (float) level / 100f;
		getWindow().setAttributes(lp);
	}

	@Override
	public void errorOnBookOpening(String errorMessage) {
		this.waitDialog.hide();
		String message = String.format(getString(R.string.error_open_bk),
				errorMessage);
		bookView.setText(new SpannedString(message));
	}

	@Override
	public void parseEntryComplete(int entry, String name) {
		if (name != null && !name.equals(this.bookTitle)) {
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
		this.waitDialog.setMessage(null);
		this.waitDialog.show();
	}

	@Override
	public void calculatingPageNumbers() {
		this.waitDialog.setTitle(R.string.calc_page_num);
		this.waitDialog.setMessage(getString(R.string.calc_page_num_desc));
	}

	@Override
	public void readingFile() {
		this.waitDialog.setTitle(R.string.opening_file);
		this.waitDialog.setMessage(null);
	}

	@Override
	public void renderingText() {
		this.waitDialog.setTitle(R.string.loading_text);
		this.waitDialog.setMessage(null);

	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	private boolean handleVolumeButtonEvent(KeyEvent event) {

		if (!config.isVolumeKeyNavEnabled()) {
			return false;
		}

		boolean invert = false;

		int rotation = Surface.ROTATION_0;

		if (Build.VERSION.SDK_INT >= 8) {
			Display display = this.getWindowManager().getDefaultDisplay();
			rotation = display.getRotation();
		}

		switch (rotation) {
		case Surface.ROTATION_0:
		case Surface.ROTATION_90:
			invert = false;
			break;
		case Surface.ROTATION_180:
		case Surface.ROTATION_270:
			invert = true;
			break;
		}

		if (event.getAction() != KeyEvent.ACTION_DOWN)
			return false;
		else {
			if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
				if (invert)
					pageDown(Orientation.HORIZONTAL);
				else
					pageUp(Orientation.HORIZONTAL);
			if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
				if (invert)
					pageUp(Orientation.HORIZONTAL);
				else
					pageDown(Orientation.HORIZONTAL);
			return true;
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();

		if (isAnimating() && action == KeyEvent.ACTION_DOWN) {
			stopAnimating();
			return true;
		}

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			return handleVolumeButtonEvent(event);

		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (action == KeyEvent.ACTION_DOWN) {
				pageDown(Orientation.HORIZONTAL);
			}

			return true;

		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (action == KeyEvent.ACTION_DOWN) {
				pageUp(Orientation.HORIZONTAL);
			}

			return true;

		case KeyEvent.KEYCODE_BACK:
			if (action == KeyEvent.ACTION_DOWN) {

				if (titleBarLayout.getVisibility() == View.VISIBLE) {
					hideTitleBar();
				} else if (bookView.hasPrevPosition()) {
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
		return anim != null && !anim.isFinished();
	}

	private void startAutoScroll() {

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		this.viewSwitcher.setInAnimation(null);
		this.viewSwitcher.setOutAnimation(null);

		bookView.setKeepScreenOn(true);

		ScrollStyle style = config.getAutoScrollStyle();

		if (style == ScrollStyle.ROLLING_BLIND) {
			prepareRollingBlind();
		} else {
			preparePageTimer();
		}

		viewSwitcher.showNext();

		uiHandler.post(new AutoScrollRunnable());
	}

	private void prepareRollingBlind() {

		Bitmap before = getBookViewSnapshot();

		bookView.pageDown();
		Bitmap after = getBookViewSnapshot();

		RollingBlindAnimator anim = new RollingBlindAnimator();
		anim.setAnimationSpeed(config.getScrollSpeed());

		anim.setBackgroundBitmap(before);
		anim.setForegroundBitmap(after);

		dummyView.setAnimator(anim);
	}

	private void preparePageTimer() {
		bookView.pageDown();
		Bitmap after = getBookViewSnapshot();

		PageTimer timer = new PageTimer();
		timer.setBackgroundImage(after);
		timer.setSpeed(config.getScrollSpeed());

		dummyView.setAnimator(timer);
	}

	private void doPageCurl(boolean flipRight) {

		if (isAnimating()) {
			return;
		}

		this.viewSwitcher.setInAnimation(null);
		this.viewSwitcher.setOutAnimation(null);

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		Bitmap before = getBookViewSnapshot();

		PageCurlAnimator animator = new PageCurlAnimator(flipRight);

		// Pagecurls should only take a few frames. When the screen gets
		// bigger, so do the frames.
		animator.SetCurlSpeed(bookView.getWidth() / 8);

		animator.setBackgroundColor(config.getBackgroundColor());

		LOG.debug("Before size: w=" + before.getWidth() + " h="
				+ before.getHeight());

		if (flipRight) {
			bookView.pageDown();
			Bitmap after = getBookViewSnapshot();
			LOG.debug("After size: w=" + after.getWidth() + " h="
					+ after.getHeight());
			animator.setBackgroundBitmap(after);
			animator.setForegroundBitmap(before);
		} else {
			bookView.pageUp();
			Bitmap after = getBookViewSnapshot();
			LOG.debug("After size: w=" + after.getWidth() + " h="
					+ after.getHeight());
			animator.setBackgroundBitmap(before);
			animator.setForegroundBitmap(after);
		}

		dummyView.setAnimator(animator);

		this.viewSwitcher.showNext();

		uiHandler.post(new PageCurlRunnable(animator));

		dummyView.invalidate();

	}

	private class PageCurlRunnable implements Runnable {

		private PageCurlAnimator animator;

		public PageCurlRunnable(PageCurlAnimator animator) {
			this.animator = animator;
		}

		@Override
		public void run() {

			if (this.animator.isFinished()) {

				if (viewSwitcher.getCurrentView() == dummyView) {
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

			if (dummyView.getAnimator() == null) {
				LOG.debug("BookView no longer has an animator. Aborting rolling blind.");
				stopAnimating();
			} else {

				Animator anim = dummyView.getAnimator();

				if (anim.isFinished()) {
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

		if (dummyView.getAnimator() != null) {
			dummyView.getAnimator().stop();
			this.dummyView.setAnimator(null);
		}

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		bookView.setKeepScreenOn(false);
	}

	private Bitmap getBookViewSnapshot() {

		try {
			Bitmap bitmap = Bitmap.createBitmap(viewSwitcher.getWidth(),
					viewSwitcher.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);

			bookView.layout(0, 0, viewSwitcher.getWidth(),
					viewSwitcher.getHeight());

			bookView.draw(canvas);

			return bitmap;
		} catch (OutOfMemoryError out) {
			viewSwitcher.setBackgroundColor(config.getBackgroundColor());
		}

		return null;
	}

	private void prepareSlide(Animation inAnim, Animation outAnim) {

		Bitmap bitmap = getBookViewSnapshot();
		dummyView.setImageBitmap(bitmap);

		viewSwitcher.layout(0, 0, viewSwitcher.getWidth(),
				viewSwitcher.getHeight());
		dummyView.layout(0, 0, viewSwitcher.getWidth(),
				viewSwitcher.getHeight());

		this.viewSwitcher.showNext();

		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);
	}

	private void pageDown(Orientation o) {

		if (bookView.isAtEnd()) {
			return;
		}

		stopAnimating();

		if (o == Orientation.HORIZONTAL) {

			AnimationStyle animH = config.getHorizontalAnim();

			if (animH == AnimationStyle.CURL) {
				doPageCurl(true);
			} else if (animH == AnimationStyle.SLIDE) {
				prepareSlide(Animations.inFromRightAnimation(),
						Animations.outToLeftAnimation());
				this.viewSwitcher.showNext();
				bookView.pageDown();
			} else {
				bookView.pageDown();
			}

		} else {
			if (config.getVerticalAnim() == AnimationStyle.SLIDE) {
				prepareSlide(Animations.inFromBottomAnimation(),
						Animations.outToTopAnimation());
				this.viewSwitcher.showNext();
			}

			bookView.pageDown();
		}

	}

	private void pageUp(Orientation o) {

		if (bookView.isAtStart()) {
			return;
		}

		stopAnimating();

		if (o == Orientation.HORIZONTAL) {

			AnimationStyle animH = config.getHorizontalAnim();

			if (animH == AnimationStyle.CURL) {
				doPageCurl(false);
			} else if (animH == AnimationStyle.SLIDE) {
				prepareSlide(Animations.inFromLeftAnimation(),
						Animations.outToRightAnimation());
				this.viewSwitcher.showNext();
				bookView.pageUp();
			} else {
				bookView.pageUp();
			}

		} else {

			if (config.getVerticalAnim() == AnimationStyle.SLIDE) {
				prepareSlide(Animations.inFromTopAnimation(),
						Animations.outToBottomAnimation());
				this.viewSwitcher.showNext();
			}

			bookView.pageUp();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (this.tocDialog == null) {
			initTocDialog();
		}

		MenuItem nightMode = menu.findItem(R.id.profile_night);
		MenuItem dayMode = menu.findItem(R.id.profile_day);

		MenuItem showToc = menu.findItem(R.id.show_toc);

		showToc.setEnabled(this.tocDialog != null);

		getSupportActionBar().show();

		if (config.getColourProfile() == ColourProfile.DAY) {
			dayMode.setVisible(false);
			nightMode.setVisible(true);
		} else {
			dayMode.setVisible(true);
			nightMode.setVisible(false);
		}

		// Only show open file item if we have a file manager installed
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("file/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		if (!isIntentAvailable(this, intent)) {
			menu.findItem(R.id.open_file).setVisible(false);
		}

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		return super.onPrepareOptionsMenu(menu);
	}

	private void hideTitleBar() {
		titleBarLayout.setVisibility(View.GONE);
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
					loadNewBook(filePath);
				}
			}
		}
	}

	private void loadNewBook(String fileName) {
		setTitle(R.string.app_name);
		this.tocDialog = null;
		this.bookTitle = null;
		this.titleBase = null;

		bookView.clear();

		updateFileName(null, fileName);
		new DownloadProgressTask().execute();
	}

	@Override
	protected void onStop() {
		super.onStop();

		saveReadingPosition();
		sendProgressUpdateToServer();
		this.waitDialog.dismiss();
	}
	
	private void saveReadingPosition() {
		if (this.bookView != null) {

			config.setLastPosition(this.fileName, this.bookView.getPosition());
			config.setLastIndex(this.fileName, this.bookView.getIndex());
			
			sendProgressUpdateToServer();
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.reading_menu, menu);

		return true;
	}

	@Override
	public void onOptionsMenuClosed(android.view.Menu menu) {
		updateFromPrefs();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		hideTitleBar();

		// Handle item selection
		switch (item.getItemId()) {

		case R.id.profile_night:
			config.setColourProfile(ColourProfile.NIGHT);
			this.restartActivity();
			return true;

		case R.id.profile_day:
			config.setColourProfile(ColourProfile.DAY);
			this.restartActivity();
			return true;

		case R.id.manual_sync:
			if (config.isSyncEnabled()) {
				new ManualProgressSync().execute();
			} else {
				Toast.makeText(this, R.string.enter_email, Toast.LENGTH_LONG)
						.show();
			}
			return true;

		case R.id.search_text:
			onSearchClick();
			return true;

		case R.id.preferences:
			// Cache old settings to check if we'll need a restart later
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

		if (config.isVerticalSwipeEnabled()) {
			pageDown(Orientation.VERTICAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onSwipeUp() {

		if (config.isVerticalSwipeEnabled()) {
			pageUp(Orientation.VERTICAL);
			return true;
		}

		return false;
	}

	@Override
	public void onScreenTap() {

		stopAnimating();

		if (this.titleBarLayout.getVisibility() == View.VISIBLE) {
			titleBarLayout.setVisibility(View.GONE);

			updateFromPrefs();
		} else {
			titleBarLayout.setVisibility(View.VISIBLE);

			getSupportActionBar().show();
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	@Override
	public boolean onSwipeLeft() {

		if (config.isHorizontalSwipeEnabled()) {
			pageDown(Orientation.HORIZONTAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onSwipeRight() {

		if (config.isHorizontalSwipeEnabled()) {
			pageUp(Orientation.HORIZONTAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onTapLeftEdge() {
		if (config.isHorizontalTappingEnabled()) {
			pageUp(Orientation.HORIZONTAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onTapRightEdge() {
		if (config.isHorizontalTappingEnabled()) {
			pageDown(Orientation.HORIZONTAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onTapTopEdge() {
		if (config.isVerticalTappingEnabled()) {
			pageUp(Orientation.VERTICAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onTopBottomEdge() {
		if (config.isVerticalTappingEnabled()) {
			pageDown(Orientation.VERTICAL);
			return true;
		}

		return false;
	}

	@Override
	public boolean onLeftEdgeSlide(int value) {

		if (config.isBrightnessControlEnabled() && value != 0) {
			int baseBrightness = config.getBrightNess();

			int brightnessLevel = Math.min(99, value + baseBrightness);
			brightnessLevel = Math.max(1, brightnessLevel);

			final int level = brightnessLevel;

			String brightness = getString(R.string.brightness);
			setScreenBrightnessLevel(brightnessLevel);

			if (brightnessToast == null) {
				brightnessToast = Toast
						.makeText(ReadingActivity.this, brightness + ": "
								+ brightnessLevel, Toast.LENGTH_SHORT);
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
		
		this.bookView.releaseResources();
		saveReadingPosition();
		
		finish();		
	}

	private void showPickProgressDialog(final List<BookProgress> results) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.cloud_bm));

		ProgressListAdapter adapter = new ProgressListAdapter(this, bookView,
				results, config);
		builder.setAdapter(adapter, adapter);

		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.show();
	}

	private void initTocDialog() {

		if (this.tocDialog != null) {
			return;
		}

		final List<BookView.TocEntry> tocList = this.bookView
				.getTableOfContents();

		if (tocList == null || tocList.isEmpty()) {
			return;
		}

		final CharSequence[] items = new CharSequence[tocList.size()];

		for (int i = 0; i < items.length; i++) {
			items[i] = tocList.get(i).getTitle();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.toc_label);

		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				bookView.navigateTo(tocList.get(item).getHref());
			}
		});

		this.tocDialog = builder.create();
		this.tocDialog.setOwnerActivity(this);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (this.bookView != null) {

			outState.putInt(POS_KEY, this.bookView.getPosition());
			outState.putInt(IDX_KEY, this.bookView.getIndex());

			sendProgressUpdateToServer();

			libraryService.close();			
		}

	}

	private void sendProgressUpdateToServer() {

		libraryService.updateReadingProgress(fileName, progressPercentage);

		backgroundHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					progressService.storeProgress(fileName,
							bookView.getIndex(), bookView.getPosition(),
							progressPercentage);
				} catch (AccessException a) {
				}
			}
		});
	}

	private void onSearchClick() {

		final ProgressDialog searchProgress = new ProgressDialog(this);
		searchProgress.setOwnerActivity(this);
		searchProgress.setCancelable(true);
		searchProgress.setMax(100);
		searchProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.search_text);
		alert.setMessage(R.string.enter_query);

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		final SearchTextTask task = new SearchTextTask(bookView.getBook()) {

			int i = 0;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				// Hide on-screen keyboard if it is showing

				InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

			}

			@Override
			protected void onProgressUpdate(SearchResult... values) {

				super.onProgressUpdate(values);
				LOG.debug("Found match at index=" + values[0].getIndex()
						+ ", offset=" + values[0].getStart() + " with context "
						+ values[0].getDisplay());
				SearchResult res = values[0];

				if (res.getDisplay() != null) {
					i++;
					String update = String.format(
							getString(R.string.search_hits), i);
					searchProgress.setTitle(update);
				}

				searchProgress.setProgress(res.getPercentage());
			}

			@Override
			protected void onCancelled() {
				Toast.makeText(ReadingActivity.this, R.string.search_cancelled,
						Toast.LENGTH_LONG).show();
			}

			protected void onPostExecute(java.util.List<SearchResult> result) {

				searchProgress.dismiss();

				if (!isCancelled()) {
					if (result.size() > 0) {
						showSearchResultDialog(result);
					} else {
						Toast.makeText(ReadingActivity.this,
								R.string.search_no_matches, Toast.LENGTH_LONG)
								.show();
					}
				}
			};
		};

		searchProgress
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						task.cancel(true);
					}
				});

		alert.setPositiveButton(android.R.string.search_go,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						CharSequence value = input.getText();

						searchProgress.setTitle(R.string.search_wait);
						searchProgress.show();
						task.execute(value.toString());
					}
				});

		alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();
	}

	private void showSearchResultDialog(
			final List<SearchTextTask.SearchResult> results) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.search_results);

		SearchResultAdapter adapter = new SearchResultAdapter(this, bookView,
				results, config);
		builder.setAdapter(adapter, adapter);

		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.show();
	}

	private class ManualProgressSync extends
			AsyncTask<Void, Integer, List<BookProgress>> {

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

			if (progress == null) {

				AlertDialog.Builder alertDialog = new AlertDialog.Builder(
						ReadingActivity.this);

				alertDialog.setTitle(R.string.sync_failed);

				if (accessDenied) {
					alertDialog.setMessage(R.string.access_denied);
				} else {
					alertDialog.setMessage(R.string.connection_fail);
				}

				alertDialog.setNeutralButton(android.R.string.ok,
						new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});

				alertDialog.show();

			} else {
				showPickProgressDialog(progress);
			}
		}
	}

	private class DownloadProgressTask extends
			AsyncTask<Void, Integer, BookProgress> {

		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.syncing);
			waitDialog.show();
		}

		@Override
		protected BookProgress doInBackground(Void... params) {
			try {
				List<BookProgress> updates = progressService
						.getProgress(fileName);

				if (updates != null && updates.size() > 0) {
					return updates.get(0);
				}
			} catch (AccessException e) {
			}

			return null;
		}

		@Override
		protected void onPostExecute(BookProgress progress) {
			waitDialog.hide();

			int index = bookView.getIndex();
			int pos = bookView.getPosition();

			if (progress != null) {

				if (progress.getIndex() > index) {
					bookView.setIndex(progress.getIndex());
					bookView.setPosition(progress.getProgress());
				} else if (progress.getIndex() == index) {
					pos = Math.max(pos, progress.getProgress());
					bookView.setPosition(pos);
				}

			}

			bookView.restore();
		}
	}
}

class ScreenReceiver extends BroadcastReceiver {

	public static boolean wasScreenOn = true;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			// do whatever you need to do here
			wasScreenOn = false;
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			// and do whatever you need to do here
			wasScreenOn = true;
		}
	}

}
