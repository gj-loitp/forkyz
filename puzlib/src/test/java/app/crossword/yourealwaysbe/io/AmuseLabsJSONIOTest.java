
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmuseLabsJSONIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/amuselabs.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test Amuse Labs");
        assertEquals(puz.getAuthor(), "Test Author");
        assertEquals(puz.getCopyright(), "Test Copyright");
        assertEquals(puz.getCompletionMessage(), "End Message");
        assertEquals(puz.getDate(), LocalDate.of(2021,8,4));

        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[0][1].getClueNumber(), "2");
        assertTrue(Box.isBlock(boxes[0][4]));
        assertEquals(boxes[5][5].getClueNumber(), "28");
        assertTrue(Box.isBlock(boxes[5][7]));

        assertEquals(boxes[0][0].getSolution(), "A");
        assertEquals(boxes[5][3].getSolution(), "B");

        assertEquals(boxes[10][3].getShape(), Box.Shape.CIRCLE);
        assertEquals(boxes[7][6].getShape(), Box.Shape.CIRCLE);
        assertFalse(boxes[3][7].hasShape());
        assertFalse(boxes[5][9].hasShape());

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Clue 1a");
        assertEquals(acrossClues.getClueByNumber("21").getHint(), "Clue 21a");
        assertEquals(downClues.getClueByNumber("1").getHint(), "Clue 1d");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Clue 2d");
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = AmuseLabsJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

