package net.nightwhistler.pageturner.view;


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

    public AlphabetBar(Context context)
    {
        super(context);
        init();
    }

    public AlphabetBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public void init()
    {
        setLayoutParams(new LinearLayout.LayoutParams(
          LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));

        /* Not strictly necessary since we override onLayout and onMeasure with
         * our custom logic, but it seems like good form to do this just to
         * show how we're arranging the children. */
        setOrientation(VERTICAL);

        char[] labels =
        { 
            '#',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        };

        for (int i = 0; i < labels.length; i++)
        {
            TextView label = new TextView(getContext());
            label.setText(String.valueOf(labels[i]));
//          label.setAlignment(Alignment.ALIGN_CENTER);
            label.setGravity(Gravity.CENTER_VERTICAL);

            label.setClickable(true);
            label.setFocusable(true);
            label.setOnClickListener(mLabelClicked);

            addView(label, new LinearLayout.LayoutParams(
              LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }

    private OnClickListener mLabelClicked = new OnClickListener()
    {
        public void onClick(View v)
        {
            if (mClickCallback != null)
                mClickCallback.onClick(v);
        }
    };

    private OnClickListener mClickCallback = null;

    /**
     * Set the click listener for alphabet labels.
     *  
     * @param listener
     *   Click listener, or null to unset.
     */
    public void setLabelClickListener(OnClickListener listener)
    {
        mClickCallback = listener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
    }

    private void useDefaultBackground()
    {
        setBackgroundResource(R.drawable.alphabet_bar_bg);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec)
    {
        Log.d(TAG, "onMeasure(" + wSpec + ", " + hSpec + ")");

        if (getBackground() == null)
            useDefaultBackground();

        int count = getChildCount();

        int hMode = MeasureSpec.getMode(hSpec);
        int hSize = MeasureSpec.getSize(hSpec);

        assert hMode == MeasureSpec.EXACTLY;

        int maxWidth = 0;

        int hSizeAdj = hSize - getPaddingTop() - getPaddingBottom(); 
        float childHeight = hSizeAdj / count;

        /* Calculate how many extra 1-pixel spaces we'll need in order to make
         * childHeight align to integer heights. */
        int variance = hSizeAdj - ((int)childHeight * count);

        int paddingWidth = getPaddingLeft() + getPaddingRight();

        for (int i = 0; i < count; i++)
        {
            TextView label = (TextView)getChildAt(i);

            label.setTextSize(childHeight * 0.5F);

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
}

