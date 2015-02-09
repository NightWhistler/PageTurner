package net.nightwhistler.pageturner;

import org.junit.Assert;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
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

                (TextUtil.splitOnPunctuation(input), equalTo(asList("aa.", "bb.", "cc.")));
    }

    @Test
    public void testSplitOnDotDotDot() {
        String input = "aa.bb...cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), equalTo(asList("aa.", "bb...", "cc.")));
    }

    @Test
    public void testSplitOnQuestionMark() {
        String input = "aa.bb?cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), equalTo(asList("aa.", "bb?", "cc.")));
    }

    @Test
    public void testSplitOnExclamationMark() {
        String input = "aa.bb!cc.";

        assertThat

                (TextUtil.splitOnPunctuation(input), equalTo(asList("aa.", "bb!", "cc.")));

    }

    @Test
    public void testQuotedText() {
        String input = "'aabbcc.'";

        assertThat

                (TextUtil.splitOnPunctuation(input), equalTo(asList("'aabbcc.'")));
    }

    @Test
    public void testQuotedComma() {
        String input = "'aabbcc,'ccc";

        assertThat
                (TextUtil.splitOnPunctuation(input), equalTo(asList("'aabbcc,'", "ccc")));
    }

    @Test
    public void testQuotedUnicode() {
        String input = "“aabbcc.”";

        assertThat

                (TextUtil.splitOnPunctuation(input), equalTo(asList("“aabbcc.”")));
    }

    @Test
    public void testDotDotDot() {
        String input = "“aabb. . . CC";

        assertThat

                (TextUtil.splitOnPunctuation(input), equalTo(asList("“aabb. . .", " CC")));
    }

    @Test
    public void testDoctor() {
        String input = "Hello, Dr. Wilson, I have been expecting you.";

        assertThat
                (TextUtil.splitOnPunctuation(input), equalTo(
                        asList("Hello, Dr. Wilson, I have been expecting you.")
                ));

    }

    @Test
    public void testExtraQuotes() {
        String input = "“It’s a start,” Arkady said. “But there are aspects of that treaty you haven’t mentioned.” ‘";

        assertThat
                (TextUtil.splitOnPunctuation(input), equalTo(
                        asList("“It’s a start,”", " Arkady said.", " “But there are aspects of that treaty you haven’t mentioned.”",
                                " ‘")
                ));

    }

    @Test
    public void testSofie() {
        String input = "‘Tja,’ zei ze. ‘Soms wel.’\n" +
                "‘Soms? Ik bedoel, vind je het eigenlijk niet vreemd dat er een wereld bestaat?’\n" +
                "‘Maar Sofie, zo moet je niet praten.’";

        assertThat
                (TextUtil.splitOnPunctuation(input), equalTo(
                        asList(
                                "‘Tja,’", " zei ze.", " ‘Soms wel.’",
                                "‘Soms? Ik bedoel, vind je het eigenlijk niet vreemd dat er een wereld bestaat?’",
                                "‘Maar Sofie, zo moet je niet praten.’"
                                )
                ));

    }


}
