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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockDialogFragment;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.bookmark.Bookmark;
import net.nightwhistler.pageturner.bookmark.BookmarkDatabaseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AddBookmarkFragment extends RoboSherlockDialogFragment {

    private String filename;
    private int bookIndex;
    private int bookPosition;

    private String initialText;

    private BookmarkDatabaseHelper bookmarkDatabaseHelper;

    private EditText inputField;

    private static final Logger LOG = LoggerFactory
            .getLogger(AddBookmarkFragment.class);


    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setBookmarkDatabaseHelper(BookmarkDatabaseHelper bookmarkDatabaseHelper) {
        this.bookmarkDatabaseHelper = bookmarkDatabaseHelper;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_add_bookmark, null);

        this.inputField = (EditText) view.findViewById(R.id.bookmark_name);
        this.inputField.setText(this.initialText);

        AddBookmarkHandler handler = new AddBookmarkHandler();
        inputField.setOnEditorActionListener(handler);


        return new AlertDialog.Builder(
                getActivity())
                .setTitle(R.string.add_bookmark)
                .setView( view )
                .setPositiveButton(R.string.add,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                handleAction();
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create();
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

    public void handleAction() {

        LOG.debug("    >>> Creating bookmark: " + inputField.getText());
        LOG.debug("    >>> for file:    " + filename);
        LOG.debug("    >>> at index:    " + bookIndex);
        LOG.debug("    >>> at position: " + bookPosition);

        bookmarkDatabaseHelper.addBookmark(
                new Bookmark( filename, inputField.getText().toString(),
                        bookIndex, bookPosition));
    }

    private class AddBookmarkHandler
            implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleAction();
                return true;
            }
            return false;
        }

    }

}