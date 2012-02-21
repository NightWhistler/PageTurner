package net.nightwhistler.pageturner.activity;

import net.nightwhistler.pageturner.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;

public class Dialogs {
	
	public static void showAboutDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.about);
		builder.setIcon(R.drawable.page_turner);

		String version = "";
		try {
			version = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// Huh? Really?
		}

		String html = "<h2>" + context.getString(R.string.app_name) + " " +  version + "</h2>";
		html += context.getString(R.string.about_gpl);
		html += "<br/><a href='http://pageturner-reader.org'>http://pageturner-reader.org</a>";

		builder.setMessage( Html.fromHtml(html));

		builder.setNeutralButton(context.getString(android.R.string.ok), 
				new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();				
			}
		});

		builder.show();
	}

}
