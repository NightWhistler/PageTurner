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

public class Bookmark {

    private String fileName;
    private String name;
    private int index;
    private int position;

    public Bookmark(String fileName, String name, int index, int position) {
        this.fileName = fileName;
        this.name = name;
        this.index = index;
        this.position = position;
    }

    void populateContentValues(ContentValues row) {
        row.put("file_name", this.fileName);
        row.put("name", this.name);
        row.put("book_index", this.index);
        row.put("book_position", this.position);
    }

    public String getFileName() {
        return fileName;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getPosition() {
        return position;
    }
}
