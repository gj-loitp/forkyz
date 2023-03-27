package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JPZIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/lat_puzzle_111128.xml");
    }

    public static InputStream getTestPuzzleAcrosticInputStream() {
        return JPZIOTest.class.getResourceAsStream("/acrostic.jpz");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        String ACROSS_CLUES = "Across";
        String DOWN_CLUES = "Down";

        assertEquals("LA Times, Mon, Nov 28, 2011", puz.getTitle());
        assertEquals("Jeff Chen / Ed. Rich Norris", puz.getAuthor());
        assertEquals("© 2011 Tribune Media Services, Inc.", puz.getCopyright());
        assertEquals("Instructions", puz.getIntroMessage());
        assertEquals("Test", puz.getNotes());
        assertEquals(
            "Congratulations<br/>"
                + "<h1>Down</h1>"
                + "<p>22: Shower Heads v7</p>"
                + "<p>61: I'm NOT going to ATTEND it / I'm going to SKIP it</p>",
            puz.getCompletionMessage()
        );

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf(new ClueID(ACROSS_CLUES, 0)));
        assertEquals(true, boxes[0][0].isStartOf(new ClueID(DOWN_CLUES, 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID(ACROSS_CLUES, 1)));

        assertEquals(boxes[0][0].getSolution(), "C");
        assertEquals(boxes[5][14].getSolution(), "Y");
        assertEquals(boxes[14][14].getSolution(), "S");
        assertEquals(boxes[3][6].getSolution(), "N");
        assertTrue(Box.isBlock(boxes[14][5]));
        assertTrue(Box.isBlock(boxes[3][0]));

        assertTrue(boxes[2][2].isBarredTop());
        assertFalse(boxes[3][2].isBarredTop());
        assertTrue(boxes[5][2].isBarredRight());
        assertFalse(boxes[5][2].isBarredLeft());
        assertTrue(boxes[6][2].isBarredBottom());
        assertFalse(boxes[6][2].isBarredLeft());
        assertTrue(boxes[7][2].isBarredLeft());
        assertFalse(boxes[7][2].isBarredRight());

        assertEquals(boxes[2][0].getShape(), Box.Shape.ARROW_LEFT);
        assertFalse(boxes[2][0].isBlock());
        assertEquals(boxes[3][0].getShape(), Box.Shape.ARROW_RIGHT);
        assertTrue(boxes[3][0].isBlock());
        assertEquals(boxes[4][0].getShape(), Box.Shape.ARROW_UP);
        assertTrue(boxes[4][0].isBlock());
        assertEquals(boxes[5][0].getShape(), Box.Shape.ARROW_DOWN);
        assertFalse(boxes[5][0].isBlock());
        assertFalse(boxes[6][0].hasShape());

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        ClueList downClues = puz.getClues(DOWN_CLUES);

        assertEquals(
            acrossClues.getClueByNumber("1").getHint(),
            "Baby bovine (4)"
        );
        assertEquals(
            acrossClues.getClueByNumber("5").getHint(),
            "At the drop of __ (4)"
        );
        assertEquals(
            acrossClues.getClueByNumber("13/18").getHint(),
            "Ice cream-and-cookies brand (4)"
        );
        assertEquals(
            acrossClues.getClueByNumber("23").getHint(),
            "Stat start"
        );
        assertFalse(acrossClues.getClueByNumber("18").hasZone());
        assertEquals(
            downClues.getClueByNumber("6").getHint(),
            "Schmooze, as with the A-list (6)"
        );
        assertEquals(
            downClues.getClueByNumber("7").getHint(),
            "Work like __ (4)"
        );

        String[][] marks = boxes[1][0].getMarks();
        assertEquals(marks[0][0], "TL");
        assertEquals(marks[0][1], "T");
        assertEquals(marks[0][2], "TR");
        assertEquals(marks[1][0], "L");
        assertEquals(marks[1][1], "C");
        assertEquals(marks[1][2], "R");
        assertEquals(marks[2][0], "BL");
        assertEquals(marks[2][1], "B");
        assertEquals(marks[2][2], "BR");
    }

    public static void assertIsTestPuzzleAcrostic(Puzzle puz) {
        String CLUES = "Clues";
        String QUOTES = "Quote";


        Box[][] boxes = puz.getBoxes();

        assertEquals(12, boxes.length);
        assertEquals(33, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isPartOf(new ClueID(CLUES, 17)));
        assertEquals(true, boxes[0][0].isStartOf(new ClueID(QUOTES, 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID(CLUES, 21)));

        assertEquals(boxes[0][0].getSolution(), "N");
        assertTrue(Box.isBlock(boxes[0][4]));
        assertEquals(boxes[1][10].getInitialValue(), ",");
        assertEquals(boxes[1][10].getResponse(), ",");

        ClueList clues = puz.getClues(CLUES);
        ClueList quotes = puz.getClues(QUOTES);

        Clue clueB = clues.getClueByIndex(1);

        assertEquals(clues.size(), 22);
        assertEquals(
            clueB.getHint(),
            "Super hyped to watch a muppet vampire while sick in bed (4, 3, 3, 5)"
        );
        assertFalse(clueB.hasClueNumber());
        assertEquals(clueB.getLabel(), "B");
        assertEquals(quotes.size(), 1);
        assertEquals(
            quotes.getClueByIndex(0).getHint(),
            "Quote"
        );

        String[][] marks = boxes[1][0].getMarks();
        assertNull(marks[0][0]);
        assertEquals(marks[0][2], "R");
    }

    @Test
    public void testJPZ() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(getTestPuzzle1InputStream(), baos);
        Puzzle puz = JPZIO.readPuzzle(
            new ByteArrayInputStream(baos.toByteArray())
        );
        assertIsTestPuzzle1(puz);
    }

    @Test
    public void testJPZAcrostic() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(getTestPuzzleAcrosticInputStream(), baos);
        Puzzle puz = JPZIO.readPuzzle(
            new ByteArrayInputStream(baos.toByteArray())
        );
        assertIsTestPuzzleAcrostic(puz);
    }
}
