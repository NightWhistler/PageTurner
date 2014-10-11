package net.nightwhistler.pageturner;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/17/13
 * Time: 7:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class TextUtilTest {

    @Test
    public void testSplitOnFullStop() {
         String input = "aa.bb.cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("aa.\nbb.\ncc.\n"));
    }

    @Test
    public void testSplitOnDotDotDot() {
        String input = "aa.bb...cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("aa.\nbb...\ncc.\n"));
    }

    @Test
    public void testSplitOnQuestionMark() {
        String input = "aa.bb?cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("aa.\nbb?\ncc.\n"));
    }

    @Test
    public void testSplitOnExclamationMark() {
        String input = "aa.bb!cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("aa.\nbb!\ncc.\n"));

    }

    @Test
    public void testQuotedText() {
        String input = "'aabbcc.'";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("'aabbcc.'\n"));
    }

    @Test
    public void testQuotedComma() {
        String input = "'aabbcc,'ccc";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("'aabbcc,'\nccc"));
    }

    @Test
    public void testQuotedUnicode() {
        String input = "“aabbcc.”";

        assertThat

                (TextUtil.splitOnPunctuation(input), is("“aabbcc.”\n"));
    }


}
