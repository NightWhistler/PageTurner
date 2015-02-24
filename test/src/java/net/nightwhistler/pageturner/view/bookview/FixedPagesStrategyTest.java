package net.nightwhistler.pageturner.view.bookview;

import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;
import org.robolectric.RobolectricTestRunner;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.dto.HighLight;
import net.nightwhistler.pageturner.view.HighlightManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static net.nightwhistler.pageturner.view.bookview.LayoutTextUtil.getSpanned;
import static net.nightwhistler.pageturner.view.bookview.LayoutTextUtil.getStringOfLength;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class FixedPagesStrategyTest {
	
	private StaticLayoutFactory mockFactory = mock(StaticLayoutFactory.class);
	private BookView mockBookView;
    private TextView mockTextView;
	
	private FixedPagesStrategy strategy;
	
	@Before
	public void createMocks() {		
		
		this.mockFactory = mock(StaticLayoutFactory.class);
		Configuration mockConfig = mock(Configuration.class);
		this.mockBookView = mock(BookView.class);
		this.mockTextView = mock(TextView.class);

        HighlightManager mockManager = mock(HighlightManager.class);
        when(mockManager.getHighLights(anyString())).thenReturn( new ArrayList<HighLight>() );

		when(mockBookView.getInnerView()).thenReturn(mockTextView);

		//Layout has lines of 10 characters, each 10px high
		final int LINE_WIDTH = 10; //10 characters
		final int LINE_HEIGHT = 10; //10 pixels per line

		initMockLayout(LINE_WIDTH, LINE_HEIGHT);
						
		this.strategy = new FixedPagesStrategy();
        this.strategy.setLayoutFactory(mockFactory);
        this.strategy.setBookView(mockBookView);
        this.strategy.setConfig(mockConfig);
        this.strategy.setHighlightManager(mockManager);
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
		when(this.mockBookView.getMeasuredHeight()).thenReturn(50);
				
		List<Integer> offsets = this.strategy.getPageOffsets(text, false);
		
		assertEquals(6, offsets.size() );

        assertEquals((int) 0, (int) offsets.get(0));
        assertEquals((int) 50, (int) offsets.get(1));
			
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
		when(this.mockBookView.getMeasuredHeight()).thenReturn(55);

		List<Integer> offsets = this.strategy.getPageOffsets(text, false);

		//The end result should still be 6 pages
		assertEquals(6, offsets.size() );

        //Offsets are still 0, 50...
        assertEquals((int) 0, (int) offsets.get(0));
        assertEquals((int) 50, (int) offsets.get(1));
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
		when(this.mockBookView.getMeasuredHeight()).thenReturn(100);
		List<Integer> offsets = this.strategy.getPageOffsets(text, false);

		//Each line should be a page
		assertEquals(3, offsets.size() );

        assertEquals((int) 0, (int) offsets.get(0));
        assertEquals((int) 1, (int) offsets.get(1));
        assertEquals((int) 2, (int) offsets.get(2));
	}


    @Test
    public void testEmptyText() {
        Spanned text = getSpanned("");

        List<Integer> offsets = this.strategy.getPageOffsets(text, false);

        //We should have an empty list
        assertEquals(0, offsets.size() );
    }

    /**
     * In this test the text is just 1 line long.
     *
     * Should yield 1 page.
     */
    @Test
    public void testOneLinePage() {

        //5 chars wide, 105px high
        initMockLayout(5, 105);

         Spanned text = getSpanned("1234");

        /*
		 * The BookView is 100px high
		 */
        when(this.mockBookView.getMeasuredHeight()).thenReturn(100);
        List<Integer> offsets = this.strategy.getPageOffsets(text, false);

        //We should have a single page
        assertEquals(1, offsets.size() );
        assertEquals((int) 0, (int) offsets.get(0));
    }
	
	@Test
	public void testPageTurning() {
		
        String page1 = getStringOfLength("ABCDEFGHIJ", 50);
        String page2 = getStringOfLength("012345", 50);
        String page3 = getStringOfLength("XYZABC", 25);

		Spanned text = getSpanned( page1 + page2 + page3 );

        assertEquals(125, text.length());  //Sanity check

		/*
		 * The BookView is 50px high, meaning it will fit 5 lines.
		 */
		when(this.mockBookView.getMeasuredHeight()).thenReturn(50);

        List<Integer> offsets = this.strategy.getPageOffsets(text, false);
        assertEquals(3, offsets.size() );
		
		this.strategy.loadText( text );
        this.strategy.updatePosition();

		assertEquals(0, this.strategy.getCurrentPage());

		verify( mockTextView ).setText(page1);

        this.strategy.pageDown();
        verify( mockTextView ).setText(page2);

        this.strategy.pageDown();
        verify( mockTextView ).setText(page3);

        this.strategy.pageUp();
        verify( mockTextView, times(2) ).setText(page2);
	}
	

}
