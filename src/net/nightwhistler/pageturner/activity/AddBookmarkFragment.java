/*
 * Copyright (C) 2013 Alex Kuiper, Rob Hoelz
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

import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockDialogFragment;
import net.nightwhistler.pageturner.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AddBookmarkHandler
	implements TextView.OnEditorActionListener, View.OnClickListener {

	private Dialog dialog;
	private int bookIndex;
	private int bookPosition;
	private TextView textView;

	private static final Logger LOG = LoggerFactory
		    .getLogger(AddBookmarkFragment.class);

	AddBookmarkHandler(Dialog dialog, int bookIndex, int bookPosition, TextView textView)
	{
		this.dialog       = dialog;
		this.bookIndex    = bookIndex;
		this.bookPosition = bookPosition;
		this.textView     = textView;
	}

	private void handleAction()
	{
	    dialog.dismiss();
	    LOG.info("    >>> Creating bookmark: " + textView.getText());
	    LOG.info("    >>> at index:    " + bookIndex);
	    LOG.info("    >>> at position: " + bookPosition);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
	{
		if(actionId == EditorInfo.IME_ACTION_DONE) {
			handleAction();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v)
	{
		handleAction();
	}
}

public class AddBookmarkFragment extends RoboSherlockDialogFragment
{
	private int bookIndex;
	private int bookPosition;
	private static final Logger LOG = LoggerFactory
		.getLogger(AddBookmarkFragment.class);

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Dialog dialog = getDialog();
		dialog.setTitle(R.string.add_bookmark);

		View v                     = inflater.inflate(R.layout.fragment_add_bookmark, container, false);
		EditText text              = (EditText) v.findViewById(R.id.bookmark_name);
		Button addButton           = (Button) v.findViewById(R.id.add_bookmark_button);
		AddBookmarkHandler handler = new AddBookmarkHandler(dialog, bookIndex, bookPosition, text);

		text.setOnEditorActionListener(handler);
		addButton.setOnClickListener(handler);

		return v;
	}

	public void setBookPosition(int bookPosition)
	{
		this.bookPosition = bookPosition;
	}

	public void setBookIndex(int bookIndex)
	{
		this.bookIndex = bookIndex;
	}
}
