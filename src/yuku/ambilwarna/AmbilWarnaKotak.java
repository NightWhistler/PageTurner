package yuku.ambilwarna;

import android.content.*;
import android.graphics.*;
import android.graphics.Shader.TileMode;
import android.util.*;
import android.view.*;

public class AmbilWarnaKotak extends View {
	Paint paint;
	Shader luar;
	final float[] color = { 1.f, 1.f, 1.f };

	public AmbilWarnaKotak(Context context) {
		this(context, null);
	}

	public AmbilWarnaKotak(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AmbilWarnaKotak(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (paint == null) {
			paint = new Paint();
			luar = new LinearGradient(0.f, 0.f, 0.f, this.getMeasuredHeight(), 0xffffffff, 0xff000000, TileMode.CLAMP);
		}
		int rgb = Color.HSVToColor(color);
		Shader dalam = new LinearGradient(0.f, 0.f, this.getMeasuredWidth(), 0.f, 0xffffffff, rgb, TileMode.CLAMP);
		ComposeShader shader = new ComposeShader(luar, dalam, PorterDuff.Mode.MULTIPLY);
		paint.setShader(shader);
		canvas.drawRect(0.f, 0.f, this.getMeasuredWidth(), this.getMeasuredHeight(), paint);
	}

	void setHue(float hue) {
		color[0] = hue;
		invalidate();
	}
}
