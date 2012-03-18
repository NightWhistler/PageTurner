package net.nightwhistler.pageturner.view;


import java.util.SortedSet;
import java.util.TreeSet;

import net.nightwhistler.pageturner.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class AlphabetBar extends LinearLayout
{
    private static final String TAG = "AlphabetBar";
    
    private SortedSet<Character> alphabet = new TreeSet<Character>();
    
    private AlphabetCallback callback;

    public AlphabetBar(Context context)
    {
        super(context);
        init();
    }

    public AlphabetBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setBackgroundResource(R.drawable.alphabet_bar_bg_dark);
        init();
    }
    
    public void setAlphabet(SortedSet<Character> alphabet ) {
    	this.alphabet = alphabet;
    	updateLabels();
    	invalidate();
    }
    
    public void setCallback(AlphabetCallback callback) {
		this.callback = callback;
	}

    private void updateLabels() {
    	
    	removeAllViews();
    	
    	for ( final Character currentChar: this.alphabet ) {
            
            TextView label = new TextView(getContext());
            label.setText(String.valueOf(currentChar));
            label.setGravity(Gravity.CENTER_VERTICAL);

            label.setClickable(true);
            label.setFocusable(true);
            label.setOnClickListener( new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if ( callback != null ) {
						callback.characterClicked(currentChar);
					}
				}
			});

            addView(label, new LinearLayout.LayoutParams(
              LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }
    
    public void init()
    {
    	setLayoutParams(new LinearLayout.LayoutParams(
    	          LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));

    	        /* Not strictly necessary since we override onLayout and onMeasure with
    	         * our custom logic, but it seems like good form to do this just to
    	         * show how we're arranging the children. */
    	        setOrientation(VERTICAL);        
    }   

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
    }

    
    @Override
    protected void onMeasure(int wSpec, int hSpec)
    {
        Log.d(TAG, "onMeasure(" + wSpec + ", " + hSpec + ")");      

        int count = getChildCount();

        int hMode = MeasureSpec.getMode(hSpec);
        int hSize = MeasureSpec.getSize(hSpec);

        assert hMode == MeasureSpec.EXACTLY;

        int maxWidth = 0;

        int hSizeAdj = hSize - getPaddingTop() - getPaddingBottom(); 
        float childHeight = 0f;
        if ( count > 0 ) {
        	childHeight = hSizeAdj / count;
        }

        /* Calculate how many extra 1-pixel spaces we'll need in order to make
         * childHeight align to integer heights. */
        int variance = hSizeAdj - ((int)childHeight * count);

        int paddingWidth = getPaddingLeft() + getPaddingRight();

        for (int i = 0; i < count; i++)
        {
            TextView label = (TextView)getChildAt(i);

            label.setTextSize(childHeight * 0.2F);

            int thisHeight = (int)childHeight;

            if (variance > 0)
            {
                thisHeight++;
                variance--;
            }

            label.measure
              (MeasureSpec.makeMeasureSpec(13, MeasureSpec.EXACTLY),
               MeasureSpec.makeMeasureSpec(thisHeight, MeasureSpec.EXACTLY));

            maxWidth = Math.max(maxWidth, label.getMeasuredWidth());
        }

        maxWidth += paddingWidth;

        setMeasuredDimension(resolveSize(maxWidth, wSpec), hSize); 
    }   
    
    public static interface AlphabetCallback {
    	public void characterClicked( Character c );
    }
}

