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

import android.annotation.TargetApi;
import android.app.*;
import android.support.v4.app.FragmentTransaction;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import com.actionbarsherlock.view.ActionMode;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;
import net.nightwhistler.htmlspanner.spans.CenterSpan;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.AnimationStyle;
import net.nightwhistler.pageturner.Configuration.ColourProfile;
import net.nightwhistler.pageturner.Configuration.ReadingDirection;
import net.nightwhistler.pageturner.Configuration.ScrollStyle;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.TextUtil;
import net.nightwhistler.pageturner.animation.*;
import net.nightwhistler.pageturner.bookmark.Bookmark;
import net.nightwhistler.pageturner.bookmark.BookmarkDatabaseHelper;
import net.nightwhistler.pageturner.dto.HighLight;
import net.nightwhistler.pageturner.dto.SearchResult;
import net.nightwhistler.pageturner.dto.TocEntry;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.sync.AccessException;
import net.nightwhistler.pageturner.sync.BookProgress;
import net.nightwhistler.pageturner.sync.ProgressService;
import net.nightwhistler.pageturner.tasks.SearchTextTask;
import net.nightwhistler.pageturner.tts.SpeechCompletedCallback;
import net.nightwhistler.pageturner.tts.TTSPlaybackItem;
import net.nightwhistler.pageturner.tts.TTSPlaybackQueue;
import net.nightwhistler.pageturner.view.*;
import net.nightwhistler.pageturner.view.bookview.BookView;
import net.nightwhistler.pageturner.view.bookview.BookViewListener;
import net.nightwhistler.pageturner.view.bookview.TextLoader;
import net.nightwhistler.pageturner.view.bookview.TextSelectionCallback;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.inject.InjectView;
import yuku.ambilwarna.AmbilWarnaDialog;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import static net.nightwhistler.pageturner.PlatformUtil.isIntentAvailable;

