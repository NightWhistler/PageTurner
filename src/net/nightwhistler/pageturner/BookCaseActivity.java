package net.nightwhistler.pageturner;

import net.nightwhistler.pageturner.view.BookCaseView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.globalmentor.android.widget.VerifiedFlingListener;

public class BookCaseActivity extends Activity {
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseActivity.class);
	
	private BookCaseView bookCaseView;
	
	private GestureDetector gestureDetector;
	private View.OnTouchListener touchListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookcase);
		this.bookCaseView = (BookCaseView) findViewById(R.id.bookCase);
		
		this.gestureDetector = new GestureDetector(this, 
				new ClickListener(this));
		this.touchListener = new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);				
			}
		};		
		
		bookCaseView.setOnTouchListener(touchListener);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.gestureDetector.onTouchEvent(event);
	}
	
	private class ClickListener extends VerifiedFlingListener {
		
		public ClickListener(Context context) {
			super(context);
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			//LOG.debug("Single tap confirmed.");
			bookCaseView.fireClick(e.getX(), e.getY() );
			return true;
		}
		
	}
	
}
