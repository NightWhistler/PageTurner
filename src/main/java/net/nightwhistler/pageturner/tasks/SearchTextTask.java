package net.nightwhistler.pageturner.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.handlers.TableHandler;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import nl.siegmann.epublib.domain.Book;

import org.htmlcleaner.TagNode;

import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class SearchTextTask extends AsyncTask<String, SearchTextTask.SearchResult, List<SearchTextTask.SearchResult>> {
	
	private Book book;
	
	private HtmlSpanner spanner;
	
	public SearchTextTask(Book book) {
		this.book = book;
		
		this.spanner = new HtmlSpanner();
		
		DummyHandler dummy = new DummyHandler();
		
        spanner.registerHandler("img", dummy );
        spanner.registerHandler("image", dummy );
        
        spanner.registerHandler("table", new TableHandler() );
	}	
	

	@Override
	protected List<SearchTextTask.SearchResult> doInBackground(String... params) {
		
		String searchTerm = params[0];
		Pattern pattern = Pattern.compile(Pattern.quote((searchTerm)));
		
		List<SearchTextTask.SearchResult> result = new ArrayList<SearchTextTask.SearchResult>();
		
		try {

			PageTurnerSpine spine = new PageTurnerSpine(book);
			
			
			
			for ( int index=0; index < spine.size(); index++ ) {
				
				spine.navigateByIndex(index);
				
				int progress = spine.getProgressPercentage(index, 0);
								
				publishProgress( new SearchResult(null, index, 0, 0, progress) );
				
				Spanned spanned = spanner.fromHtml(spine.getCurrentResource().getReader());				
				Matcher matcher = pattern.matcher(spanned);
				
				while ( matcher.find() ) {
					int from = Math.max(0, matcher.start() - 20 );
					int to = Math.min(spanned.length() -1, matcher.end() + 20 );
					
					if ( isCancelled() ) {
						return null;
					}
					
					String text = "…" + spanned.subSequence(from, to).toString().trim() + "…";
					SearchResult res = new SearchResult(text, index, matcher.start(), matcher.end(),
							spine.getProgressPercentage(index, matcher.start()));
					
					this.publishProgress( res );
					result.add(res);
				}
				
			}
		} catch (IOException io) {
			return null;
		}

		return result;		
	}
	
	public static class SearchResult {
		
		private String display;
		private int index;
		private int start;
		private int end;
		
		private int percentage;
		
		public SearchResult(String display, int index, int offset, int end, int percentage ) {
			this.display = display;
			this.index = index;
			this.start = offset;
			this.end = end;
			this.percentage = percentage;
		}
		
		public String getDisplay() {
			return display;
		}
		
		public int getIndex() {
			return index;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}
		
		public int getPercentage() {
			return percentage;
		}
		
	}
	
	private static class DummyHandler extends TagNodeHandler {
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {

			 builder.append("\uFFFC");
		}
	}
}
