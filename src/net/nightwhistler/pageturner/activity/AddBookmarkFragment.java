/*
 * Copyright (C) 2013 Alex Kuiper, Rob Hoelz
 * 
 * This file is part of PageTurner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nightwhistler.pageturner.activity;

import android.app.Dialog;
import android.content.Context;
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
import net.nightwhistler.pageturner.bookmark.Bookmark;
import net.nightwhistler.pageturner.bookmark.BookmarkDatabaseHelper;
import net.nightwhistler.pageturner.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AddBookmarkFragment extends RoboSherlockDialogFragment {

    private String filename;
    private int bookIndex;
    private int bookPosition;

    private String initialText;

    private BookmarkDatabaseHelper bookmarkDatabaseHelper;

    private static final Logger LOG = LoggerFactory
            .getLogger(AddBookmarkFragment.class);

    public AddBookmarkFragment(String filename, BookmarkDatabaseHelper helper ) {
        this.filename = filename;
        this.bookmarkDatabaseHelper = helper;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Dialog dialog = getDialog();
        dialog.setTitle(R.string.add_bookmark);

        View v = inflater.inflate(R.layout.fragment_add_bookmark, container, false);
        EditText text = (EditText) v.findViewById(R.id.bookmark_name);
        text.setText( this.initialText );

        Button addButton = (Button) v.findViewById(R.id.add_bookmark_button);
        AddBookmarkHandler handler = new AddBookmarkHandler(getActivity(), dialog,
                filename, bookIndex, bookPosition, text);

        text.setOnEditorActionListener(handler);
        addButton.setOnClickListener(handler);

        return v;
    }

    public void setBookPosition(int bookPosition) {
        this.bookPosition = bookPosition;
    }

    public void setBookIndex(int bookIndex) {
        this.bookIndex = bookIndex;
    }

    public void setInitialText( String text ) {
        this.initialText = text;
    }

    private class AddBookmarkHandler
            implements TextView.OnEditorActionListener, View.OnClickListener {

        private Dialog dialog;
        private String filename;
        private int bookIndex;
        private int bookPosition;
        private TextView textView;

        private final Logger LOG = LoggerFactory
                .getLogger(AddBookmarkFragment.class);

        AddBookmarkHandler(Context context, Dialog dialog, String filename,
                           int bookIndex, int bookPosition, TextView textView) {

            this.dialog = dialog;
            this.filename = filename;
            this.bookIndex = bookIndex;
            this.bookPosition = bookPosition;
            this.textView = textView;
        }

        private void handleAction() {
            dialog.dismiss();
            LOG.debug("    >>> Creating bookmark: " + textView.getText());
            LOG.debug("    >>> for file:    " + filename);
            LOG.debug("    >>> at index:    " + bookIndex);
            LOG.debug("    >>> at position: " + bookPosition);

            bookmarkDatabaseHelper.addBookmark(
                    new Bookmark( filename, textView.getText().toString(),
                            bookIndex, bookPosition));
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleAction();
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            handleAction();
        }
    }

}