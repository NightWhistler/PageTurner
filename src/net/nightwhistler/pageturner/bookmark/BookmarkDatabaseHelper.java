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
package net.nightwhistler.pageturner.bookmark;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.ArrayList;

@Singleton
public class BookmarkDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "PageTurnerBookmarks";
    private static final String TABLE_NAME = "bookmarks";
    private static final int VERSION = 1;

    private SQLiteDatabase database;

    public enum Field {
        file_name("TEXT NOT NULL"),
        name("TEXT NOT NULL"),
        book_index("INTEGER NOT NULL"),
        book_position("INTEGER NOT NULL");

        public String fieldDef;

        private Field(String fieldDef) {
            this.fieldDef = fieldDef;
        }
    }

    @Inject
    public BookmarkDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    private String getCreateTableString() {
        String create = "CREATE TABLE " + TABLE_NAME + " (";
        boolean isFirst = true;

        for (Field f : Field.values()) {
            if (isFirst) {
                isFirst = false;
            } else {
                create += ",";
            }

            create += " " + f.name() + " " + f.fieldDef;
        }

        create += " );";

        return create;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateTableString());
        db.execSQL("CREATE UNIQUE INDEX fn_name_index ON " + TABLE_NAME + "(" + Field.file_name + ", "
                + Field.book_index + ", " + Field.book_position + ");");


        for ( int i=0; i < 10000; i++ ) {

            Bookmark bm = new Bookmark( "fake_file", Integer.toHexString( i ), i % 3, i );

            ContentValues row = new ContentValues();

            bm.populateContentValues(row);

            db.insert(TABLE_NAME, null, row);
        }

    }

    public void deleteBookmark( Bookmark bookmark ) {
        getDataBase().delete( TABLE_NAME,
                "file_name = ? and book_index = ? and book_position = ?",
                array(
                        bookmark.getFileName(),
                        Integer.toString(bookmark.getIndex()),
                        Integer.toString(bookmark.getPosition() )
                )
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException();
    }

    private synchronized SQLiteDatabase getDataBase() {
        if (this.database == null || !this.database.isOpen()) {
            this.database = getWritableDatabase();
        }

        return this.database;
    }

    public void addBookmark(Bookmark bm) {
        ContentValues row = new ContentValues();

        bm.populateContentValues(row);

        getDataBase().insert(TABLE_NAME, null, row);
    }

    private static String[] array( String... items ) {
        return items;
    }

    public List<Bookmark> getBookmarksForFile(String fileName) {

        if ( fileName == null ) {
            return new ArrayList<Bookmark>();
        }

        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        Cursor cursor = getDataBase().query(false, TABLE_NAME, null,
                "file_name = ?", new String[]{fileName}, null, null,
                "book_index, book_position ASC", null);

        int fileNameIndex = cursor.getColumnIndex(Field.file_name.name());
        int nameIndex = cursor.getColumnIndex(Field.name.name());
        int indexIndex = cursor.getColumnIndex(Field.book_index.name());
        int positionIndex = cursor.getColumnIndex(Field.book_position.name());

        while (cursor.moveToNext()) {
            bookmarks.add(new Bookmark(
                    cursor.getString(fileNameIndex),
                    cursor.getString(nameIndex),
                    cursor.getInt(indexIndex),
                    cursor.getInt(positionIndex)));
        }
        cursor.close();

        return bookmarks;
    }
}
