package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPuzIOTest {

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
        return IPuzIOTest.class.getResourceAsStream("/zones-0-based.ipuz");
    }

    public static InputStream getTestPuzzleZones1BasedInputStream() {
        return IPuzIOTest.class.getResourceAsStream("/zones-1-based.ipuz");
    }

    // same as 1-based, but check picked up by version number
    public static InputStream getTestPuzzleZonesIOV2InputStream() {
        return IPuzIOTest.class.getResourceAsStream("/zones-io-v2.ipuz");
    }

    public static InputStream getTestPuzzleAcrosticInputStream() {
        return IPuzIOTest.class.getResourceAsStream("/acrostic.ipuz");
    }

    public static InputStream getTestPuzzleShapesInputStream() {
        return IPuzIOTest.class.getResourceAsStream("/shapes.ipuz");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getKind(), Puzzle.Kind.CROSSWORD);

        assertEquals(puz.getTitle(), "Test &amp; puzzle");
        assertEquals(puz.getAuthor(), "Test author");
        assertEquals(puz.getCopyright(), "Test copyright");
        assertEquals(puz.getSourceUrl(), "https://testurl.com");
        assertEquals(puz.getSource(), "Test publisher");
        assertEquals(puz.getIntroMessage(), "Intro");
        assertEquals(puz.getCompletionMessage(), "Explanation");
        assertEquals(puz.getDate(), LocalDate.of(2003,2,1));

        assertEquals(puz.getWidth(), 3);
        assertEquals(puz.getHeight(), 2);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[0][1].getClueNumber(), "2");
        assertFalse(boxes[0][1].hasShape());
        assertTrue(Box.isBlock(boxes[0][2]));
        assertEquals(boxes[1][0].getClueNumber(), "3");
        assertEquals(boxes[1][0].getResponse(), "A");
        assertEquals(boxes[1][0].getShape(), Box.Shape.CIRCLE);

        assertTrue(boxes[0][0].isBlank());
        assertEquals(boxes[0][1].getResponse(), "B");
        assertEquals(boxes[1][1].getResponse(), "C");
        assertTrue(boxes[1][2].isBlank());

        assertEquals(boxes[0][0].getSolution(), "A");
        assertEquals(boxes[0][1].getSolution(), "B");
        assertEquals(boxes[1][0].getSolution(), "A");
        assertEquals(boxes[1][1].getSolution(), "C");
        assertEquals(boxes[1][2].getSolution(), "D");

        assertFalse(boxes[0][0].hasMarks());
        assertTrue(boxes[1][0].hasMarks());
        assertEquals(boxes[1][0].getMarks()[1][2], "t");
        assertNull(boxes[1][0].getMarks()[0][2]);

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Vertical");

        Clue clueOneAcross = acrossClues.getClueByNumber("1");
        assertEquals(clueOneAcross.getHint(), "Test clue 1");
        assertFalse(clueOneAcross.hasLabel());
        assertEquals(acrossClues.getClueByNumber("3").getHint(), "Test clue 2");

        Clue clueOneDown = downClues.getClueByNumber("1");
        assertEquals(clueOneDown.getLabel(), "One");
        assertEquals(clueOneDown.getHint(), "Test clue 3");
        assertEquals(
            downClues.getClueByNumber("2").getHint(),
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

        assertEquals(boxes[8][3].getSolution(), "V");
        assertEquals(boxes[10][1].getSolution(), "R");
        assertEquals(boxes[1][10].getSolution(), "W");

        assertEquals(boxes[1][2].getShape(), Box.Shape.CIRCLE);
        assertFalse(boxes[2][1].hasShape());

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertTrue(
            boxes[0][7].isPartOf(acrossClues.getClueByNumber("5"))
        );
        assertFalse(
            boxes[0][7].isPartOf(downClues.getClueByNumber("5"))
        );
        assertTrue(
            boxes[1][7].isPartOf(downClues.getClueByNumber("6"))
        );
        assertFalse(
            boxes[1][7].isPartOf(downClues.getClueByNumber("5"))
        );

        assertEquals(acrossClues.getClueByNumber("5").getHint(), "Clue 5");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Clue 2d");
    }

    public static void assertIsTestPuzzleExtras(Puzzle puz) throws Exception {
        assertEquals(
            puz.getIntroMessage(),
            "Weird features: unnumbered words, clues "
                +  "in non-standards lists, colors, etc"
        );
        assertEquals(puz.getNotes(), "Puzzle notes");
        assertEquals(puz.getCompletionMessage(), "You solved the puzzle");

        Box[][] boxes = puz.getBoxes();

        assertFalse(boxes[0][6].hasClueNumber());
        assertEquals(boxes[0][8].getClueNumber(), "5");
        assertFalse(boxes[6][0].hasClueNumber());
        assertEquals(boxes[7][0].getClueNumber(), "25");

        assertFalse(boxes[2][0].hasColor());
        assertFalse(boxes[8][10].hasColor());
        assertTrue(boxes[6][0].hasColor());
        assertTrue(boxes[10][6].hasColor());
        int grey1 = Integer.valueOf("DCDCDC", 16);
        int grey2 = Integer.valueOf("EFEFEF", 16);
        int grey3 = Integer.valueOf("EAEAEA", 16);
        int darkGreen = Integer.valueOf("50c878", 16);
        int yellow = Integer.valueOf("FFFAA0", 16);
        assertEquals(boxes[0][0].getColor(), yellow);
        assertFalse(boxes[1][1].hasColor());
        assertEquals(boxes[6][0].getColor(), grey1);
        assertEquals(boxes[10][6].getColor(), grey1);
        assertEquals(boxes[0][2].getColor(), grey2);
        assertEquals(boxes[0][3].getColor(), grey3);
        assertFalse(boxes[0][3].hasTextColor());
        assertFalse(boxes[0][3].hasBarColor());
        assertEquals(boxes[0][8].getColor(), darkGreen);
        assertTrue(boxes[0][8].hasTextColor());
        assertTrue(boxes[0][8].hasBarColor());
        assertEquals(boxes[0][8].getTextColor(), Integer.valueOf("123456", 16));
        assertEquals(boxes[0][8].getBarColor(), Integer.valueOf("654321", 16));
        assertFalse(boxes[0][9].hasBarColor());
        assertEquals(boxes[0][9].getTextColor(), Integer.valueOf("121212", 16));
        assertFalse(boxes[0][10].hasTextColor());
        assertEquals(boxes[0][10].getBarColor(), Integer.valueOf("212121", 16));
        assertEquals(boxes[1][2].getInitialValue(), "A");
        assertFalse(boxes[1][1].hasInitialValue());
        assertEquals(boxes[1][4].getInitialValue(), "Y");

        Box interestingBlock = boxes[1][3];
        assertTrue(interestingBlock.isBlock());
        assertEquals(interestingBlock.getColor(), grey2);
        assertEquals(interestingBlock.getInitialValue(), "X");
        assertEquals(interestingBlock.getResponse(), "X");

        assertTrue(puz.hasInitialValueCells());

        Box bars = boxes[0][4];
        assertEquals(bars.getBarTop(), Box.Bar.DASHED);
        assertEquals(bars.getBarBottom(), Box.Bar.DOTTED);
        assertEquals(bars.getBarLeft(), Box.Bar.SOLID);
        assertEquals(bars.getBarRight(), Box.Bar.NONE);

        Set<String> clueLists = puz.getClueListNames();
        assertEquals(clueLists.size(), 3);
        assertTrue(clueLists.contains("OddOnes"));

        ClueList oddClues = puz.getClues("OddOnes");

        assertEquals(oddClues.getClueByIndex(5).getHint(), "Odd sixth");
        assertEquals(oddClues.getClueByIndex(0).getHint(), "Odd first");
    }

    public static void assertIsTestPuzzleZones(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        ClueList bases = puz.getClues("Bases");
        ClueList pathways = puz.getClues("Pathways");

        assertTrue(
            boxes[2][2].isPartOf(bases.getClueByNumber("&#x1f332;"))
        );
        assertFalse(
            boxes[2][2].isPartOf(pathways.getClueByNumber("1"))
        );
        assertTrue(
            boxes[1][8].isPartOf(bases.getClueByNumber("&#x2615;"))
        );
        assertFalse(
            boxes[1][8].isPartOf(pathways.getClueByNumber("2"))
        );
        assertTrue(
            boxes[7][1].isPartOf(pathways.getClueByNumber("5"))
        );
        assertFalse(
            boxes[7][1].isPartOf(bases.getClueByNumber("&#x1f98a;"))
        );

        Zone zoneTree = bases.getClueByNumber("&#x2615;").getZone();
        Zone zoneThumb = bases.getClueByNumber("&#x1f44d;").getZone();

        assertEquals(zoneTree.size(), 8);
        assertEquals(zoneThumb.size(), 8);

        assertEquals(zoneTree.getPosition(3), new Position(1, 8));
        assertEquals(zoneThumb.getPosition(6), new Position(6, 6));

        Zone zone3 = pathways.getClueByNumber("4").getZone();

        assertEquals(zone3.size(), 5);
        assertEquals(zone3.getPosition(3), new Position(3, 5));
    }

    public static void assertIsTestPuzzleAcrostic(Puzzle puz) throws Exception {
        assertEquals(puz.getKind(), Puzzle.Kind.ACROSTIC);

        ClueList words = puz.getClues("Clues");
        ClueList quote = puz.getClues("Quote");

        Clue clueA = words.getClueByIndex(0);
        Clue clueC = words.getClueByIndex(2);

        assertEquals(clueA.getLabel(), "A");
        assertEquals(clueC.getLabel(), "C");

        Zone zoneA = clueA.getZone();
        assertEquals(zoneA.getPosition(2), new Position(0, 2));
        assertEquals(zoneA.getPosition(3), new Position(0, 4));

        Zone zoneQuote = quote.getClueByIndex(0).getZone();
        assertEquals(zoneQuote.size(), 13);
        assertEquals(zoneQuote.getPosition(5), new Position(1, 1));
    }

    public static void assertIsTestPuzzleShapes(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getShape(), Box.Shape.CIRCLE);
        assertEquals(boxes[0][1].getShape(), Box.Shape.ARROW_LEFT);
        assertEquals(boxes[0][2].getShape(), Box.Shape.ARROW_RIGHT);
        assertEquals(boxes[0][3].getShape(), Box.Shape.ARROW_UP);
        assertEquals(boxes[0][4].getShape(), Box.Shape.ARROW_DOWN);
        assertEquals(boxes[0][5].getShape(), Box.Shape.TRIANGLE_LEFT);
        assertEquals(boxes[0][6].getShape(), Box.Shape.TRIANGLE_RIGHT);
        assertEquals(boxes[0][7].getShape(), Box.Shape.TRIANGLE_UP);
        assertEquals(boxes[0][8].getShape(), Box.Shape.TRIANGLE_DOWN);
        assertEquals(boxes[0][9].getShape(), Box.Shape.DIAMOND);

        assertEquals(boxes[1][0].getShape(), Box.Shape.CLUB);
        assertEquals(boxes[1][1].getShape(), Box.Shape.HEART);
        assertEquals(boxes[1][2].getShape(), Box.Shape.SPADE);
        assertEquals(boxes[1][3].getShape(), Box.Shape.STAR);
        assertEquals(boxes[1][4].getShape(), Box.Shape.SQUARE);
        assertEquals(boxes[1][5].getShape(), Box.Shape.RHOMBUS);
        assertEquals(boxes[1][6].getShape(), Box.Shape.FORWARD_SLASH);
        assertEquals(boxes[1][7].getShape(), Box.Shape.BACK_SLASH);
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
            acrossClues.getClueByNumber("1").getHint(),
            "Test <b>clue</b> 1<br>A clue&excl;"
        );
    }

    @Test
    public void testIPuz() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }

    @Test
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

    @Test
    public void testIPuzReadPlayWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ClueList across = puz.getClues("Across");
            ClueList vertical = puz.getClues("Vertical");

            ClueID cidA1 = across.getClueByNumber("1").getClueID();
            ClueID cidA3 = across.getClueByNumber("3").getClueID();
            ClueID cidV1 = vertical.getClueByNumber("1").getClueID();
            ClueID cidV2 = vertical.getClueByNumber("2").getClueID();

            puz.setSupportUrl("http://test.url");
            puz.setShareUrl("http://testshare.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(2, 1));
            puz.setCurrentClueID(cidA3);

            puz.updateHistory(cidA3);
            puz.updateHistory(cidV1);

            puz.setNote(
                cidA1,
                new Note("test1", "test2", "test3", "test4")
            );
            puz.setNote(
                cidV2,
                new Note("test5", "test6\nnew line", "test7", "test8")
            );
            puz.flagClue(cidA3, true);
            puz.flagClue(cidV1, true);

            puz.setPlayerNote(
                new Note("scratch", "a note", "anagsrc", "anagsol")
            );

            puz.addImage(new PuzImage("myimage.jpg", 2, 3, 4, 5));

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse("X");
            boxes[1][2].setResponse("Y");
            boxes[0][1].setResponder("Test");
            boxes[1][0].setCheated(true);

            puz.setPinnedClueID(cidA3);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            Box[][] boxes2 = puz2.getBoxes();

            assertEquals(puz2.getSupportUrl(), "http://test.url");
            assertEquals(puz2.getShareUrl(), "http://testshare.url");
            assertEquals(puz2.getTime(), 1234L);
            assertEquals(puz.getPosition(), puz2.getPosition());
            assertEquals(puz.getCurrentClueID(), cidA3);
            assertEquals(puz.getHistory().get(0), cidV1);
            assertEquals(puz.getHistory().get(1), cidA3);
            assertEquals(puz.getNote(cidA1).getText(), "test2"
            );
            assertEquals(puz.getNote(cidV2).getText(), "test6\nnew line");
            assertEquals(puz.getNote(cidV2).getAnagramSource(), "test7");
            assertEquals(boxes2[0][1].getResponse(), "X");
            assertEquals(boxes2[1][2].getResponse(), "Y");
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());
            assertTrue(puz.isFlagged(cidV1));
            assertTrue(puz.isFlagged(cidA3));
            assertFalse(puz.isFlagged(cidA1));
            assertEquals(puz.getPinnedClueID(), cidA3);

            assertEquals(puz, puz2);
        }
    }

    @Test
    public void testIPuzHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleHTML(puz);
        }
    }

    @Test
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

    @Test
    public void testIPuzBarred() throws Exception {
        try (InputStream is = getTestPuzzle2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle2(puz);
        }
    }

    @Test
    public void testIPuzReadPlayWriteReadBarred() throws Exception {
        try (InputStream is = getTestPuzzle2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ClueList across = puz.getClues("Across");
            ClueList down = puz.getClues("Down");

            ClueID cidD2 = down.getClueByNumber("2").getClueID();
            ClueID cidD3 = down.getClueByNumber("3").getClueID();
            ClueID cidD12 = down.getClueByNumber("12").getClueID();
            ClueID cidA1 = across.getClueByNumber("1").getClueID();

            puz.setSupportUrl("http://test.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(1, 2));
            puz.setCurrentClueID(cidD12);

            puz.updateHistory(cidD3);
            puz.updateHistory(cidA1);

            puz.setNote(cidA1, new Note("test1", "test2", "test3", "test4"));
            puz.setNote(
                cidD2,
                new Note("test5", "test6\nnew line", "test7", "test8")
            );

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse("X");
            boxes[1][2].setResponse("Y");
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
            assertEquals(puz.getCurrentClueID(), cidD12);
            assertEquals(puz.getHistory().get(0), cidA1);
            assertEquals(puz.getHistory().get(1), cidD3);
            assertEquals(puz.getNote(cidA1).getText(), "test2");
            assertEquals(puz.getNote(cidD2).getText(), "test6\nnew line");
            assertEquals(puz.getNote(cidD2).getAnagramSource(), "test7");
            assertEquals(boxes2[0][1].getResponse(), "X");
            assertEquals(boxes2[1][2].getResponse(), "Y");
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());

            assertEquals(puz, puz2);
        }
    }

    @Test
    public void testIPuzExtras() throws Exception {
        try (InputStream is = getTestPuzzleExtrasInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleExtras(puz);
        }
    }

    @Test
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

    @Test
    public void testIPuzZones() throws Exception {
        try (InputStream is = getTestPuzzleZonesInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleZones(puz);
        }
    }

    @Test
    public void testIPuzZones1Based() throws Exception {
        try (InputStream is = getTestPuzzleZones1BasedInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleZones(puz);
        }
    }

    @Test
    public void testIPuzZonesIOV2() throws Exception {
        try (InputStream is = getTestPuzzleZonesIOV2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleZones(puz);
        }
    }

    @Test
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

    @Test
    public void testIPuzAcrostic() throws Exception {
        try (InputStream is = getTestPuzzleAcrosticInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleAcrostic(puz);
        }
    }

    @Test
    public void testIPuzWriteReadAcrostic() throws Exception {
        try (InputStream is = getTestPuzzleAcrosticInputStream()) {
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

    @Test
    public void testJPZAcrosticWriteIPuzRead() throws Exception {
        try (InputStream is = JPZIOTest.getTestPuzzleAcrosticInputStream()) {
            Puzzle puz = JPZIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            // bit of a hack because "" is read back as null
            puz.setNotes(null);

            assertEquals(puz, puz2);
        }
    }

    @Test
    public void testIPuzShapeReadWrite() throws Exception {
        try (InputStream is = getTestPuzzleShapesInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            assertIsTestPuzzleShapes(puz);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            // bit of a hack because "" is read back as null
            puz.setNotes(null);

            assertEquals(puz, puz2);
        }
    }


}

