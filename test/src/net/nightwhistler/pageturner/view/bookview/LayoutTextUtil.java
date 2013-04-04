package net.nightwhistler.pageturner.view.bookview;

import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayoutTextUtil {

	/**
	 * Very simple Layout class with predictable properties.
	 * 
	 * It ignores all formatting, creating fixed-length lines.
	 * 
	 * @author Alex Kuiper
	 *
	 */
	private static class SimpleLayout {
		private CharSequence text;
		private int lineWidth;
		private int lineHeight;
		
		public SimpleLayout(CharSequence text, int lineWidth, int lineHeight) {
			this.text = text;
			this.lineWidth = lineWidth;
			this.lineHeight = lineHeight;
		}
		
		public int getLineCount() {
			return (text.length() / lineWidth) + 1;
		}
		
		public int getHeight() {
			return getLineCount() * lineHeight;
		}
		
		public int getLineForVertical(int vertical) {
			
			int line = vertical / lineHeight;
			int lastLine = text.length() / lineWidth;
			
			if ( line < 0 ) {
				return 0;
			} else if ( line > lastLine ) {
				return lastLine;
			}
			
			return line;
		}
		
		public int getLineStart(int line) {
			
			if ( line >= getLineCount() ) {
				return text.length();
			}
			
			return line * lineWidth;
		}
		
		public int getLineEnd(int line) {
			return getLineStart(line + 1);
		}
		
		public int getLineTop( int line ) {
			return line * lineHeight;
		}
		
		public int getLineForOffset( int offset ) {
			return offset / lineWidth;
		}
	}
	
	
	/**
	 * Returns a StaticLayout which delegates most work to a SimpleLayout.
	 * 
	 * @param text the text to layout
	 * @param lineWidth  the number of characters in a line
	 * @param lineHeight height of a line in pixels
	 * @return a mocked StaticLayout
	 */
	public static StaticLayout createMockLayout( final CharSequence text, final int lineWidth, final int lineHeight ) {
		
		final StaticLayout mockLayout = mock(StaticLayout.class);
		final SimpleLayout simpleLayout = new SimpleLayout(text, lineWidth, lineHeight);
		
		when( mockLayout.getLineCount() ).thenReturn( simpleLayout.getLineCount() );
		when( mockLayout.getHeight() ).thenReturn( simpleLayout.getHeight() );	
		
		
		when( mockLayout.getLineForVertical(anyInt())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				Integer vertical = (Integer) invocation.getArguments()[0];
				return simpleLayout.getLineForVertical(vertical);
			}
		});		
		
		when( mockLayout.getLineStart(anyInt())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				Integer line = (Integer) invocation.getArguments()[0];
				return simpleLayout.getLineStart(line);
			}
		});
		
		when( mockLayout.getLineEnd(anyInt())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				int line = (Integer) invocation.getArguments()[0];
				return simpleLayout.getLineEnd(line);
			}
		});
		
		when( mockLayout.getLineForOffset(anyInt())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				int offset = (Integer) invocation.getArguments()[0];
				return simpleLayout.getLineForOffset(offset);
			}
		});
		
		when( mockLayout.getLineTop(anyInt())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				int line = (Integer) invocation.getArguments()[0];
				return simpleLayout.getLineTop(line);
			}
		});
		
		
		return mockLayout;
	}

    public static Spanned getSpanned( final String fromString ) {

        Spanned mockSpanned = Mockito.spy(new SpannedString(fromString));
        when(mockSpanned.subSequence(anyInt(), anyInt())).thenAnswer(new Answer<CharSequence>() {
            @Override
            public CharSequence answer(InvocationOnMock invocation) throws Throwable {
                return fromString.subSequence( (Integer) invocation.getArguments()[0], (Integer) invocation.getArguments()[1]);
            }
        });

        when(mockSpanned.length()).thenReturn(fromString.length());
        when(mockSpanned.toString()).thenReturn(fromString);

        return mockSpanned;
    }


	public static String getStringOfLength( String seed, int length ) {
		StringBuilder builder = new StringBuilder();
		
		int repeat = length / seed.length();
		
		for ( int i=0; i < repeat; i++ ) {
			builder.append(seed);
		}
		
		int remainder = length % seed.length();
		
		builder.append(seed.substring(0, remainder));
		String result = builder.toString();
		Assert.assertEquals( length, result.length());
		
		return result;
	}
	
	
}
