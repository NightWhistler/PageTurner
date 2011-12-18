package net.nightwhistler.pageturner.view;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.html.CleanHtmlParser;
import net.nightwhistler.pageturner.html.TagNodeHandler;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;

public class TableHandler extends TagNodeHandler {

	private int tableWidth = 200;
	private int tableHeight = 500;
	private Typeface typeFace = Typeface.DEFAULT;
	private float textSize = 16f;
	private int textColor = Color.BLACK;
	private int backgroundColor = Color.WHITE;
		
	private Context context;
	private CleanHtmlParser parser;
	
	public TableHandler(Context context, CleanHtmlParser parser) {
		this.context = context;
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
		
		if ( node instanceof ContentNode ) {
			table.addCell( new SpannedString( ( (ContentNode) node).getContent() ));
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
	
	
	private Bitmap render(Table table) {
		
		
		return null;		
	}
	
	private TextPaint getTextPaint() {
		TextPaint textPaint = new TextPaint();
		textPaint.setColor( this.textColor );
		textPaint.setAntiAlias(true);
		textPaint.setTextSize( this.textSize );
		textPaint.setTypeface( this.typeFace );
		
		return textPaint;
	}	
	
	private int calculateTableHeight( Table table ) {
		
		TextPaint textPaint = getTextPaint();
		
		int tableHeight = 0;
		
		if ( ! table.isEmpty() ) {
			
			int columnCount = table.getFirstRow().size();			
			int columnWidth = tableWidth / columnCount;
			
			for ( List<Spanned> row: table.getRows() ) {
				
				int rowHeight = 0;
				
				for ( Spanned cell: row ) {
					
					StaticLayout layout = new StaticLayout(cell, textPaint,
							columnWidth, Alignment.ALIGN_NORMAL, 1f, 0f, false);
					
					if ( layout.getHeight() > rowHeight ) {
						rowHeight = layout.getHeight();
					}
				}	
				
				tableHeight += rowHeight;
			}			
		}
		
		return tableHeight;		
	}
	
	@Override
	public void handleTagNode(TagNode node, SpannableStringBuilder builder,
			int start, int end) {
		
		builder.append("\uFFFC");
        
		Table table = getTable(node);
		Bitmap bitmap = render(table);
		
		BitmapDrawable drawable = new BitmapDrawable( bitmap );
		drawable.setBounds( 0, 0, bitmap.getWidth(), bitmap.getHeight() );
		
		
		builder.setSpan( new ImageSpan(drawable), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.setSpan(new AlignmentSpan() {
			@Override
			public Alignment getAlignment() {
				return Alignment.ALIGN_CENTER;
			}
		}, start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	
				
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
		
		public boolean isEmpty() {
			return content.isEmpty();
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
