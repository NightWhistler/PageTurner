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

import jedi.functional.FunctionalPrimitives;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static jedi.functional.FunctionalPrimitives.select;

public class TextUtil {

    private static final Pattern PUNCTUATION = Pattern.compile("\\.( ?\\.)*[\"'”’]?|[\\?!] ?[\"'”’]?|, ?[\"'”’]|”");

    /*
    These are titles like Mr. Mrs., etc that will often cause incorrect
    breaks in English text. We filter them out.
     */
    private static final String[] TITLES = { "mr", "mrs", "dr", "ms", "st" };

    private TextUtil() {}

    /**
     * Processes an input string and enters a newline after full stops,
     * question marks, etc.
     *
     * @param input
     * @return
     */
    public static List<String> splitOnPunctuation(String input) {

        StringBuffer stringBuffer = new StringBuffer();

        Matcher matcher = PUNCTUATION.matcher(input);

        int previousMatch = 0;

        while (matcher.find()) {

            String match = matcher.group();
            int startIndex = matcher.start();

            String subString = input.substring(previousMatch, startIndex );

            boolean shouldReplace = true;

            for ( String title: TITLES ) {
                if ( subString.toLowerCase().endsWith(title)) {
                    shouldReplace = false;
                }
            }

            if ( subString.trim().length() == 1 ) {
                shouldReplace = false;
            }

            String replacement;

            if ( shouldReplace ) {
                replacement = match + "\n";
            } else {
                replacement = match;
            }

            matcher.appendReplacement(stringBuffer, replacement);
            previousMatch = startIndex;
        }

        matcher.appendTail(stringBuffer);

        return select(asList(stringBuffer.toString().split("\n")), s -> s.length() > 0 );
    }

    public static String shortenText( String original ) {

        String text = original;

        if ( text.length() > 40 ) {
            text = text.substring(0, 40) + "…";
        }

        return text;
    }


}
