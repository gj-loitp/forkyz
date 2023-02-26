
package app.crossword.yourealwaysbe.puz;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.IOTest;
import app.crossword.yourealwaysbe.io.IPuzIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayboardTest {

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "blockobjects = {0}")
    @ValueSource(booleans = {false, true})
    private @interface TestWithAndWithoutBlockObjects { }

    @TestWithAndWithoutBlockObjects
    public void testMoveUp(boolean blockObjects) throws Exception {
         Puzzle puz = loadTestPuz(blockObjects);

         Playboard board = new Playboard(puz);
         moveToPosition(board, 5, 5);
         board.moveUp(false);

         assertAtRow(board, 4);
         board.moveUp(false);

         assertAtRow(board, 3);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         moveToPosition(board, 4, 4);

         assertAtRow(board, 4);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         assertAtRow(board, 1);
         board.moveUp(false);

         assertAtRow(board, 0);
         board.moveUp(false);

         assertAtRow(board, 0);
         board.moveUp(false);

         assertAtRow(board, 0);
         board.moveUp(false);
    }

    @TestWithAndWithoutBlockObjects
    public void testDeleteLetter(boolean blockObjects) throws Exception {
         Puzzle puz = loadTestPuz(blockObjects);

         Playboard board = new Playboard(puz);
         board.setDontDeleteCrossing(false);

         // Create
         //
         //  A
         // ABCDE
         //    A
         //
         // then delete from E back, row should be empty

         moveToPosition(board, 1, 0);
         board.playLetter('A');
         board.playLetter('B');
         board.playLetter('C');
         board.playLetter('D');
         board.playLetter('E');

         moveToPosition(board, 0, 1);
         board.playLetter('A');

         moveToPosition(board, 2, 3);
         board.playLetter('A');

         moveToPosition(board, 1, 4);
         for (int i = 0; i < 5; i++)
             board.deleteLetter();

         assertBoxBlank(puz, 1, 0);
         assertBoxBlank(puz, 1, 1);
         assertBoxBlank(puz, 1, 2);
         assertBoxBlank(puz, 1, 3);
         assertBoxBlank(puz, 1, 4);
         assertBoxLetter(puz, 0, 1, "A");
         assertBoxLetter(puz, 2, 3, "A");
    }

    @TestWithAndWithoutBlockObjects
    public void testDeleteLetterCrossing(boolean blockObjects) throws Exception {
         Puzzle puz = loadTestPuz(blockObjects);

         Playboard board = new Playboard(puz);
         board.setDontDeleteCrossing(true);

         // Create
         //
         //  A
         // ABCDE
         //    A
         //
         // then delete from E back, B and D should remain

         moveToPosition(board, 1, 0);
         board.playLetter('A');
         board.playLetter('B');
         board.playLetter('C');
         board.playLetter('D');
         board.playLetter('E');

         moveToPosition(board, 0, 1);
         board.playLetter('A');

         moveToPosition(board, 2, 3);
         board.playLetter('A');

         moveToPosition(board, 1, 4);
         for (int i = 0; i < 5; i++)
             board.deleteLetter();

         assertBoxBlank(puz, 1, 0);
         assertBoxLetter(puz, 1, 1, "B");
         assertBoxBlank(puz, 1, 2);
         assertBoxLetter(puz, 1, 3, "D");
         assertBoxBlank(puz, 1, 4);
    }

    @TestWithAndWithoutBlockObjects
    public void testMoveNextOnAxis(boolean blockObjects) throws Exception {
        Puzzle puz = loadTestPuz(blockObjects);
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.MOVE_NEXT_ON_AXIS);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 0, 6);
        for (int i = 0; i < 10; i++)
            board.playLetter('A');
        assertPosition(board, 0, 14);

        // Down
        moveToPosition(board, 0, 6);
        board.toggleSelection();

        for (int i = 0; i < 3; i++)
            board.playLetter('A');
        assertPosition(board, 3, 6);
        board.playLetter('A');
        assertPosition(board, 5, 6);
        for (int i = 0; i < 10; i++)
            board.playLetter('A');
        assertPosition(board, 14, 6);

        // Down / Back
        moveToPosition(board, 5, 6);
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 3, 6);
        for (int i = 0; i < 5; i++)
            board.deleteLetter();

        // Across / Back
        moveToPosition(board, 0, 6);
        board.toggleSelection();
        board.deleteLetter();
        assertPosition(board, 0, 4);
        for (int i = 0; i < 6; i++)
            board.deleteLetter();
        assertPosition(board, 0, 0);
    }

    @TestWithAndWithoutBlockObjects
    public void testMoveStopEnd(boolean blockObjects) throws Exception {
        Puzzle puz = loadTestPuz(blockObjects);
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.STOP_ON_END);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 0, 4);

        // Down
        moveToPosition(board, 0, 6);
        board.toggleSelection();

        for (int i = 0; i < 3; i++)
            board.playLetter('A');
        assertPosition(board, 3, 6);
        board.playLetter('A');
        assertPosition(board, 3, 6);

        // Down / Back
        moveToPosition(board, 5, 6);
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 5, 6);

        // Across / Back
        moveToPosition(board, 0, 6);
        board.toggleSelection();
        board.deleteLetter();
        assertPosition(board, 0, 6);
    }

    @TestWithAndWithoutBlockObjects
    public void testMoveNextClue(boolean blockObjects) throws Exception {
        Puzzle puz = loadTestPuz(blockObjects);
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.MOVE_NEXT_CLUE);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 0, 6);
        for (int i = 0; i < 8; i++)
            board.playLetter('A');
        assertPosition(board, 1, 0);

        // Down
        moveToPosition(board, 0, 6);
        board.toggleSelection();

        for (int i = 0; i < 3; i++)
            board.playLetter('A');
        assertPosition(board, 3, 6);
        board.playLetter('A');
        assertPosition(board, 0, 7);

        // Down / Back
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 3, 6);

        // Across / Back
        moveToPosition(board, 0, 6);
        board.toggleSelection();
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 0, 4);
        for (int i = 0; i < 5; i++)
            board.deleteLetter();
        // wrap to down clues
        assertPosition(board, 14, 10);
        for (int i = 0; i < 3; i++)
            board.deleteLetter();
        assertPosition(board, 14, 14);

        // Wrap across
        board.toggleSelection();
        board.playLetter('A');
        assertPosition(board, 0, 0);

        // Wrap down
        board.toggleSelection();
        board.deleteLetter();
        // wrap to across clues
        assertPosition(board, 14, 10);
        board.playLetter('A');
        assertPosition(board, 0, 0);
    }

    @TestWithAndWithoutBlockObjects
    public void testMoveParallel(boolean blockObjects) throws Exception {
        Puzzle puz = loadTestPuz(blockObjects);
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.MOVE_PARALLEL_WORD);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 1, 0);
        for (int i = 0; i < 100; i++)
            board.playLetter('A');
        assertPosition(board, 14, 3);

        // Down
        moveToPosition(board, 0, 4);
        board.toggleSelection();

        for (int i = 0; i < 2; i++)
            board.playLetter('A');
        assertPosition(board, 2, 4);
        board.playLetter('A');
        assertPosition(board, 0, 6);
        for (int i = 0; i < 100; i++)
            board.playLetter('A');
        assertPosition(board, 3, 14);

        // Down / Back
        for (int i = 0; i < 5; i++)
            board.deleteLetter();
        assertPosition(board, 3, 13);
        for (int i = 0; i < 100; i++)
            board.deleteLetter();
        assertPosition(board, 0, 0);

        // Across / Back
        moveToPosition(board, 14, 3);
        board.toggleSelection();
        for (int i = 0; i < 5; i++)
            board.deleteLetter();
        assertPosition(board, 13, 3);
        for (int i = 0; i < 100; i++)
            board.deleteLetter();
        assertPosition(board, 0, 0);
    }

    @TestWithAndWithoutBlockObjects
    public void testPlayFullMoveAxis(boolean blockObjects) throws Exception {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.MOVE_NEXT_ON_AXIS);
    }

    @TestWithAndWithoutBlockObjects
    public void testPlayFullMoveStopEnd(boolean blockObjects) throws Exception {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.STOP_ON_END);
    }

    @TestWithAndWithoutBlockObjects
    public void testPlayFullMoveNextClue(boolean blockObjects) throws Exception {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.MOVE_NEXT_CLUE);
    }

    @TestWithAndWithoutBlockObjects
    public void testPlayFullMoveParallelWord(boolean blockObjects) throws Exception {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.MOVE_PARALLEL_WORD);
    }

    @TestWithAndWithoutBlockObjects
    public void testDetachedWord(boolean blockObjects) throws Exception {
        Puzzle puz = loadTestDetachedPuz(blockObjects);
        Playboard board = new Playboard(puz);
        moveToPosition(board, 8, 2);
        Playboard.Word word = board.getCurrentWord();
        Zone boardZone = word.getZone();

        int[][] expectedZoneArr = new int[][] {
            {7, 1}, {7, 2}, {7, 3}, {7, 5},
            {8, 2}, {9, 2}, {10, 2},
            {9, 1}, {9, 3},
            {12, 2}, {14, 2},
        };
        Zone expectedZone = new Zone();
        for (int[] pos : expectedZoneArr)
            expectedZone.addPosition(new Position(pos[0], pos[1]));

        assertEquals(boardZone, expectedZone);
    }

    @TestWithAndWithoutBlockObjects
    public void testRevealInitialLetter(boolean blockObjects) throws Exception {
        Puzzle puz = loadTestPuz(blockObjects);
        Playboard board = new Playboard(puz);
        Position pos1 = new Position(14, 5);
        Box box1 = puz.checkedGetBox(pos1);
        Box box2 = puz.checkedGetBox(0, 3);
        Box box3 = puz.checkedGetBox(3, 6);

        board.setHighlightLetter(pos1);
        box1.setResponse("_");

        board.revealInitialLetter();
        assertEquals(box1.getResponse(), "_");

        box1.setInitialValue("*");
        box2.setInitialValue("*");

        board.revealInitialLetter();
        assertEquals(box1.getResponse(), "*");
        assertTrue(box2.isBlank());
        assertTrue(box3.isBlank());

        board.revealInitialLetters();
        assertEquals(box1.getResponse(), "*");
        assertEquals(box2.getResponse(), "*");
        assertTrue(box3.isBlank());
    }

    private void checkNoMoveFullGrid(
        boolean blockObjects, MovementStrategy moveStrat
    ) throws Exception {
        // pick a box this far from the edges to test
        final int CHECK_OFFSET = 5;

        Puzzle puz = loadTestPuz(blockObjects);

        int width = puz.getWidth();
        int height = puz.getHeight();
        Box[][] boxes = puz.getBoxes();

        Playboard board = new Playboard(puz);
        board.setMovementStrategy(moveStrat);
        board.setSkipCompletedLetters(true);

        Position checkPos = null;

        // fill grid, find checkPos
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Box box = boxes[row][col];
                if (!Box.isBlock(box)) {
                    if (
                        checkPos == null
                        && row > CHECK_OFFSET && col > CHECK_OFFSET
                    ) {
                        checkPos = new Position(row, col);
                        box.setBlank();
                    } else {
                        box.setResponse('A');
                    }
                }
            }
        }

        board.setHighlightLetter(checkPos);
        board.playLetter('A');

        assertEquals(checkPos, board.getHighlightLetter());
    }

    private void assertBoxBlank(Puzzle puz, int row, int col) throws Exception {
        assertTrue(puz.checkedGetBox(row, col).isBlank());
    }

    private void assertBoxLetter(
        Puzzle puz, int row, int col, String letter
    ) throws Exception {
        assertEquals(puz.checkedGetBox(row, col).getResponse(), letter);
    }

    private void assertPosition(
        Playboard board, int row, int col
    ) throws Exception {
        assertEquals(board.getHighlightLetter(), new Position(row, col));
    }

    private void assertAtRow(Playboard board, int row) throws Exception {
        assertEquals(board.getHighlightLetter().getRow(), row);
    }

    private void moveToPosition(Playboard board, int row, int col) {
        Position pos = new Position(row, col);
        if (!pos.equals(board.getHighlightLetter()))
            board.setHighlightLetter(pos);
    }

    /**
     * Load test puzzle
     *
     * @param blockObjects whether to use objects for blank boxes
     * instead of null
     */
    private Puzzle loadTestPuz(boolean blockObjects) throws IOException {
        Puzzle puz = IO.loadNative(
            new DataInputStream(
                IOTest.class.getResourceAsStream("/test.puz")
            )
        );

        if (blockObjects)
            insertBlockObjects(puz);

        return puz;
    }

    /**
     * Load test puzzle with detached cells
     *
     * @param blockObjects whether to use objects for blank boxes
     * instead of null
     */
    private Puzzle loadTestDetachedPuz(boolean blockObjects)
            throws IOException {
        Puzzle puz = IPuzIO.readPuzzle(
            IOTest.class.getResourceAsStream("/detachedCells.ipuz")
        );

        if (blockObjects)
            insertBlockObjects(puz);

        return puz;
    }

    /**
     * Replace null boxes with a block object
     */
    private void insertBlockObjects(Puzzle puz) {
        if (puz == null)
            return;

        Box block = new Box();
        block.setBlock(true);

        Box[][] boxes = puz.getBoxes();
        for (int row = 0; row < puz.getHeight(); row++) {
            for (int col = 0; col < puz.getWidth(); col++) {
                if (boxes[row][col] == null)
                    boxes[row][col] = block;
            }
        }
    }
}
