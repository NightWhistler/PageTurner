package net.nightwhistler.pageturner;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/17/13
 * Time: 7:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class TextUtil {

    private static final Pattern PUNCTUATION = Pattern.compile("\\.+\"?'?|\\?\"?'?|!\"?'?");

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
