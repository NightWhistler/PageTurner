package com.revive;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
//import com.markupartist.android.widget.ActionBar;
import android.view.Window;
import com.markupartist.android.widget.ActionBar;
import com.revive.R;
import net.nightwhistler.pageturner.activity.LibraryActivity;
//import net.nightwhistler.pageturner.R;

/**
 * Created by IntelliJ IDEA.
 * User: achachiez
 * Date: 1/28/12
 * Time: 11:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class bookshop extends Activity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bookshop);
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle("Revive Bookshop");
        actionBar.addAction(new ActionBar.IntentAction(this, review(),
                R.drawable.book_refresh));
        actionBar.setHomeAction(new ActionBar.IntentAction(this, home(),
                com.revive.R.drawable.book_refresh));
    }
    private Intent review() {
        final Intent intent = new Intent();
        intent.setClass(this, review.class);
        // intent.putExtra(Intent.EXTRA_TEXT, "Shared from the ActionBar widget.");
        return (intent);
    }

    private Intent home() {
        final Intent intent = new Intent();
        intent.setClass(this, LibraryActivity.class);
        // intent.putExtra(Intent.EXTRA_TEXT, "Shared from the ActionBar widget.");
        return (intent);
    }
}