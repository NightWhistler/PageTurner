package net.nightwhistler.pageturner.view.bookview;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyFloat;

import java.util.List;

import net.nightwhistler.pageturner.Configuration;
import nl.siegmann.epublib.util.StringUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.xtremelabs.robolectric.RobolectricTestRunner;

import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;

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

		when(mockFactory.create( any(CharSequence.class), any(TextPaint.class),
				anyInt(), anyInt())).thenAnswer(new Answer<StaticLayout>() {
				@Override
				public StaticLayout answer(InvocationOnMock invocation)
						throws Throwable {
					return LayoutTextUtil.createMockLayout(invocation.getArguments()[0].toString(), 
							LINE_WIDTH, LINE_HEIGHT);
				}
			});		
						
		this.strategy = new FixedPagesStrategy(mockBookView, mockConfig, mockFactory);		
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
		 * The BookView is 60px high, meaning it will fit 5 lines,
		 * which means 50 characters. The 10px is empty space at
		 * the bottom for page numbers.
		 */
		when(this.mockBookView.getHeight()).thenReturn(60);		
				
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
		 * The BookView is 65px high, meaning it will fit 5 lines, but not 6.
		 */
		when(this.mockBookView.getHeight()).thenReturn(65);		

		List<Integer> offsets = this.strategy.getPageOffsets(text, false);

		//The end result should still be 6 pages
		Assert.assertEquals(6, offsets.size() );
	}
	
	

}
