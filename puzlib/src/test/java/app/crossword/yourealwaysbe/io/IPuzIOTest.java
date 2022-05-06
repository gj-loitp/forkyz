package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

public class IPuzIOTest extends TestCase {

    public IPuzIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return IPuzIOTest.class.getResourceAsStream("/test.ipuz");
    }

    public static InputStream getTestPuzzle2InputStream() {
        return IPuzIOTest.class.getResourceAsStream("/barred-test.ipuz");
    }

    public static InputStream getTestPuzzleExtrasInputStream() {
        return IPuzIOTest.class.getResourceAsStream("/extras.ipuz");
    }

    public static InputStream getTestPuzzleZonesInputStream() {
        return IPuzIOTest.class.getResourceAsStream("/zones.ipuz");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test &amp; puzzle");
        assertEquals(puz.getAuthor(), "Test author");
        assertEquals(puz.getCopyright(), "Test copyright");
        assertEquals(puz.getSourceUrl(), "https://testurl.com");
        assertEquals(puz.getSource(), "Test publisher");
        assertEquals(puz.getDate(), LocalDate.of(2003,2,1));

        assertEquals(puz.getWidth(), 3);
        assertEquals(puz.getHeight(), 2);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[0][1].getClueNumber(), "2");
        assertFalse(boxes[0][1].isCircled());
        assertEquals(boxes[0][2], null);
        assertEquals(boxes[1][0].getClueNumber(), "3");
        assertEquals(boxes[1][0].getResponse(), 'A');
        assertTrue(boxes[1][0].isCircled());

        assertTrue(boxes[0][0].isBlank());
        assertEquals(boxes[0][1].getResponse(), 'B');
        assertEquals(boxes[1][1].getResponse(), 'C');
        assertTrue(boxes[1][2].isBlank());

        assertEquals(boxes[0][0].getSolution(), 'A');
        assertEquals(boxes[0][1].getSolution(), 'B');
        assertEquals(boxes[1][0].getSolution(), 'A');
        assertEquals(boxes[1][1].getSolution(), 'C');
        assertEquals(boxes[1][2].getSolution(), 'D');

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Vertical");

        assertEquals(acrossClues.getClue("1").getHint(), "Test clue 1");
        assertEquals(acrossClues.getClue("3").getHint(), "Test clue 2");
        assertEquals(downClues.getClue("1").getHint(), "Test clue 3");
        assertEquals(
            downClues.getClue("2").getHint(),
            "Test clue 4 (cont. 1 Across/1 Down) "
                + "(ref. 1&2 Across) (clues 2/1/3) (3-2-1)"
        );
    }

    public static void assertIsTestPuzzle2(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        assertTrue(boxes[1][1].isBarredTop());
        assertFalse(boxes[0][2].isBarredBottom());
        assertTrue(boxes[3][4].isBarredLeft());
        assertFalse(boxes[3][4].isBarredRight());

        assertEquals(boxes[8][3].getSolution(), 'V');
        assertEquals(boxes[10][1].getSolution(), 'R');
        assertEquals(boxes[1][10].getSolution(), 'W');

        assertTrue(boxes[1][2].isCircled());
        assertFalse(boxes[2][1].isCircled());

        assertTrue(boxes[0][7].isPartOf(new ClueID("5", "Across")));
        assertFalse(boxes[0][7].isPartOf(new ClueID("5", "Down")));
        assertTrue(boxes[1][7].isPartOf(new ClueID("6", "Down")));
        assertFalse(boxes[1][7].isPartOf(new ClueID("5", "Down")));

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClue("5").getHint(), "Clue 5");
        assertEquals(downClues.getClue("2").getHint(), "Clue 2d");
    }

    public static void assertIsTestPuzzleExtras(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        assertFalse(boxes[0][6].hasClueNumber());
        assertEquals(boxes[0][8].getClueNumber(), "5");
        assertFalse(boxes[6][0].hasClueNumber());
        assertEquals(boxes[7][0].getClueNumber(), "25");

        assertFalse(boxes[2][0].hasColor());
        assertFalse(boxes[8][10].hasColor());
        assertTrue(boxes[6][0].hasColor());
        assertTrue(boxes[10][6].hasColor());
        int grey = Integer.valueOf("DCDCDC", 16);
        assertEquals(boxes[6][0].getColor(), grey);
        assertEquals(boxes[10][6].getColor(), grey);

        Set<String> clueLists = puz.getClueListNames();
        assertEquals(clueLists.size(), 3);
        assertTrue(clueLists.contains("OddOnes"));

        ClueList oddClues = puz.getClues("OddOnes");

        assertEquals(oddClues.getUnnumberedClue(5).getHint(), "Odd sixth");
        assertEquals(oddClues.getUnnumberedClue(0).getHint(), "Odd first");
    }

    public static void assertIsTestPuzzleZones(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        assertTrue(boxes[2][2].isPartOf(new ClueID("&#x1f332;", "Bases")));
        assertFalse(boxes[2][2].isPartOf(new ClueID("1", "Pathways")));
        assertTrue(boxes[1][8].isPartOf(new ClueID("&#x2615;", "Bases")));
        assertFalse(boxes[1][8].isPartOf(new ClueID("2", "Pathways")));
        assertTrue(boxes[7][1].isPartOf(new ClueID("5", "Pathways")));
        assertFalse(boxes[7][1].isPartOf(new ClueID("&#x1f98a;", "Bases")));

        ClueList bases = puz.getClues("Bases");

        Zone zoneTree = bases.getClue("&#x2615;").getZone();
        Zone zoneThumb = bases.getClue("&#x1f44d;").getZone();

        assertEquals(zoneTree.size(), 8);
        assertEquals(zoneThumb.size(), 8);

        assertEquals(zoneTree.getPosition(3), new Position(1, 8));
        assertEquals(zoneThumb.getPosition(6), new Position(6, 6));

        ClueList pathways = puz.getClues("Pathways");

        Zone zone3 = pathways.getClue("4").getZone();

        assertEquals(zone3.size(), 5);
        assertEquals(zone3.getPosition(3), new Position(3, 5));
    }

    /**
     * Test HTML in various parts of puzzle
     */
    public static InputStream getTestPuzzleHTMLInputStream() {
        return JPZIOTest.class.getResourceAsStream("/html.ipuz");
    }

    public static void assertIsTestPuzzleHTML(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "<b>Test</b> &amp; puzzle<br>For testing");
        assertEquals(
            puz.getAuthor(), "Test author<br><b>For<sup>Test</sup></b>"
        );
        assertEquals(
            puz.getSource(), "Test &nbsp;&nbsp;publisher<br>test<i>test</i>"
        );

        ClueList acrossClues = puz.getClues("Across");

        assertEquals(
            acrossClues.getClue("1").getHint(),
            "Test <b>clue</b> 1<br>A clue&excl;"
        );
    }

    public void testIPuz() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }

    public void testIPuzWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzReadPlayWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            puz.setSupportUrl("http://test.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(2, 1));
            puz.setCurrentClueID(new ClueID("3", "Across"));

            puz.updateHistory(new ClueID("3", "Across"));
            puz.updateHistory(new ClueID("1", "Vertical"));

            puz.setNote(
                new ClueID("1", "Across"),
                new Note("test1", "test2", "test3", "test4")
            );
            puz.setNote(
                new ClueID("2", "Vertical"),
                new Note("test5", "test6\nnew line", "test7", "test8")
            );
            puz.flagClue(new ClueID("3", "Across"), true);
            puz.flagClue(new ClueID("1", "Vertical"), true);

            puz.setPlayerNote(
                new Note("scratch", "a note", "anagsrc", "anagsol")
            );

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse('X');
            boxes[1][2].setResponse('Y');
            boxes[0][1].setResponder("Test");
            boxes[1][0].setCheated(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            Box[][] boxes2 = puz2.getBoxes();

            assertEquals(puz2.getSupportUrl(), "http://test.url");
            assertEquals(puz2.getTime(), 1234L);
            assertEquals(puz.getPosition(), puz2.getPosition());
            assertEquals(
                puz.getCurrentClueID(),
                new ClueID("3", "Across")
            );
            assertEquals(
                puz.getHistory().get(0),
                new ClueID("1", "Vertical")
            );
            assertEquals(
                puz.getHistory().get(1),
                new ClueID("3", "Across")
            );
            assertEquals(
                puz.getNote(new ClueID("1", "Across")).getText(),
                "test2"
            );
            assertEquals(
                puz.getNote(new ClueID("2", "Vertical")).getText(),
                "test6\nnew line"
            );
            assertEquals(
                puz.getNote(new ClueID("2", "Vertical")).getAnagramSource(),
                "test7"
            );
            assertEquals(boxes2[0][1].getResponse(), 'X');
            assertEquals(boxes2[1][2].getResponse(), 'Y');
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());
            assertTrue(puz.isFlagged(new ClueID("1", "Vertical")));
            assertTrue(puz.isFlagged(new ClueID("3", "Across")));
            assertFalse(puz.isFlagged(new ClueID("1", "Across")));

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleHTML(puz);
        }
    }

    public void testIPuzWriteReadHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzBarred() throws Exception {
        try (InputStream is = getTestPuzzle2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle2(puz);
        }
    }

    public void testIPuzReadPlayWriteReadBarred() throws Exception {
        try (InputStream is = getTestPuzzle2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            puz.setSupportUrl("http://test.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(1, 2));
            puz.setCurrentClueID(new ClueID("12", "Down"));

            puz.updateHistory(new ClueID("3", "Down"));
            puz.updateHistory(new ClueID("1", "Across"));

            puz.setNote(
                new ClueID("1", "Across"),
                new Note("test1", "test2", "test3", "test4")
            );
            puz.setNote(
                new ClueID("2", "Down"),
                new Note("test5", "test6\nnew line", "test7", "test8")
            );

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse('X');
            boxes[1][2].setResponse('Y');
            boxes[0][1].setResponder("Test");
            boxes[1][0].setCheated(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            Box[][] boxes2 = puz2.getBoxes();

            assertEquals(puz2.getSupportUrl(), "http://test.url");
            assertEquals(puz2.getTime(), 1234L);
            assertEquals(puz.getPosition(), puz2.getPosition());
            assertEquals(
                puz.getCurrentClueID(),
                new ClueID("12", "Down")
            );
            assertEquals(
                puz.getHistory().get(0),
                new ClueID("1", "Across")
            );
            assertEquals(
                puz.getHistory().get(1),
                new ClueID("3", "Down")
            );
            assertEquals(
                puz.getNote(new ClueID("1", "Across")).getText(),
                "test2"
            );
            assertEquals(
                puz.getNote(new ClueID("2", "Down")).getText(),
                "test6\nnew line"
            );
            assertEquals(
                puz.getNote(new ClueID("2", "Down")).getAnagramSource(),
                "test7"
            );
            assertEquals(boxes2[0][1].getResponse(), 'X');
            assertEquals(boxes2[1][2].getResponse(), 'Y');
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzExtras() throws Exception {
        try (InputStream is = getTestPuzzleExtrasInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleExtras(puz);
        }
    }

    public void testIPuzWriteReadExtras() throws Exception {
        try (InputStream is = getTestPuzzleExtrasInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzZones() throws Exception {
        try (InputStream is = getTestPuzzleZonesInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleZones(puz);
        }
    }

    public void testIPuzWriteReadZones() throws Exception {
        try (InputStream is = getTestPuzzleZonesInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }
}

