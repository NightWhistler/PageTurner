package net.nightwhistler.pageturner.view;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.html.CleanHtmlParser;
import net.nightwhistler.pageturner.html.TagNodeHandler;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;

public class TableHandler extends TagNodeHandler {
	
	Logger LOG = LoggerFactory.getLogger(TableHandler.class);

	private int tableWidth = 400;	
	private Typeface typeFace = Typeface.DEFAULT;
	private float textSize = 16f;
	private int textColor = Color.BLACK;
	private int backgroundColor = Color.WHITE;
		
	private static final int PADDING = 5;
	
	private CleanHtmlParser parser;
	
	public TableHandler(CleanHtmlParser parser) {
		this.parser = parser;
	}
	
	public void setTableWidth(int tableWidth) {
		this.tableWidth = tableWidth;
	}
	
	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	
	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}
	
	public void setTextSize(float textSize) {
		this.textSize = textSize;
	}
	
	public void setTypeFace(Typeface typeFace) {
		this.typeFace = typeFace;
	}
	
	@Override
	public boolean rendersContent() {
		return true;
	}
	
	private void readNode( Object node, Table table ) {
		
		//We can't handle plain content nodes within the table.
		if ( node instanceof ContentNode ) {			
			return;
		}
		
		TagNode tagNode = (TagNode) node;
		
		if ( tagNode.getName().equals("td") ) {
			Spanned result = this.parser.fromTagNode( tagNode );
			table.addCell(result);
			return;
		}
		
		if ( tagNode.getName().equals("tr") ) {
			table.addRow();
		}
		
		for ( Object child: tagNode.getChildren() ) {
			readNode(child, table);
		}
		
	}
		
	private Table getTable( TagNode node ) {
	
		Table result = new Table();
		
		readNode(node, result);		
		
		return result;
	}	
	
	private TextPaint getTextPaint() {
		TextPaint textPaint = new TextPaint();
		textPaint.setColor( this.textColor );
		textPaint.setAntiAlias(true);
		textPaint.setTextSize( this.textSize );
		textPaint.setTypeface( this.typeFace );
		
		return textPaint;
	}	
	
	private int calculateRowHeight( List<Spanned> row ) {
		
		TextPaint textPaint = getTextPaint();
		
		int columnWidth = tableWidth / row.size();

		int rowHeight = 0;

		for ( Spanned cell: row ) {

			StaticLayout layout = new StaticLayout(cell, textPaint,
					columnWidth - 2*PADDING,
					Alignment.ALIGN_NORMAL, 1f, 0f, true);			

			if ( layout.getHeight() > rowHeight ) {
				rowHeight = layout.getHeight();
			}
		}	
		
		return rowHeight;		
	}
	
	@Override
	public void handleTagNode(TagNode node, SpannableStringBuilder builder,
			int start, int end) {	
        		
		Table table = getTable(node);
		
		LOG.debug("Table has " + table.getRows().size() + " rows and " +
				table.getFirstRow().size() + " columns" );
		
		for (int i=0; i < table.getRows().size(); i++ ) {
			
			List<Spanned> row = table.getRows().get(i);
			builder.append("\uFFFC");
			
			TableRowDrawable drawable = new TableRowDrawable(row );
			drawable.setBounds( 0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight() );

			builder.setSpan( new ImageSpan(drawable), start, start + 1, 
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			builder.setSpan(new AlignmentSpan() {
				@Override
				public Alignment getAlignment() {
					return Alignment.ALIGN_CENTER;
				}
			}, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			start++;
		}		
	}
	
	private class TableRowDrawable extends Drawable {
		
		private List<Spanned> tableRow;

		public TableRowDrawable(List<Spanned> tableRow) {
			this.tableRow = tableRow;			
		}
		
		
		@Override
		public void draw(Canvas canvas) {
			Paint paint = new Paint();
			paint.setColor( textColor );
			paint.setStyle(Style.STROKE);
			
			int numberOfColumns = tableRow.size();
			int columnWidth = tableWidth / numberOfColumns;		
			int rowHeight = calculateRowHeight(tableRow);
			
			int offset = 0;
			
			for ( int i=0; i < numberOfColumns; i++ ) {
				
				offset = i * columnWidth;
				
				//The rect is open at the bottom, so there's a single line between rows.
				canvas.drawRect(offset, 0, offset + columnWidth, rowHeight, paint);
				
				StaticLayout layout = new StaticLayout(tableRow.get(i), getTextPaint(),
						(columnWidth - 2*PADDING), Alignment.ALIGN_NORMAL, 1f, 0f, true);			
				
				canvas.translate(offset + PADDING, 0);				
				layout.draw(canvas);				
				canvas.translate( -1 * PADDING, 0);
				
			}			
		}
		
		
		@Override
		public int getIntrinsicHeight() {
			return calculateRowHeight(tableRow);
		}
		
		@Override
		public int getIntrinsicWidth() {
			return tableWidth;
		}
		
		@Override
		public int getOpacity() {
			return PixelFormat.OPAQUE;
		}
		
		@Override
		public void setAlpha(int alpha) {
						
		}
		
		@Override
		public void setColorFilter(ColorFilter cf) {
					
		}
	}
	
	private class Table {
		private List<List<Spanned>> content = new ArrayList<List<Spanned>>();
		
		public void addRow() {
			content.add( new ArrayList<Spanned>() );
		}
		
		public List<Spanned> getBottomRow() {
			return content.get( content.size() -1 );
		}
		
		public List<Spanned> getFirstRow() {
			return content.get(0);
		}		
		
		public List<List<Spanned>> getRows() {
			return content;
		}
		
		public void addCell(Spanned text) {
			if ( content.isEmpty() ) {
				throw new IllegalStateException("No rows added yet");
			}
			
			getBottomRow().add( text );			
		}
	}
	
	
	
}
