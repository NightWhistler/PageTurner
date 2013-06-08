/*
 * Copyright (C) 2013 Alex Kuiper
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
package net.nightwhistler.pageturner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Pattern PUNCTUATION = Pattern.compile("\\.+\"?'?”?|\\?\"?'?”?|!\"?'?”?|,\"|,'|,”");

    private TextUtil() {}

    /**
     * Processes an input string and enters a newline after full stops,
     * question marks, etc.
     *
     * @param input
     * @return
     */
    public static String splitOnPunctuation(String input) {

        StringBuffer result = new StringBuffer();

        Matcher matcher = PUNCTUATION.matcher(input);

        while (matcher.find()) {

            String match = matcher.group();
            matcher.appendReplacement(result, match + "\n");
        }

        matcher.appendTail(result);
        return result.toString();

    }

}
