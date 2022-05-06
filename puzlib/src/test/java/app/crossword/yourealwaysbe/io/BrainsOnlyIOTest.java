package app.crossword.yourealwaysbe.io;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Created with IntelliJ IDEA.
 * User: keber_000
 * Date: 2/9/14
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class BrainsOnlyIOTest  extends TestCase {

    public static InputStream getTestPuzzle1InputStream() {
        return BrainsOnlyIOTest.class.getResourceAsStream("/brainsonly.txt");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals("SODA SPEAK", puz.getTitle());
        assertEquals(
            "S.N. &amp; Robert Francis, edited by Stanley Newman",
            puz.getAuthor()
        );

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf("Across"));
        assertEquals(true, boxes[0][0].isStartOf("Down"));
        assertEquals(false, boxes[0][3].isStartOf("Across"));

        assertEquals(boxes[0][0].getSolution(), 'D');
        assertEquals(boxes[5][14].getSolution(), 'Y');
        assertEquals(boxes[14][14].getSolution(), 'P');
        assertEquals(boxes[14][5], null);
        assertEquals(boxes[3][6], null);

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClue("1").getHint(), "Toss out");
        assertEquals(acrossClues.getClue("41").getHint(), "Straighten out");
        assertEquals(downClues.getClue("1").getHint(), "Sancho Panza's mount");
        assertEquals(downClues.getClue("59").getHint(), "Part of pewter");
    }

    public void testParse() throws Exception {
        Puzzle puz = BrainsOnlyIO.parse(getTestPuzzle1InputStream());
        assertIsTestPuzzle1(puz);
    }

    public void testParse2() throws Exception {

        Puzzle puz = BrainsOnlyIO.parse(BrainsOnlyIOTest.class.getResourceAsStream("/brainsonly2.txt"));
        assertEquals(
            puz.getTitle(),
            "OCCUPIED NATIONS: Surrounding the long answers"
        );
        ClueList acrossClues = puz.getClues("Across");
        assertEquals(acrossClues.getClue("15").getHint(), "Elevator guy");
        assertEquals(
            acrossClues.getClue("5").getHint(),
            "Company with a duck mascot"
        );
    }

    public void testParse3() throws Exception {
        try {
            // This was from http://brainsonly.com/servlets-newsday-crossword/newsdaycrossword?date=150903
            BrainsOnlyIO.parse(BrainsOnlyIOTest.class.getResourceAsStream("/brainsonly3.txt"));
        } catch (IOException e) {
            return;
        }
        fail("Expected brainsonly3.txt to fail to parse");
    }
}
