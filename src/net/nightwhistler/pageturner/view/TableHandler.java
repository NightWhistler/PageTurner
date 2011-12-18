package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.html.TagNodeHandler;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.util.Config;
import android.view.ViewGroup.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class TableHandler extends TagNodeHandler {

	private int tableWidth = 200;
	private int tableHeight = 500;
	private Typeface typeFace = Typeface.DEFAULT;
	private float textSize = 16f;
	private int textColor = Color.BLACK;
	private int backgroundColor = Color.WHITE;
		
	private Context context;
	
	public TableHandler(Context context) {
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
	
		
	private void buildLayout( TableLayout layout, TableRow currentRow, Object node ) {
		if ( node instanceof ContentNode && currentRow != null) {
			ContentNode cn = (ContentNode) node;
			TextView labelTV = new TextView(context);
            labelTV.setText(cn.getContent());
            labelTV.setTextColor(this.textColor);
            labelTV.setTypeface(typeFace);
            labelTV.setTextSize(textSize);
            
            labelTV.setBackgroundColor(this.backgroundColor);
            labelTV.setLayoutParams(new LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
            
            currentRow.addView(labelTV);
            
            return;
		} 
		
		TagNode tagNode = (TagNode) node;
		
		if ( tagNode.getName().equals("tr") ) {
			TableRow newRow = new TableRow(context);
			layout.addView(newRow, new TableLayout.LayoutParams(
	                    LayoutParams.FILL_PARENT,
	                    LayoutParams.WRAP_CONTENT));
			
			currentRow = newRow;
		}
		
		for ( Object child: tagNode.getChildren() ) {
			buildLayout(layout, currentRow, child);
		}
	}
	
	private Bitmap render(TagNode node) {
		
		TableLayout table = new TableLayout(context);
		table.setLayoutParams( new LayoutParams(this.tableWidth, LayoutParams.WRAP_CONTENT) );
		
		buildLayout(table, null, node);		
		
		//table.measure(tableWidth, 1024);
		//int height = table.getMeasuredHeight();
		//int width = table.getMeasuredWidth();
		
		int width = tableWidth;
		int height = 1024;
		
		//Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888 );
		
		table.layout(0, 0, width, height );		
		
		
		table.buildDrawingCache(false);
		Bitmap drawingCache = table.getDrawingCache();		
	  		
		if ( drawingCache != null ) {					
			Bitmap copy = drawingCache.copy(drawingCache.getConfig(), false);
			table.destroyDrawingCache();			
			return copy;
		}
		
		
		//table.draw(new Canvas(result));
		
		
		return null;		
	}
	
	@Override
	public void handleTagNode(TagNode node, SpannableStringBuilder builder,
			int start, int end) {
		
		builder.append("\uFFFC");
        
		Bitmap bitmap = render(node);
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
	
}