public class ReadingFragment extends RoboSherlockFragment implements
		BookViewListener, TextSelectionCallback, OnUtteranceCompletedListener,
        SpeechCompletedCallback, DialogFactory.SearchCallBack {

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

    private static final int BOOK_OPEN_STAGES = 5;

	private static final Logger LOG = LoggerFactory
			.getLogger("ReadingFragment");

	@Inject
	private ProgressService progressService;

	@Inject
	private LibraryService libraryService;

	@Inject
	private Configuration config;

    @Inject
    private DialogFactory dialogFactory;

    @Inject
    private NotificationManager notificationManager;

    @Inject
    private Context context;

	@InjectView(R.id.mainContainer)
	private ViewSwitcher viewSwitcher;

	@InjectView(R.id.bookView)
	private BookView bookView;

	@InjectView(R.id.myTitleBarTextView)
	private TextView titleBar;

	@InjectView(R.id.myTitleBarLayout)
	private RelativeLayout titleBarLayout;
		
	@InjectView(R.id.mediaPlayerLayout)
	private LinearLayout mediaLayout;

	@InjectView(R.id.titleProgress)
	private SeekBar progressBar;

	@InjectView(R.id.percentageField)
	private TextView percentageField;

	@InjectView(R.id.authorField)
	private TextView authorField;

	@InjectView(R.id.dummyView)
	private AnimatedImageView dummyView;
	
	@InjectView(R.id.mediaProgress)
	private SeekBar mediaProgressBar;

	@InjectView(R.id.pageNumberView)
	private TextView pageNumberView;
	
	@InjectView(R.id.playPauseButton)
	private ImageButton playPauseButton;
	
	@InjectView(R.id.stopButton)
	private ImageButton stopButton;

    @InjectView(R.id.nextButton)
    private ImageButton nextButton;

    @InjectView(R.id.prevButton)
    private ImageButton prevButton;

    @InjectView(R.id.wordView)
    private TextView wordView;
	
	@Inject
	private TelephonyManager telephonyManager;

	@Inject
	private PowerManager powerManager;
	
	@Inject
	private AudioManager audioManager;

    @Inject
    private TTSPlaybackQueue ttsPlaybackItemQueue;

    @Inject
    private TextLoader textLoader;

    @Inject
    private HighlightManager highlightManager;

    @Inject
    private BookmarkDatabaseHelper bookmarkDatabaseHelper;

    private MenuItem searchMenuItem;

    private Map<String, TTSPlaybackItem> ttsItemPrep = new HashMap<String, TTSPlaybackItem>();
    private List<SearchResult> searchResults = new ArrayList<SearchResult>();

	private ProgressDialog waitDialog;

    private TextToSpeech textToSpeech;
	
	private boolean ttsAvailable = false;
		
	private String bookTitle;
	private String titleBase;

	private String fileName;
	private int progressPercentage;

    private String language = "en";
	
	private int currentPageNumber = -1;	
	
	private static enum Orientation {
		HORIZONTAL, VERTICAL
	}
	
	private static class SavedConfigState {
		private boolean brightness;
		private boolean stripWhiteSpace;
		private String fontName;
		private String serifFontName;
		private String sansSerifFontName;
		private boolean usePageNum;
		private boolean fullscreen;
		private int vMargin;
		private int hMargin;
		private int textSize;
		private boolean scrolling;
        private boolean allowStyling;
        private boolean allowColoursFromCSS;
	}

	private SavedConfigState savedConfigState = new SavedConfigState();
	private BookView.SelectedWord selectedWord = null;

	private Handler uiHandler;
	private Handler backgroundHandler;

	private Toast brightnessToast;

	private PageTurnerMediaReceiver mediaReceiver;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Restore preferences
		this.uiHandler = new Handler();
		HandlerThread bgThread = new HandlerThread("background");
		bgThread.start();
		this.backgroundHandler = new Handler(bgThread.getLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&  config.isFullScreenEnabled() ) {
            return inflater.inflate(R.layout.fragment_reading_fs, container, false);
        } else {
		    return inflater.inflate(R.layout.fragment_reading, container, false);
        }
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasOptionsMenu(true);
		this.bookView.init();

		this.progressBar.setFocusable(true);
		this.progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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

        this.mediaProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar,
                                          int progress, boolean fromUser) {
                if (fromUser) {
                    seekToPointInPlayback(progress);
                }
            }
        });


		this.textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				onTextToSpeechInit(status);				
			}
		});


		this.bookView.addListener(this);
		this.bookView.setTextSelectionCallback(this);
	}
	
	private void seekToPointInPlayback(int position) {

        TTSPlaybackItem item = this.ttsPlaybackItemQueue.peek();

		if ( item != null ) {
			item.getMediaPlayer().seekTo(position);
		}
	}
	
	public void onMediaButtonEvent( int buttonId ) {		
		
		if ( buttonId == R.id.playPauseButton &&
				! ttsIsRunning() ) {
			startTextToSpeech();
			return;
		}

        TTSPlaybackItem item = this.ttsPlaybackItemQueue.peek();
		
		if ( item == null ) {
			stopTextToSpeech(false);
			return;
		}

        MediaPlayer mediaPlayer = item.getMediaPlayer();
        uiHandler.removeCallbacks(progressBarUpdater);

        switch ( buttonId ) {
            case R.id.stopButton:
                stopTextToSpeech(true);
                return;
            case R.id.nextButton:
                performSkip(true);
                uiHandler.post(progressBarUpdater);
                return;
            case R.id.prevButton:
                performSkip(false);
                uiHandler.post(progressBarUpdater);
                return;

            case R.id.playPauseButton:
                if ( mediaPlayer.isPlaying() ) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                    uiHandler.post(progressBarUpdater);
                }
                return;
        }
	}

    private void performSkip( boolean toEnd ) {

        if ( ! ttsIsRunning() ) {
            return;
        }

        TTSPlaybackItem item = this.ttsPlaybackItemQueue.peek();

        if ( item != null ) {
            MediaPlayer player = item.getMediaPlayer();

            if ( toEnd ) {
                player.seekTo( player.getDuration() );
            } else {
                player.seekTo(0);
            }

        }

    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		DisplayMetrics metrics = new DisplayMetrics();
        SherlockFragmentActivity activity = getSherlockActivity();

		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		displayPageNumber(-1); // Initializes the pagenumber view properly

		final GestureDetector gestureDetector = new GestureDetector(context,
				new NavGestureDetector(bookView, this, metrics));

		View.OnTouchListener gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				
				if ( ttsIsRunning() ) {
					return false;
				}
				
				return gestureDetector.onTouchEvent(event);
			}
		};
		
		this.viewSwitcher.setOnTouchListener(gestureListener);
		this.bookView.setOnTouchListener(gestureListener);
        this.dummyView.setOnTouchListener(gestureListener);
		
		registerForContextMenu(bookView);
		saveConfigState();

        Intent intent = activity.getIntent();
		String file = null;

		if ( intent.getData() != null) {
			file = intent.getData().getPath();
		}

		if (file == null) {
			file = config.getLastOpenedFile();
		}

		updateFromPrefs();
		updateFileName(savedInstanceState, file);
		
		if ("".equals(fileName) || ! new File(fileName).exists() ) {

            LOG.info( "Requested to open file " + fileName + ", which doesn't seem to exist. " +
                    "Switching back to the library.");

			Intent newIntent = new Intent(context, LibraryActivity.class);
			startActivity(newIntent);
			activity.finish();
			return;

		} else {

			if (savedInstanceState == null && config.isSyncEnabled()) {
				new DownloadProgressTask().execute();
			} else {
				bookView.restore();
			}
		}

        if ( ttsIsRunning() ) {
            this.mediaLayout.setVisibility(View.VISIBLE);
            this.ttsPlaybackItemQueue.updateSpeechCompletedCallbacks(this);
            uiHandler.post( progressBarUpdater );
        }

        activity.getSupportActionBar().addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {

                LOG.debug("Detected change of visibility in action-bar: visible=" + isVisible );

                if (isVisible) {
                    titleBarLayout.setVisibility(View.VISIBLE);
                } else {
                    titleBarLayout.setVisibility(View.GONE);
                }
            }
        });


        /*
        new ShakeListener(getActivity()).setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                if ( ! ttsIsRunning() ) {
                    startTextToSpeech();
                }
            }
        });
        */
	}

	private void saveConfigState() {
		// Cache old settings to check if we'll need a restart later
		savedConfigState.brightness = config.isBrightnessControlEnabled();
		savedConfigState.stripWhiteSpace = config.isStripWhiteSpaceEnabled();
	
		savedConfigState.usePageNum = config.isShowPageNumbers();
		savedConfigState.fullscreen = config.isFullScreenEnabled();
		
		savedConfigState.hMargin = config.getHorizontalMargin();
		savedConfigState.vMargin = config.getVerticalMargin();
		
		savedConfigState.textSize = config.getTextSize();
		savedConfigState.fontName = config.getDefaultFontFamily().getName();
		savedConfigState.serifFontName = config.getSerifFontFamily().getName();
		savedConfigState.sansSerifFontName = config.getSansSerifFontFamily().getName();
		
		savedConfigState.scrolling = config.isScrollingEnabled();
        savedConfigState.allowStyling = config.isAllowStyling();
        savedConfigState.allowColoursFromCSS = config.isUseColoursFromCSS();
		
	}

	/*
	 * @see roboguice.activity.RoboActivity#onPause()
	 */
	@Override
	public void onPause() {
        LOG.debug("onPause() called.");
			
		saveReadingPosition();
		super.onPause();
	}
	
	private void printScreenAndCallState(String calledFrom) {
	    boolean isScreenOn = powerManager.isScreenOn();

	    if (!isScreenOn) {
	    	LOG.debug(calledFrom + ": Screen is off");
	    } else {
	    	LOG.debug(calledFrom + ": Screen is on");
	    }
	    
	    int phoneState = telephonyManager.getCallState();
	    
	    if ( phoneState == TelephonyManager.CALL_STATE_RINGING || phoneState == TelephonyManager.CALL_STATE_OFFHOOK ) {
	    	LOG.debug(calledFrom + ": Detected call activity");
	    } else {
	    	LOG.debug(calledFrom + ": No active call.");
	    }
	}

    private void playBeep( boolean error ) {

        if ( ! isAdded() ) {
            return;
        }

        try {
            MediaPlayer beepPlayer = new MediaPlayer();

            String file = "beep.mp3";

            if ( error ) {
                file = "error.mp3";
            }

            AssetFileDescriptor descriptor = context.getAssets().openFd(file);
            beepPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            beepPlayer.prepare();

            beepPlayer.start();
        } catch (Exception io) {
            //We'll manage without the beep :)
        }
    }
	
	private void startTextToSpeech() {

        if ( audioManager.isMusicActive() ) {
            return;
        }

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ) {
			subscribeToMediaButtons();
		}

        playBeep(false);

		File fos = new File( config.getTTSFolder() );

        if ( fos.exists() && ! fos.isDirectory() ) {
            fos.delete();
        }

        fos.mkdirs();

        if ( ! (fos.exists() && fos.isDirectory() )  ) {
            LOG.error("Failed to create folder " + fos.getAbsolutePath() );
            showTTSFailed("Failed to create folder " + fos.getAbsolutePath() );
            return;
        }

        saveReadingPosition();
		//Delete any old TTS files still present.
		for ( File f: fos.listFiles() ) {
			f.delete();
		}

        ttsItemPrep.clear();

        if (! ttsAvailable ) {
            return;
        }

        this.wordView.setTextColor( config.getTextColor() );
        this.wordView.setBackgroundColor( config.getBackgroundColor() );

        this.ttsPlaybackItemQueue.activate();
		this.mediaLayout.setVisibility(View.VISIBLE);
		
		this.getWaitDialog().setMessage(getString(R.string.init_tts));
		this.getWaitDialog().show();

        streamTTSToDisk();
	}

    private void streamTTSToDisk() {
        new Thread( new StreamToDiskRunnable() ).start();
    }

    private class StreamToDiskRunnable implements Runnable {
        @Override
        public void run() {

            CharSequence text = bookView.getStrategy().getText();

            if ( text == null || ! ttsIsRunning()) {
                return;
            }

            File ttsFolder = new File( config.getTTSFolder() );
            String textToSpeak = text.toString().substring( bookView.getStartOfCurrentPage() );

            textToSpeak = TextUtil.splitOnPunctuation(textToSpeak);
            String[] parts = textToSpeak.split("\n");

            int offset = bookView.getStartOfCurrentPage();

            try {
                for ( int i=0; i < parts.length && ttsIsRunning(); i++ ) {

                    LOG.debug("Streaming part " + i + " to disk." );

                    String part = parts[i];

                    boolean lastPart = i == parts.length -1;

                    //Utterance ID doubles as the filename
                    String pageName = new File( ttsFolder, "tts_" + UUID.randomUUID() + ".wav").getAbsolutePath();
                    streamPartToDisk(pageName, part, offset, textToSpeak.length(), lastPart);

                    offset += part.length() +1;

                    Thread.yield();
                }
            } catch (TTSFailedException e) {
                //Just stop streaming
            }
        }
    }

    private void showTTSFailed(final String message) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                stopTextToSpeech(true);
                closeWaitDialog();

                if ( isAdded() ) {
                    playBeep(true);

                    StringBuilder textBuilder = new StringBuilder( getString(R.string.tts_failed) );
                    textBuilder.append("\n").append(message);

                    Toast.makeText(context, textBuilder.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        } );
    }

    /** Checked exception to indicate TTS failure **/
    private static class TTSFailedException extends Exception {}

    private void streamPartToDisk(String fileName, String part, int offset, int totalLength, boolean endOfPage )
        throws TTSFailedException {

        LOG.debug("Request to stream text to file " + fileName + " with text " + part );

        if ( part.trim().length() > 0 || endOfPage ) {

            HashMap<String, String> params = new HashMap<String, String>();

            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, fileName);

            TTSPlaybackItem item = new TTSPlaybackItem( part, new MediaPlayer(), totalLength, offset, endOfPage, fileName);
            ttsItemPrep.put(fileName, item);

            int result = textToSpeech.synthesizeToFile(part, params, fileName);
            if ( result != TextToSpeech.SUCCESS ) {
                String message = "synthesizeToFile failed with result " + result;
                LOG.error(message);
                showTTSFailed(message);
                throw new TTSFailedException();
            }
        } else {
            LOG.debug("Skipping part, since it's empty.");
        }
    }
	
	@Override
	public void onUtteranceCompleted(final String wavFile) {
		
        LOG.debug("TTS streaming completed for " + wavFile );

		if ( ! ttsIsRunning() ) {
            this.textToSpeech.stop();
			return;
		}

        if ( ! ttsItemPrep.containsKey(wavFile) ) {
            LOG.error("Got onUtteranceCompleted for " + wavFile + " but there is no corresponding TTSPlaybackItem!");
            return;
        }

        final TTSPlaybackItem item = ttsItemPrep.remove(wavFile);

		try {

			MediaPlayer mediaPlayer = item.getMediaPlayer();
			mediaPlayer.reset();
			mediaPlayer.setDataSource(wavFile);
			mediaPlayer.prepare();
			
			this.ttsPlaybackItemQueue.add(item);
			
		} catch (Exception e) {
			LOG.error("Could not play", e);
            showTTSFailed(e.getLocalizedMessage());
            return;
		} 
		
		this.uiHandler.post(new Runnable() {
			
			@Override
			public void run() {					
				closeWaitDialog();
			}
		});		
		
		//If the queue is size 1, it only contains the player we just added,
		//meaning this is a first playback start.
		if ( ttsPlaybackItemQueue.size() == 1 ) {
			startPlayback();
		}
	}	
	
	private Runnable progressBarUpdater = new Runnable() {

        private boolean pausedBecauseOfCall = false;

		public void run() {

            if ( ! ttsIsRunning() ) {
                return;
            }

            long delay = 1000;

            synchronized ( ttsPlaybackItemQueue ) {

                TTSPlaybackItem item = ttsPlaybackItemQueue.peek();

                if ( item != null ) {

                    MediaPlayer mediaPlayer = item.getMediaPlayer();

                    int phoneState = telephonyManager.getCallState();

                    if ( mediaPlayer != null && mediaPlayer.isPlaying() ) {

                        if ( phoneState == TelephonyManager.CALL_STATE_RINGING ||
                                phoneState == TelephonyManager.CALL_STATE_OFFHOOK ) {

                            LOG.debug("Detected call, pausing TTS.");

                            mediaPlayer.pause();
                            this.pausedBecauseOfCall = true;
                        } else {

                            double percentage = (double) mediaPlayer.getCurrentPosition() / (double) mediaPlayer.getDuration();

                            mediaProgressBar.setMax(mediaPlayer.getDuration());
                            mediaProgressBar.setProgress(mediaPlayer.getCurrentPosition());

                            int currentDuration = item.getOffset() + (int) (percentage * item.getText().length());

                            bookView.navigateTo(bookView.getIndex(), currentDuration );

                            wordView.setText( item.getText() );

                            delay = 100;

                        }

                    } else if ( mediaPlayer != null && phoneState == TelephonyManager.CALL_STATE_IDLE
                            && pausedBecauseOfCall ) {
                        LOG.debug("Call over, resuming TTS.");

                        //We reset to the start of the current section before resuming playback.
                        mediaPlayer.seekTo(0);

                        mediaPlayer.start();
                        pausedBecauseOfCall = false;
                        delay = 100;
                    }
                }
            }
			
            // Running this thread after 100 milliseconds
            uiHandler.postDelayed(this, delay);

		}
	};
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	private void subscribeToMediaButtons() {
		if ( this.mediaReceiver == null ) {
			this.mediaReceiver = new PageTurnerMediaReceiver();
			IntentFilter filter = new IntentFilter(MediaButtonReceiver.INTENT_PAGETURNER_MEDIA);
			context.registerReceiver(mediaReceiver, filter);

			audioManager.registerMediaButtonEventReceiver(
					new ComponentName(context, MediaButtonReceiver.class));
		}
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	private void unsubscribeFromMediaButtons() {		
		if ( this.mediaReceiver != null  ) {
			context.unregisterReceiver(mediaReceiver);
			this.mediaReceiver = null;
		
			audioManager.unregisterMediaButtonEventReceiver(
                    new ComponentName(context, MediaButtonReceiver.class));
		}
	}
	
	
	private boolean ttsIsRunning() {
		return ttsPlaybackItemQueue.isActive();
	}
	
	public void speechCompleted( TTSPlaybackItem item, MediaPlayer mediaPlayer ) {
		
        LOG.debug("Speech completed for " + item.getFileName() );       

		if (! ttsPlaybackItemQueue.isEmpty() ) {
			this.ttsPlaybackItemQueue.remove();
		}

		if ( ttsIsRunning()  ) {

			startPlayback();

            if ( item.isLastElementOfPage() ) {
                this.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        pageDown(Orientation.VERTICAL);
                    }
                });
            }
		}		

		mediaPlayer.release();		
		new File(item.getFileName()).delete();
	}
	
	private void startPlayback() {

        LOG.debug("startPlayback() - doing peek()");

        final TTSPlaybackItem item = this.ttsPlaybackItemQueue.peek();

        if ( item == null ) {
            LOG.debug("Got null item, bailing out.");
			return;
		}

        LOG.debug("Start playback for item " + item.getFileName());
        LOG.debug("Text: '" + item.getText() + "'");
		
		if ( item.getMediaPlayer().isPlaying() ) {
			return;
		}

        item.setOnSpeechCompletedCallback(this);
        uiHandler.post(progressBarUpdater);
		item.getMediaPlayer().start();

	}
	
	private void stopTextToSpeech(boolean unsubscribeMediaButtons) {

        this.ttsPlaybackItemQueue.deactivate();

		this.mediaLayout.setVisibility(View.GONE);
		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && unsubscribeMediaButtons ) {
			unsubscribeFromMediaButtons();
		}

        this.textToSpeech.stop();

        this.ttsItemPrep.clear();

        saveReadingPosition();
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.textToSpeech.shutdown();
        this.closeWaitDialog();
    }

    @SuppressWarnings("deprecation")
	public void onTextToSpeechInit(int status) {					

		this.ttsAvailable = (status == TextToSpeech.SUCCESS) && !Configuration.IS_NOOK_TOUCH;

        if ( this.ttsAvailable ) {
            this.textToSpeech.setOnUtteranceCompletedListener(this);
        }
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

	@Override
	public void progressUpdate(int progressPercentage, int pageNumber,
			int totalPages) {

        if ( ! isAdded() || getActivity() == null ) {
            return;
        }

		this.currentPageNumber = pageNumber;
		
		// Work-around for calculation errors and weird values.
		if (progressPercentage < 0 || progressPercentage > 100) {
			return;
		}

		this.progressPercentage = progressPercentage;

		if (config.isShowPageNumbers() && pageNumber > 0) {
			percentageField.setText("" + progressPercentage + "%  "
					+ pageNumber + " / " + totalPages);
			displayPageNumber(pageNumber);			

		} else {
			percentageField.setText("" + progressPercentage + "%");			
		}

		this.progressBar.setProgress(progressPercentage);
		this.progressBar.setMax(100);
	}

	private void displayPageNumber(int pageNumber) {

		String pageString;

		if ( !config.isScrollingEnabled() && pageNumber > 0) {
			pageString = Integer.toString(pageNumber) + "\n";
		} else {
			pageString = "\n";
		}

		SpannableStringBuilder builder = new SpannableStringBuilder(pageString);
		builder.setSpan(new CenterSpan(), 0, builder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		pageNumberView.setTextColor(config.getTextColor());
		pageNumberView.setTextSize(config.getTextSize());

		pageNumberView.setTypeface(config.getDefaultFontFamily().getDefaultTypeface());

		pageNumberView.setText(builder);
        pageNumberView.invalidate();
	}

	private void updateFromPrefs() {

        SherlockFragmentActivity activity = getSherlockActivity();

        if ( activity == null ) {
            return;
        }

		bookView.setTextSize(config.getTextSize());

		int marginH = config.getHorizontalMargin();
		int marginV = config.getVerticalMargin();

        this.textLoader.setFontFamily(config.getDefaultFontFamily());
		this.bookView.setFontFamily(config.getDefaultFontFamily());
		this.textLoader.setSansSerifFontFamily(config.getSansSerifFontFamily());
		this.textLoader.setSerifFontFamily(config.getSerifFontFamily());

		bookView.setHorizontalMargin(marginH);
		bookView.setVerticalMargin(marginV);

		if (!isAnimating()) {
			bookView.setEnableScrolling(config.isScrollingEnabled());
		}

		textLoader.setStripWhiteSpace(config.isStripWhiteSpaceEnabled());
        textLoader.setAllowStyling( config.isAllowStyling() );
        textLoader.setUseColoursFromCSS( config.isUseColoursFromCSS() );

		bookView.setLineSpacing(config.getLineSpacing());

		if (config.isFullScreenEnabled()) {
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			activity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			activity.getSupportActionBar().hide();
		} else {
			activity.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			activity.getSupportActionBar().show();
		}

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && config.isDimSystemUI() ) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

		if (config.isKeepScreenOn()) {
			activity.getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		restoreColorProfile();

		// Check if we need a restart
		if (config.isFullScreenEnabled() != savedConfigState.fullscreen
				|| config.isShowPageNumbers() != savedConfigState.usePageNum
				|| config.isBrightnessControlEnabled() != savedConfigState.brightness
				|| config.isStripWhiteSpaceEnabled() != savedConfigState.stripWhiteSpace
				|| !config.getDefaultFontFamily().getName().equalsIgnoreCase(savedConfigState.fontName)
				|| !config.getSerifFontFamily().getName().equalsIgnoreCase(savedConfigState.serifFontName)
				|| !config.getSansSerifFontFamily().getName().equalsIgnoreCase(savedConfigState.sansSerifFontName)
				|| config.getHorizontalMargin() != savedConfigState.hMargin
				|| config.getVerticalMargin() != savedConfigState.vMargin
				|| config.getTextSize() != savedConfigState.textSize 
				|| config.isScrollingEnabled() != savedConfigState.scrolling
                || config.isAllowStyling() != savedConfigState.allowStyling
                || config.isUseColoursFromCSS() != savedConfigState.allowColoursFromCSS ) {

            textLoader.invalidateCachedText();
			restartActivity();
		}

		Configuration.OrientationLock orientation = config
				.getScreenOrientation();

		switch (orientation) {
		case PORTRAIT:
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case LANDSCAPE:
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case REVERSE_LANDSCAPE:
			getActivity().setRequestedOrientation(8); // Android 2.3+ value
			break;
		case REVERSE_PORTRAIT:
			getActivity().setRequestedOrientation(9); // Android 2.3+ value
			break;
		default:
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
	}

    @Override
    public void onLowMemory() {
        this.textLoader.clearCachedText();
    }

    private void restartActivity() {

		onStop();

        //Clear any cached text.
        textLoader.closeCurrentBook();
		Intent intent = new Intent(context, ReadingActivity.class);
		intent.setData(Uri.parse(this.fileName));
		startActivity(intent);
		this.libraryService.close();

        Activity activity = getActivity();

        if ( activity != null ) {
            activity.finish();
        }
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
            hideTitleBar();
			updateFromPrefs();
		} else {
			getActivity().getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	public boolean onTouchEvent(MotionEvent event) {	
		return bookView.onTouchEvent(event);
	}

	@Override
	public void bookOpened(final Book book) {

        SherlockFragmentActivity activity = getSherlockActivity();

        if ( activity == null ) {
            return;
        }

        this.language = this.bookView.getBook().getMetadata().getLanguage();
        LOG.debug("Got language for book: " + language );

		this.bookTitle = book.getTitle();

        this.config.setLastReadTitle(this.bookTitle);

		this.titleBase = this.bookTitle;
		activity.setTitle(titleBase);
		this.titleBar.setText(titleBase);
		
		activity.supportInvalidateOptionsMenu();

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
		
		updateFromPrefs();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {

		// This is a hack to give the longclick handler time
		// to find the word the user long clicked on.

		if (this.selectedWord != null) {

			final CharSequence word = this.selectedWord.getText();
            final int startIndex = this.selectedWord.getStartOffset();
            final int endIndex = this.selectedWord.getEndOffset();

			String header = String.format(getString(R.string.word_select),
					selectedWord.getText() );

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

            menu.add(R.string.highlight).setOnMenuItemClickListener(
                    new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(android.view.MenuItem menuItem) {

                            highLight( startIndex, endIndex, word.toString() );

                            return false;
                        }
                    });


			android.view.MenuItem newItem = menu
					.add(getString(R.string.wikipedia_lookup));
			newItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(android.view.MenuItem item) {
					lookupWikipedia(word.toString());
					return true;
				}
			});

            android.view.MenuItem newItem3 = menu
                    .add(getString(R.string.lookup_wiktionary));
            newItem3.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    lookupWiktionary(word.toString());
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
	public void highLight(int from, int to, String selectedText) {

        int pageStart = bookView.getStartOfCurrentPage();

        String text = TextUtil.shortenText( selectedText );

        this.highlightManager.registerHighlight(fileName, text, bookView.getIndex(),
                pageStart + from, pageStart + to);

        bookView.update();
    }

    private void showHighlightEditDialog( final HighLight highLight ) {
        final AlertDialog.Builder editalert = new AlertDialog.Builder(context);

        editalert.setTitle(R.string.text_note);
        final EditText input = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        editalert.setView(input);
        input.setText( highLight.getTextNote() );

        editalert.setPositiveButton(R.string.save_note, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                highLight.setTextNote(input.getText().toString());
                bookView.update();
                highlightManager.saveHighLights();
            }
        });
        editalert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        editalert.setNeutralButton(R.string.clear_note, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                highLight.setTextNote(null);
                bookView.update();
                highlightManager.saveHighLights();
            }
        });

        editalert.show();
    }

    private void showHighlightColourDialog( final HighLight highLight ) {

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(
                context, highLight.getColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                //do nothing.
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                highLight.setColor( color );
                bookView.update();
                highlightManager.saveHighLights();
            }
        });

        ambilWarnaDialog.show();
    }

    public void onBookmarkClick( final Bookmark bookmark ) {

        getSherlockActivity().startActionMode( new ActionMode.Callback() {

            private MenuItem delete;

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

                this.delete = menu.add( R.string.delete );
                this.delete.setIcon( R.drawable.trash_can );

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                actionMode.setTitle( R.string.bookmark_options );
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

                boolean result = false;

                if ( menuItem == delete ) {
                    bookmarkDatabaseHelper.deleteBookmark( bookmark );
                    Toast.makeText( context,R.string.bookmark_deleted, Toast.LENGTH_SHORT ).show();
                    result = true;
                }

                if ( result ) {
                    actionMode.finish();
                }

                return result;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    @Override
    public void onHighLightClick(final HighLight highLight) {

        LOG.debug( "onHighLightClick" );

        getSherlockActivity().startActionMode( new ActionMode.Callback() {

            private MenuItem edit;
            private MenuItem delete;
            private MenuItem colour;

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

                this.edit = menu.add( R.string.edit );
                this.edit.setIcon( R.drawable.edit );

                this.colour = menu.add( R.string.set_colour );
                this.colour.setIcon( R.drawable.color );

                this.delete = menu.add( R.string.delete );
                this.delete.setIcon( R.drawable.trash_can );

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                actionMode.setTitle( R.string.highlight_options );
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

                boolean result = false;

                if ( menuItem == edit ) {
                    showHighlightEditDialog( highLight );
                    result = true;
                } else if ( menuItem == delete ) {
                    deleteHightlight( highLight);
                    result = true;
                } else if ( menuItem == colour ) {
                    showHighlightColourDialog( highLight );
                    result = true;
                }

                if ( result ) {
                    actionMode.finish();
                }

                return result;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });

    }

    private void deleteHightlight( final HighLight highLight ) {

        final OnClickListener deleteHighlight = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                highlightManager.removeHighLight(highLight);
                Toast.makeText( context,R.string.highlight_deleted, Toast.LENGTH_SHORT ).show();
                bookView.update();
            }
        };

        if ( highLight.getTextNote() != null && highLight.getTextNote().length() > 0 ) {
            new AlertDialog.Builder(context)
                    .setMessage( R.string.notes_attached )
                    .setNegativeButton( android.R.string.no, null )
                    .setPositiveButton(android.R.string.yes, deleteHighlight )
                    .show();
        } else {
            highlightManager.removeHighLight(highLight);
            Toast.makeText( context,R.string.highlight_deleted, Toast.LENGTH_SHORT ).show();
            bookView.update();
        }
    }

    @Override
	public boolean isDictionaryAvailable() {
		return isIntentAvailable(context, getDictionaryIntent());
	}

	@Override
	public void lookupDictionary(String text) {
		Intent intent = getDictionaryIntent();
		intent.putExtra(EXTRA_QUERY, text); // Search Query
		startActivityForResult(intent, 5);
	}

    private String getLanguageCode() {
        if ( this.language == null || this.language.equals("") || this.language.equalsIgnoreCase("und") ) {
            return Locale.getDefault().getLanguage();
        }

        return this.language;
    }

	@Override
	public void lookupWikipedia(String text) {

		openBrowser("http://" + getLanguageCode() + ".wikipedia.org/wiki/Special:Search?search="
                + URLEncoder.encode(text));
	}

    public void lookupWiktionary(String text) {
        openBrowser("http://" + getLanguageCode() + ".wiktionary.org/w/index.php?title=Special%3ASearch&search="
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

        Activity activity = getActivity();

        if ( activity != null ) {
		    WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		    lp.screenBrightness = (float) level / 100f;
		    activity.getWindow().setAttributes(lp);
        }
	}

	@Override
	public void errorOnBookOpening(String errorMessage) {

        LOG.error( errorMessage );

		closeWaitDialog();

        ReadingActivity readingActivity = (ReadingActivity) getActivity();

        if ( readingActivity != null ) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(readingActivity);
            builder.setContentTitle(getString(R.string.app_name))
                    .setContentText(errorMessage)
                    .setSmallIcon(R.drawable.cross)
                    .setAutoCancel( true );

            builder.setTicker(errorMessage);
            PendingIntent pendingIntent = PendingIntent.getActivity( readingActivity, 0, new Intent(), 0);
            builder.setContentIntent( pendingIntent );

            notificationManager.notify( errorMessage.hashCode(), builder.build() );

            readingActivity.launchActivity(LibraryActivity.class);
        }
	}

    private ProgressDialog getWaitDialog() {

        if ( this.waitDialog == null ) {
            this.waitDialog = new ProgressDialog(context);
            this.waitDialog.setOwnerActivity(getActivity());
        }

        this.waitDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                // This just consumes all key events and does nothing.
                return true;

            }
        });

        return this.waitDialog;
    }



    private void closeWaitDialog() {

        if ( waitDialog != null ) {
            this.waitDialog.dismiss();
            this.waitDialog = null;
        }

    }

	@Override
	public void parseEntryComplete( String name) {

        if (name != null && !name.equals(this.bookTitle)) {
			this.titleBase = this.bookTitle + " - " + name;
		} else {
			this.titleBase = this.bookTitle;
		}
		
		Activity activity = getActivity();

        if ( activity != null ) {

            activity.setTitle(this.titleBase);

            if ( this.ttsPlaybackItemQueue.isActive() && this.ttsPlaybackItemQueue.isEmpty() ) {
                streamTTSToDisk();
            }

            closeWaitDialog();
        }

	}

	@Override
	public void parseEntryStart(int entry) {

        if ( ! isAdded() || getActivity() == null ) {
            return;
        }


        this.viewSwitcher.clearAnimation();
		this.viewSwitcher.setBackgroundDrawable(null);
		restoreColorProfile();
		displayPageNumber(-1); //Clear page number

        ProgressDialog progressDialog = getWaitDialog();
        progressDialog.setMessage(getString( R.string.loading_wait));

        progressDialog.show();
	}	

	@Override
	public void readingFile() {
        if ( isAdded() ) {
            this.getWaitDialog().setMessage(getString( R.string.opening_file) );
        }
	}

	@Override
	public void renderingText() {
        if ( isAdded() ) {
            this.getWaitDialog().setMessage(getString( R.string.loading_text) );
        }
	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	private boolean handleVolumeButtonEvent(KeyEvent event) {

		//Disable volume button handling during TTS
		if (!config.isVolumeKeyNavEnabled() || ttsIsRunning() ) {
			return false;
		}

        Activity activity = getActivity();

        if ( activity == null ) {
            return false;
        }

		boolean invert = false;

		int rotation = Surface.ROTATION_0;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			Display display = activity.getWindowManager().getDefaultDisplay();
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

		if (event.getAction() != KeyEvent.ACTION_DOWN) {
			return true;
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
			if (invert) {
				pageDown(Orientation.HORIZONTAL);
            }
			else {
				pageUp(Orientation.HORIZONTAL);
            }
		} else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (invert) {
				pageUp(Orientation.HORIZONTAL);
            } else {
				pageDown(Orientation.HORIZONTAL);
            }
        }
	    
        return true;
	}

	public boolean dispatchMediaKeyEvent(KeyEvent event) {

		int action = event.getAction();
		int keyCode = event.getKeyCode();

        if ( audioManager.isMusicActive() && ! ttsIsRunning() ) {
            return false;
        }

        switch (keyCode) {

            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                return simulateButtonPress(action, R.id.playPauseButton, playPauseButton);

            case KeyEvent.KEYCODE_MEDIA_STOP:
                return simulateButtonPress(action, R.id.stopButton, stopButton );

            case KeyEvent.KEYCODE_MEDIA_NEXT:
               return simulateButtonPress(action, R.id.nextButton, nextButton );

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                return simulateButtonPress(action, R.id.prevButton, prevButton );
        }
		
		return false;
	}

    private boolean simulateButtonPress(int action, int idToSend, ImageButton buttonToClick ) {
        if ( action == KeyEvent.ACTION_DOWN ) {
            onMediaButtonEvent(idToSend);
            buttonToClick.setPressed(true);
        } else {
            buttonToClick.setPressed(false);
        }

        buttonToClick.invalidate();
        return true;
    }

	
	public boolean dispatchKeyEvent(KeyEvent event) {

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        LOG.debug("Got key event: " + keyCode + " with action " + action );


        if ( searchMenuItem != null && searchMenuItem.isActionViewExpanded() ) {
            boolean result = searchMenuItem.getActionView().dispatchKeyEvent(event);

            if ( result ) {
                return true;
            }
        }

		final int KEYCODE_NOOK_TOUCH_BUTTON_LEFT_TOP = 92;
		final int KEYCODE_NOOK_TOUCH_BUTTON_LEFT_BOTTOM = 93;
		final int KEYCODE_NOOK_TOUCH_BUTTON_RIGHT_TOP = 94;
		final int KEYCODE_NOOK_TOUCH_BUTTON_RIGHT_BOTTOM = 95;
        
		boolean nook_touch_up_press = false;

		if (isAnimating() && action == KeyEvent.ACTION_DOWN) {
			stopAnimating();
			return true;
		}
		
		/*
		 * Tricky bit of code here: if we are NOT running TTS,
		 * we want to be able to start it using the play/pause button.
		 * 
		 * When we ARE running TTS, we'll get every media event twice:
		 * once through the receiver and once here if focused. 
		 * 
		 * So, we only try to read media events here if tts is running.
		 */		
		if ( ! ttsIsRunning() && dispatchMediaKeyEvent(event) ) {
			return true;
		}

        LOG.debug("Key event is NOT a media key event.");

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
                    updateFromPrefs();
                    return true;
				} else if (bookView.hasPrevPosition()) {
					bookView.goBackInHistory();
					return true;
				}
			}

            return false;

		case KEYCODE_NOOK_TOUCH_BUTTON_LEFT_TOP:
		case KEYCODE_NOOK_TOUCH_BUTTON_RIGHT_TOP:
                    nook_touch_up_press = true;
		case KEYCODE_NOOK_TOUCH_BUTTON_LEFT_BOTTOM:
		case KEYCODE_NOOK_TOUCH_BUTTON_RIGHT_BOTTOM:
                    if(!Configuration.IS_NOOK_TOUCH || action == KeyEvent.ACTION_UP)
                        return false;
                    if(nook_touch_up_press == config.isNookUpButtonForward())
                        pageDown(Orientation.HORIZONTAL);
                    else
                        pageUp(Orientation.HORIZONTAL);
                    return true;
		}


        LOG.debug("Not handling key event: returning false.");
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

		PageTimer timer = new PageTimer(after, pageNumberView.getHeight());

		timer.setSpeed(config.getScrollSpeed());

		dummyView.setAnimator(timer);
	}

	private void doPageCurl(boolean flipRight, boolean pageDown) {

		if (isAnimating() || bookView == null ) {
			return;
		}

		this.viewSwitcher.setInAnimation(null);
		this.viewSwitcher.setOutAnimation(null);

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		Bitmap before = getBookViewSnapshot();

		this.pageNumberView.setVisibility(View.GONE);

		PageCurlAnimator animator = new PageCurlAnimator(flipRight);

		// Pagecurls should only take a few frames. When the screen gets
		// bigger, so do the frames.
		animator.SetCurlSpeed(bookView.getWidth() / 8);

		animator.setBackgroundColor(config.getBackgroundColor());
		
		if (pageDown) {
			bookView.pageDown();						
		} else {
			bookView.pageUp();			
		}
		
		Bitmap after = getBookViewSnapshot();
		
		if ( flipRight ) {
			animator.setBackgroundBitmap(after);
			animator.setForegroundBitmap(before);
		} else {
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
				pageNumberView.setVisibility(View.VISIBLE);

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

		this.pageNumberView.setVisibility(View.VISIBLE);
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

			if (config.isShowPageNumbers()) {

				/**
				 * FIXME: creating an intermediate bitmap here because I can't
				 * figure out how to draw the pageNumberView directly on the
				 * canvas and have it show up in the right place.
				 */

				Bitmap pageNumberBitmap = Bitmap.createBitmap(
						pageNumberView.getWidth(), pageNumberView.getHeight(),
						Config.ARGB_8888);
				Canvas pageNumberCanvas = new Canvas(pageNumberBitmap);

				pageNumberView.layout(0, 0, pageNumberView.getWidth(),
						pageNumberView.getHeight());
				pageNumberView.draw(pageNumberCanvas);

				canvas.drawBitmap(pageNumberBitmap, 0, viewSwitcher.getHeight()
						- pageNumberView.getHeight(), new Paint());

				pageNumberBitmap.recycle();

			}

			return bitmap;
		} catch (OutOfMemoryError out) {
			viewSwitcher.setBackgroundColor(config.getBackgroundColor());
		}

		return null;
	}

	private void prepareSlide(Animation inAnim, Animation outAnim) {

		Bitmap bitmap = getBookViewSnapshot();
		dummyView.setImageBitmap(bitmap);

		this.pageNumberView.setVisibility(View.GONE);

		inAnim.setAnimationListener(new Animation.AnimationListener() {

			public void onAnimationStart(Animation animation) {}

			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				onSlideFinished();
			}
		});
		
		viewSwitcher.layout(0, 0, viewSwitcher.getWidth(),
				viewSwitcher.getHeight());
		dummyView.layout(0, 0, viewSwitcher.getWidth(),
				viewSwitcher.getHeight());

		this.viewSwitcher.showNext();

		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);
	}

	private void onSlideFinished() {
		if ( currentPageNumber > 0 ) {
			this.pageNumberView.setVisibility(View.VISIBLE);
		}
	}

	private void pageDown(Orientation o) {

		if (bookView.isAtEnd()) {
			return;
		}		 

		stopAnimating();

		if (o == Orientation.HORIZONTAL) {

			AnimationStyle animH = config.getHorizontalAnim();
			ReadingDirection direction = config.getReadingDirection();

			if (animH == AnimationStyle.CURL) {
				doPageCurl(direction == ReadingDirection.LEFT_TO_RIGHT, true);
			} else if (animH == AnimationStyle.SLIDE) {
				
				if ( direction == ReadingDirection.LEFT_TO_RIGHT ) {
					prepareSlide(Animations.inFromRightAnimation(),
							Animations.outToLeftAnimation());
				} else {
					prepareSlide(Animations.inFromLeftAnimation(),
							Animations.outToRightAnimation());
				}
				
				viewSwitcher.showNext();
				bookView.pageDown();
			} else {
				bookView.pageDown();
			}

		} else {
			if (config.getVerticalAnim() == AnimationStyle.SLIDE) {
				prepareSlide(Animations.inFromBottomAnimation(),
						Animations.outToTopAnimation());
				viewSwitcher.showNext();
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
			ReadingDirection direction = config.getReadingDirection();

			if (animH == AnimationStyle.CURL) {
				doPageCurl(direction == ReadingDirection.RIGHT_TO_LEFT, false);
			} else if (animH == AnimationStyle.SLIDE) {
				if ( direction == ReadingDirection.LEFT_TO_RIGHT ) {
					prepareSlide(Animations.inFromLeftAnimation(),
						Animations.outToRightAnimation());
				} else {
					prepareSlide(Animations.inFromRightAnimation(),
							Animations.outToLeftAnimation());
				}
				viewSwitcher.showNext();
				bookView.pageUp();
			} else {
				bookView.pageUp();
			}

		} else {

			if (config.getVerticalAnim() == AnimationStyle.SLIDE) {
				prepareSlide(Animations.inFromTopAnimation(),
						Animations.outToBottomAnimation());
				viewSwitcher.showNext();
			}

			bookView.pageUp();
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {

        SherlockFragmentActivity activity = getSherlockActivity();

        if ( activity == null ) {
            return;
        }

		MenuItem nightMode = menu.findItem(R.id.profile_night);
		MenuItem dayMode = menu.findItem(R.id.profile_day);

		MenuItem tts = menu.findItem( R.id.text_to_speech );
		tts.setEnabled(ttsAvailable);

		activity.getSupportActionBar().show();

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

		if (!isIntentAvailable(context, intent)) {
			menu.findItem(R.id.open_file).setVisible(false);
		}

		activity.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	private void hideTitleBar() {
		titleBarLayout.setVisibility(View.GONE);
	}

	/**
	 * This is called after the file manager finished.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK && data != null) {
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

        Activity activity = getActivity();

        if ( activity != null ) {

            activity.setTitle(R.string.app_name);
            this.bookTitle = null;
            this.titleBase = null;

            bookView.clear();

            updateFileName(null, fileName);
            new DownloadProgressTask().execute();
        }
	}

	@Override
	public void onStop() {
		super.onStop();

        LOG.debug("onStop() called.");
		printScreenAndCallState("onStop()");

		closeWaitDialog();
        libraryService.close();
	}

    public BookView getBookView() {
        return this.bookView;
    }

	public void saveReadingPosition() {
		if (this.bookView != null) {

			int index = this.bookView.getIndex();
			int position = this.bookView.getProgressPosition();
			
			if ( index != -1 && position != -1 ) {			
				config.setLastPosition(this.fileName, position);
				config.setLastIndex(this.fileName, index);
			
				sendProgressUpdateToServer(index, position);
			}
		}

	}

    public void share(int from, int to, String selectedText) {

        int pageStart = bookView.getStartOfCurrentPage();

        String text = bookTitle + ", " + authorField.getText() + "\n";

        int offset = pageStart + from;

        int pageNumber = bookView.getPageNumberFor( bookView.getIndex(), offset );
        int totalPages = bookView.getTotalNumberOfPages();

        int percentage = bookView.getPercentageFor( bookView.getIndex(), offset );

        if ( pageNumber != -1 ) {
            text = text + String.format( getString(R.string.page_number_of),
                    pageNumber, totalPages ) + " (" + progressPercentage + "%)\n\n";

        } else {
            text += "" + progressPercentage + "%\n\n";
        }

        text += selectedText;

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);


        sendIntent.putExtra(Intent.EXTRA_TEXT, text );
        sendIntent.setType("text/plain");

        startActivity(Intent.createChooser(sendIntent, getText(R.string.abs__share_action_provider_share_with)));

    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.reading_menu, menu);

        this.searchMenuItem = menu.findItem(R.id.search_text);

        if (this.searchMenuItem != null) {
            final com.actionbarsherlock.widget.SearchView searchView =
                    (com.actionbarsherlock.widget.SearchView) searchMenuItem.getActionView();

            if (searchView != null) {

                searchView.setSubmitButtonEnabled(true);
                searchView.setOnQueryTextListener(new com.actionbarsherlock.widget.SearchView.OnQueryTextListener() {

                    //This is a work-around, since we get the onQuerySubmit() event twice
                    //when the user hits enter
                    private String lastQuery = "";

                    @Override
                    public boolean onQueryTextSubmit(String query) {

                        if ( query.equals(lastQuery) && searchResults != null ) {
                            showSearchResultDialog(searchResults);
                        } else if ( ! query.equals(lastQuery) ) {
                            searchResults = null;
                            lastQuery = query;
                            performSearch(query);
                        }

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                } );
            }
        }
	}

	@Override
	public void onOptionsMenuClosed(android.view.Menu menu) {
		updateFromPrefs();
        hideTitleBar();
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
				Toast.makeText(context, R.string.enter_email, Toast.LENGTH_LONG)
						.show();
			}
			return true;

		case R.id.search_text:
			onSearchRequested();
			return true;

		case R.id.preferences:
			saveConfigState();
			Intent i = new Intent(context, PageTurnerPrefsActivity.class);
			startActivity(i);
			return true;

		case R.id.open_file:
			launchFileManager();
			return true;

		case R.id.rolling_blind:
			startAutoScroll();
			return true;
			
		case R.id.text_to_speech:
			startTextToSpeech();
			return true;

		case R.id.about:
			dialogFactory.buildAboutDialog().show();
			return true;

		case R.id.add_bookmark:
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.addToBackStack(null);
			AddBookmarkFragment fragment = new AddBookmarkFragment();

            fragment.setFilename(this.fileName);
            fragment.setBookmarkDatabaseHelper(bookmarkDatabaseHelper);
			fragment.setBookIndex(this.bookView.getIndex());
            fragment.setBookPosition(this.bookView.getProgressPosition());

            String firstLine = this.bookView.getFirstLine();

            if ( firstLine.length() > 20 ) {
                firstLine = firstLine.substring(0, 20) + "…";
            }

            fragment.setInitialText( firstLine );

			fragment.show(ft, "dialog");
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

        SherlockFragmentActivity activity = getSherlockActivity();

        if ( activity == null ) {
            return;
        }

        stopAnimating();

        if (this.titleBarLayout.getVisibility() == View.VISIBLE) {
            titleBarLayout.setVisibility(View.GONE);

            updateFromPrefs();
        } else {
            titleBarLayout.setVisibility(View.VISIBLE);

            getSherlockActivity().getSupportActionBar().show();
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
	
	@Override
	public boolean onSwipeLeft() {

		if (config.isHorizontalSwipeEnabled()) {
			
			if ( config.getReadingDirection() == ReadingDirection.LEFT_TO_RIGHT ) {			
				pageDown(Orientation.HORIZONTAL);
			} else {
				pageUp(Orientation.HORIZONTAL);
			}
		
			return true;
		}

		return false;
	}

	@Override
	public boolean onSwipeRight() {

		if (config.isHorizontalSwipeEnabled()) {
			
			if ( config.getReadingDirection() == ReadingDirection.LEFT_TO_RIGHT ) {			
				pageUp(Orientation.HORIZONTAL);
			} else {
				pageDown(Orientation.HORIZONTAL);
			}
			
			return true;
		}

		return false;
	}

	@Override
	public boolean onTapLeftEdge() {
		if (config.isHorizontalTappingEnabled()) {
			if ( config.getReadingDirection() == ReadingDirection.LEFT_TO_RIGHT ) {			
				pageUp(Orientation.HORIZONTAL);
			} else {
				pageDown(Orientation.HORIZONTAL);
			}
			
			return true;
		}

		return false;
	}

	@Override
	public boolean onTapRightEdge() {
		if (config.isHorizontalTappingEnabled()) {
			
			if ( config.getReadingDirection() == ReadingDirection.LEFT_TO_RIGHT ) {			
				pageDown(Orientation.HORIZONTAL);
			} else {
				pageUp(Orientation.HORIZONTAL);
			}
			
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
	public boolean onTapBottomEdge() {
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
						.makeText(context, brightness + ": "
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
    public void onWordLongPressed(int startOffset, int endOffset, CharSequence word) {

        this.selectedWord = new BookView.SelectedWord( startOffset, endOffset, word );

        Activity activity = getActivity();

        if ( activity != null ) {
			activity.openContextMenu(bookView);
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
			Toast.makeText(context, getString(R.string.install_oi),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void showPickProgressDialog(final List<BookProgress> results) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(getString(R.string.cloud_bm));

		ProgressListAdapter adapter = new ProgressListAdapter(context, bookView, results);
		builder.setAdapter(adapter, adapter);

		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(getActivity());
		dialog.show();
	}

    public boolean hasTableOfContents() {
        List<TocEntry> toc = this.bookView.getTableOfContents();
        return toc != null && ! toc.isEmpty();
    }

    public List<NavigationCallback> getTableOfContents() {

        List<NavigationCallback> result = new ArrayList<NavigationCallback>();
        List<TocEntry> tocEntries = this.bookView.getTableOfContents();

        if ( tocEntries != null ) {

            for ( final TocEntry tocEntry: tocEntries ) {

                result.add(new NavigationCallback() {
                    @Override
                    public String getTitle() {
                        return tocEntry.getTitle();
                    }

                    @Override
                    public String getSubtitle() {
                        return "";
                    }

                    @Override
                    public void onClick() {
                        bookView.navigateTo( tocEntry.getHref() );
                    }

                    @Override
                    public void onLongClick() {
                        //Do nothing
                    }
                });
            }

        }

        return result;
    }

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		if (this.bookView != null) {
			outState.putInt(POS_KEY, this.bookView.getProgressPosition());
			outState.putInt(IDX_KEY, this.bookView.getIndex());
		}

	}

	private void sendProgressUpdateToServer(final int index, final int position) {

        libraryService.updateReadingProgress(fileName, progressPercentage);

		backgroundHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					progressService.storeProgress(fileName,
							index, position,
							progressPercentage);
				} catch (Exception e) {
					LOG.error("Error saving progress", e);
				}
			}
		});
	}

    @Override
    public void performSearch(String query) {

        LOG.debug("Starting search for: " + query );

        final ProgressDialog searchProgress = new ProgressDialog(context);
        searchProgress.setOwnerActivity(getActivity());
        searchProgress.setCancelable(true);
        searchProgress.setMax(100);
        searchProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        final SearchTextTask task = new SearchTextTask(bookView.getBook()) {

            int i = 0;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                searchProgress.setMessage(getString(R.string.search_wait));
                searchProgress.show();

                // Hide on-screen keyboard if it is showing
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

            }

            @Override
            protected void onProgressUpdate(SearchResult... values) {

                if ( ! isAdded() ) {
                    return;
                }

                super.onProgressUpdate(values);

                LOG.debug("Found match at index=" + values[0].getIndex()
                        + ", offset=" + values[0].getStart() + " with context "
                        + values[0].getDisplay());
                SearchResult res = values[0];

                if (res.getDisplay() != null) {
                    i++;
                    String update = String.format(
                            getString(R.string.search_hits), i);
                    searchProgress.setMessage(update);
                }

                searchProgress.setProgress( bookView.getPercentageFor(res.getIndex(), res.getStart() ));
            }

            @Override
            protected void onCancelled() {
                if ( isAdded() ) {
                    Toast.makeText(context, R.string.search_cancelled,
                        Toast.LENGTH_LONG).show();
                }
            }

            protected void onPostExecute(java.util.List<SearchResult> result) {

                searchProgress.dismiss();

                if (!isCancelled() && isAdded() ) {
                    if (result.size() > 0) {
                        searchResults = result;
                        showSearchResultDialog(result);
                    } else {
                        Toast.makeText(context,
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

        task.execute(query);
    }

    private void setSupportProgressBarIndeterminateVisibility(boolean enable) {
        SherlockFragmentActivity activity = getSherlockActivity();
        if ( activity != null) {
            LOG.debug("Setting progress bar to " + enable );
            activity.setSupportProgressBarIndeterminateVisibility(enable);
        } else {
            LOG.debug("Got null activity.");
        }
    }

    @Override
    public void onCalculatePageNumbersComplete() {
       setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onStartCalculatePageNumbers() {
        setSupportProgressBarIndeterminateVisibility(true);
    }

    public void onSearchRequested() {
        if ( this.searchMenuItem != null && searchMenuItem.getActionView() != null ) {
            getSherlockActivity().getSupportActionBar().show();
            this.searchMenuItem.expandActionView();
            this.searchMenuItem.getActionView().requestFocus();
        } else {
            dialogFactory.showSearchDialog(R.string.search_text, R.string.enter_query, this);
        }
    }

    //Hack to prevent showing the dialog twice
    private boolean isSearchResultsDialogShowing = false;

	private void showSearchResultDialog(
			final List<SearchResult> results) {

        if ( isSearchResultsDialogShowing ) {
            return;
        }

        isSearchResultsDialogShowing = true;

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.search_results);

		SearchResultAdapter adapter = new SearchResultAdapter(context, bookView, results);
		builder.setAdapter(adapter, adapter);

		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(getActivity());
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                isSearchResultsDialogShowing = false;
            }
        });
		dialog.show();
	}

    public boolean hasSearchResults() {
        return this.searchResults != null && !this.searchResults.isEmpty();
    }

    public List<NavigationCallback> getSearchResults() {

        List<NavigationCallback> result = new ArrayList<NavigationCallback>();

        if ( searchResults == null ) {
            return result;
        }

        final int totalNumberOfPages = bookView.getTotalNumberOfPages();

        int counter = 0;

        for ( final SearchResult searchResult: this.searchResults ) {

            int percentage = bookView.getPercentageFor(searchResult.getIndex(), searchResult.getStart());
            int pageNumber = bookView.getPageNumberFor(searchResult.getIndex(), searchResult.getStart());

            final String text;
            final int currentCount = counter;

            if ( pageNumber != -1 ) {
                text = String.format( context.getString(R.string.page_number_of),
                        pageNumber, totalNumberOfPages )
                        + " (" + percentage + "%)";
            } else {
                text = percentage + "%";
            }

            result.add( new NavigationCallback() {
                @Override
                public String getTitle() {
                    return searchResult.getDisplay();
                }

                @Override
                public String getSubtitle() {
                    return text;
                }

                @Override
                public void onClick() {
                    bookView.navigateBySearchResult( searchResult );
                }

                @Override
                public void onLongClick() {
                    //Do nothing
                }
            });

            counter++;
        }

        return result;
    }


    public boolean hasHighlights() {

        List<HighLight> highLights = this.highlightManager.getHighLights( bookView.getFileName() );

        return highLights != null && ! highLights.isEmpty();
    }

    public boolean hasBookmarks() {

        List<Bookmark> bookmarks = this.bookmarkDatabaseHelper.getBookmarksForFile( bookView.getFileName() );

        return bookmarks != null && ! bookmarks.isEmpty();
    }

    private String getHighlightLabel( int index, int position, String text ) {

        final int totalNumberOfPages = bookView.getTotalNumberOfPages();

        int percentage = bookView.getPercentageFor( index, position );
        int pageNumber = bookView.getPageNumberFor( index, position );

        String result = percentage + "%";

        if ( pageNumber != -1 ) {
            result = String.format( context.getString(R.string.page_number_of),
                    pageNumber, totalNumberOfPages )
                    + " (" + percentage + "%)";
        }

        if ( text != null && text.trim().length() > 0 ) {
            result += ": " + TextUtil.shortenText( text );
        }

        return result;
    }

    public List<NavigationCallback> getBookmarks() {

        List<Bookmark> bookmarks = this.bookmarkDatabaseHelper.getBookmarksForFile( bookView.getFileName() );

        List<NavigationCallback> result = new ArrayList<NavigationCallback>();

        for ( final Bookmark bookmark: bookmarks ) {

            final String finalText = getHighlightLabel( bookmark.getIndex(),
                    bookmark.getPosition(), null );

            result.add( new NavigationCallback() {
                @Override
                public String getTitle() {
                    return bookmark.getName();
                }

                @Override
                public String getSubtitle() {
                    return finalText;
                }

                @Override
                public void onClick() {
                    bookView.navigateTo( bookmark.getIndex(), bookmark.getPosition() );
                }

                @Override
                public void onLongClick() {
                    onBookmarkClick( bookmark );
                }
            });

        }


        return result;
    }


    public List<NavigationCallback> getHighlights() {

        List<HighLight> highLights = this.highlightManager.getHighLights( bookView.getFileName() );

        List<NavigationCallback> result = new ArrayList<NavigationCallback>();

        for ( final HighLight highLight: highLights ) {

            final String finalText = getHighlightLabel( highLight.getIndex(), highLight.getStart(),
                    highLight.getTextNote() );

            result.add( new NavigationCallback() {
                @Override
                public String getTitle() {
                    return highLight.getDisplayText();
                }

                @Override
                public String getSubtitle() {
                    return finalText;
                }

                @Override
                public void onClick() {
                    bookView.navigateTo( highLight.getIndex(), highLight.getStart() );
                }

                @Override
                public void onLongClick() {
                    onHighLightClick( highLight );
                }
            });

        }

        return result;
    }

	private class ManualProgressSync extends
			AsyncTask<Void, Integer, List<BookProgress>> {

		private boolean accessDenied = false;

		@Override
		protected void onPreExecute() {
            if ( isAdded() ) {

                ProgressDialog progressDialog = getWaitDialog();
                progressDialog.setMessage(getString(R.string.syncing));
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        ManualProgressSync.this.cancel(true);
                    }
                });

                progressDialog.show();
            }
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
        protected void onCancelled() {
            closeWaitDialog();
        }

        @Override
		protected void onPostExecute(List<BookProgress> progress) {
			closeWaitDialog();

			if (progress == null) {

				AlertDialog.Builder alertDialog = new AlertDialog.Builder(
						getActivity());

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

			} else if ( progress.isEmpty() ) {
			    Toast.makeText(context, R.string.no_sync_points, Toast.LENGTH_LONG).show();
			} else {
                showPickProgressDialog(progress);
            }
		}
	}

	private class DownloadProgressTask extends
			AsyncTask<Void, Integer, BookProgress> {

		@Override
		protected void onPreExecute() {
            if ( isAdded() ) {
			    ProgressDialog progressDialog = getWaitDialog();
                progressDialog.setMessage(getString(R.string.syncing));
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        DownloadProgressTask.this.cancel(true);

                        ProgressDialog progressDialog = getWaitDialog();
                        progressDialog.setMessage( getString( R.string.cancelling) );
                        progressDialog.show();

                    }
                });

                progressDialog.show();
            }
		}

        @Override
        protected void onCancelled() {


            bookView.restore();
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
			    //Ignore, since it's a background process
            }

			return null;
		}

		@Override
		protected void onPostExecute(BookProgress progress) {
			closeWaitDialog();

			int index = bookView.getIndex();
			int pos = bookView.getProgressPosition();

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
	
	private class PageTurnerMediaReceiver extends BroadcastReceiver {
		
		private final Logger LOG = LoggerFactory.getLogger("PTSMediaReceiver");
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			LOG.debug("Got intent: " + intent.getAction() );
			
			if ( intent.getAction().equals(MediaButtonReceiver.INTENT_PAGETURNER_MEDIA)) {
				KeyEvent event = new KeyEvent(
						intent.getIntExtra("action", 0),
						intent.getIntExtra("keyCode", 0));
				dispatchMediaKeyEvent(event);
			}
			
		}
	}	
}
