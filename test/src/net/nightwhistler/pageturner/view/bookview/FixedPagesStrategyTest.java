package net.nightwhistler.pageturner.view.bookview;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import net.nightwhistler.pageturner.Configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FixedPagesStrategyTest {
	
	private StaticLayoutFactory mockFactory = mock(StaticLayoutFactory.class);
	private BookView mockBookView;
	
	private FixedPagesStrategy strategy;

	@Before
	public void createMocks() {		
		
		this.mockFactory = mock(StaticLayoutFactory.class);
		Configuration mockConfig = mock(Configuration.class);
		this.mockBookView = mock(BookView.class);
		TextView mockTextView = mock(TextView.class);
		
		when(mockBookView.getInnerView()).thenReturn(mockTextView);

		//Layout has lines of 10 characters, each 10px high
		final int LINE_WIDTH = 10; //100 characters
		final int LINE_HEIGHT = 10; //10 pixels per line

		initMockLayout(LINE_WIDTH, LINE_HEIGHT);
						
		this.strategy = new FixedPagesStrategy(mockBookView, mockConfig, mockFactory);		
	}	
	
	private void initMockLayout( final int lineWidth, final int lineHeight ) {
		when(mockFactory.create( any(CharSequence.class), any(TextPaint.class),
				anyInt(), anyInt())).thenAnswer(new Answer<StaticLayout>() {
					@Override
					public StaticLayout answer(InvocationOnMock invocation)
							throws Throwable {
						if ( invocation != null && invocation.getArguments()[0] != null ) {
							return LayoutTextUtil.createMockLayout(invocation.getArguments()[0].toString(), 
								lineWidth, lineHeight);
						} else {
							return null;
						}
					}
				});		
	}
	
	@Test
	/**
	 * Simple testcase: the window fits 5 lines exactly.
	 */
	public void testGetOffsets() {
		
		//Text is 275 characters long, which is 5.5 pages. 
		//Every line should be exactly ABCDEFGHIJ
		String text = LayoutTextUtil.getStringOfLength("ABCDEFGHIJ", 275);
		
		/*
		 * The BookView is 50px high, meaning it will fit 5 lines,
		 * which means 50 characters. 
		 */
		when(this.mockBookView.getHeight()).thenReturn(50);		
				
		List<Integer> offsets = this.strategy.getPageOffsets(text, false);
		
		Assert.assertEquals(6, offsets.size() );		
			
	}
	
	@Test
	/**
	 * Slightly more complicated test: the window fits
	 * 5 lines and a bit.
	 * 
	 */
	public void testGetOffsetsBadFit() {

		//Text is 275 characters long, which is 5.5 pages. 
		//Every line should be exactly ABCDEFGHIJ
		String text = LayoutTextUtil.getStringOfLength("ABCDEFGHIJ", 275);

		/*
		 * The BookView is 55px high, meaning it will fit 5 lines, but not 6.
		 */
		when(this.mockBookView.getHeight()).thenReturn(55);		

		List<Integer> offsets = this.strategy.getPageOffsets(text, false);

		//The end result should still be 6 pages
		Assert.assertEquals(6, offsets.size() );
	}
	
	@Test
	/**
	 * Special case, where the lines are higher than
	 * can be fit on the screen. This can happen with
	 * large images.
	 */
	public void testVeryLargeLines() {
		
		//1 char wide, 105px high
		initMockLayout(1, 105);
		
		String text = "ABC";
		
		/*
		 * The BookView is 100px high
		 */
		when(this.mockBookView.getHeight()).thenReturn(100);
		List<Integer> offsets = this.strategy.getPageOffsets(text, false);

		//Each line should be a page
		Assert.assertEquals(3, offsets.size() );		
		
	}
	
	

}
