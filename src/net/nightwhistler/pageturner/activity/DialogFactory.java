/*
 * Copyright (C) 2012 Alex Kuiper
 * 
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.nightwhistler.pageturner.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.R;

public class DialogFactory {

    @Inject
    private Context context;

    public static interface SearchCallBack {
        void performSearch(String query);
    }

    public void showSearchDialog( int titleId, int questionId, final SearchCallBack callBack ) {

        final AlertDialog.Builder searchInputDialogBuilder = new AlertDialog.Builder(context);

        searchInputDialogBuilder.setTitle(titleId);
        searchInputDialogBuilder.setMessage(questionId);

        // Set an EditText view to get user input
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInputDialogBuilder.setView(input);


        searchInputDialogBuilder.setPositiveButton(android.R.string.search_go,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        callBack.performSearch(input.getText().toString());
                    }
                });

        searchInputDialogBuilder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

        final AlertDialog searchInputDialog = searchInputDialogBuilder.show();

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (event == null) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        callBack.performSearch(input.getText().toString());
                        searchInputDialog.dismiss();
                        return true;
                    }
                } else if (actionId == EditorInfo.IME_NULL) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        callBack.performSearch(input.getText().toString());
                        searchInputDialog.dismiss();
                    }

                    return true;
                }

                return false;
            }
        });
    }

	public AlertDialog buildAboutDialog() {
		
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

        return builder.create();
	}

}
