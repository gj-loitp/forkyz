package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;

public class Box implements Serializable {
    public static final String BLANK = " ";
    private static final int NOCOLOR = -1;

    // use if block has styles (such as colour)
    private boolean block = false;
    private String responder;
    private boolean cheated;
    private String initialValue;
    private String response = BLANK;
    private String solution = null;
    private String clueNumber;
    // for each clue this box is a part of, the index of the cell it is
    // the clue word, sorted for consistency of cycling through clues
    private NavigableMap<ClueID, Integer> cluePositions = new TreeMap<>();

    public enum Bar { NONE, SOLID, DASHED, DOTTED };
    private Bar barTop = Bar.NONE;
    private Bar barBottom = Bar.NONE;
    private Bar barLeft = Bar.NONE;
    private Bar barRight = Bar.NONE;

    public enum Shape {
        CIRCLE, ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN, TRIANGLE_LEFT,
        TRIANGLE_RIGHT, TRIANGLE_UP, TRIANGLE_DOWN, DIAMOND, CLUB,
        HEART, SPADE, STAR, SQUARE, RHOMBUS, FORWARD_SLASH, BACK_SLASH,
        X
    };
    private Shape shape;

    // 3x3 grid of small text marks
    private String[][] marks = null;

    // 24-bit representation 0x00rrggbb
    private int color = NOCOLOR;
    private int textColor = NOCOLOR;
    private int barColor = NOCOLOR;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Box other = (Box) obj;

        if (!cluePositions.equals(other.cluePositions)) {
            return false;
        }

        if (isCheated() != other.isCheated()) {
            return false;
        }
        if (!Objects.equals(getClueNumber(), other.getClueNumber())) {
            return false;
        }

        if (!Objects.equals(getShape(), other.getShape())) {
            return false;
        }

        if (isBlock() != other.isBlock()) {
            return false;
        }

        if (getResponder() == null) {
            if (other.getResponder() != null) {
                return false;
            }
        } else if (!responder.equals(other.responder)) {
            return false;
        }

        if (!Objects.equals(getInitialValue(), other.getInitialValue())) {
            return false;
        }

        if (!Objects.equals(getResponse(), other.getResponse())) {
            return false;
        }

        if (!Objects.equals(getSolution(), other.getSolution())) {
            return false;
        }

        if (!Objects.equals(getBarTop(), other.getBarTop()))
            return false;

        if (!Objects.equals(getBarBottom(), other.getBarBottom()))
            return false;

        if (!Objects.equals(getBarLeft(), other.getBarLeft()))
            return false;

        if (!Objects.equals(getBarRight(), other.getBarRight()))
            return false;

        if (getColor() != other.getColor())
            return false;

        if (getTextColor() != other.getTextColor())
            return false;

        if (getBarColor() != other.getBarColor())
            return false;

        // Annoying Arrays.equals doesn't do arrays of arrays..
        String[][] marks = getMarks();
        String[][] otherMarks = other.getMarks();
        if (marks != null || otherMarks != null) {
            if (marks == null || otherMarks == null)
                return false;
            if (marks.length != otherMarks.length)
                return false;
            for (int row = 0; row < marks.length; row++) {
                if (!Arrays.equals(marks[row], otherMarks[row]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + cluePositions.hashCode();
        result = (prime * result) + (isCheated() ? 1231 : 1237);
        result = (prime * result) + Objects.hash(getClueNumber());
        result = (prime * result) + Objects.hash(getShape());
        result = (prime * result) + Objects.hash(
            getBarTop(), getBarBottom(), getBarLeft(), getBarRight()
        );
        result = (prime * result) +
            ((getResponder() == null) ? 0 : getResponder().hashCode());
        result = (prime * result) + Objects.hash(getInitialValue());
        result = (prime * result) + Objects.hash(getResponse());
        result = (prime * result) + Objects.hash(getSolution());
        result = (prime * result) + getColor() + getTextColor() + getBarColor();
        result = (prime * result) + (isBlock() ? 1231 : 1237);
        // ignore marks, too awkward and probably empty

        return result;
    }

    @Override
    public String toString() {
        String number = getClueNumber();
        if (number != null)
            return number + getSolution() + " ";
        else
            return getSolution() + " ";
    }

    /**
     * @param responder the responder to set
     */
    public void setResponder(String responder) {
        this.responder = responder;
    }

    /**
     * @return if start of clue in list name with box number
     */
    public boolean isStartOf(ClueID clueID) {
        Integer position = cluePositions.get(clueID);
        return position != null && position == 0;
    }

    /**
     * True if this box is a block (variant of null)
     *
     * Block boxes are useful if the block contains e.g. a colour or
     * shape
     */
    public boolean isBlock() {
        return block;
    }

    /**
     * Convenience method for checking block
     *
     * Could be a null box or isBlock true
     *
     * Block boxes cannot have solutions or be part of a clue or be
     * cheated. But may have styles like colours and shapes. May also
     * have responses, but only if they were defined in the original
     * puzzle (not entered by a user).
     */
    public static boolean isBlock(Box box) {
        return box == null || box.isBlock();
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    /**
     * @return the cheated
     */
    public boolean isCheated() {
        return cheated;
    }

    /**
     * @param cheated the cheated to set
     */
    public void setCheated(boolean cheated) {
        this.cheated = cheated;
    }

    public boolean hasShape() {
        return shape != null;
    }

    public Shape getShape() {
        return this.shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public boolean hasInitialValue() {
        return initialValue != null;
    }

    public String getInitialValue() {
        return initialValue;
    }

    /**
     * When setting this, you should also setResponse
     *
     * This field will not actually be rendered, it's just used when
     * outputting the puzzle without its saved state.
     */
    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }

    /**
     * @return the response
     */
    public String getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(char response) {
        setResponse(String.valueOf(response));
    }

    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * True if box has solution (i.e. not '\0')
     */
    public boolean hasSolution() {
        return getSolution() != null;
    }

    /**
     * @return the solution
     */
    public String getSolution() {
        return solution;
    }

    /**
     * @param solution the solution to set
     */
    public void setSolution(char solution) {
        setSolution(String.valueOf(solution));
    }

    public void setSolution(String solution) {
        this.solution = String.valueOf(solution);
    }

    /**
     * True if there is a clue number in the box
     */
    public boolean hasClueNumber() {
        return getClueNumber() != null;
    }

    /**
     * @return the clueNumber, or null for no clue
     */
    public String getClueNumber() {
        return clueNumber;
    }

    /**
     * @param clueNumber the clueNumber to set
     */
    public void setClueNumber(String clueNumber) {
        this.clueNumber = clueNumber;
    }

    /**
     * @return the responder
     */
    public String getResponder() {
        return responder;
    }

    /**
     * @return if the current box is blank
     */
    public boolean isBlank() { return BLANK.equals(getResponse()); }

    public void setBlank() { setResponse(BLANK); }

    /**
     * @returns true if box is part of the clue
     */
    public boolean isPartOf(ClueID clueId) {
        return cluePositions.containsKey(clueId);
    }

    public boolean isPartOf(Clue clue) {
        if (clue == null)
            return false;
        return isPartOf(clue.getClueID());
    }

    /**
     * The clue ids that have this box in their zones
     *
     * Set will iterate in ClueID order.
     */
    public NavigableSet<ClueID> getIsPartOfClues() {
        return cluePositions.navigableKeySet();
    }

    /**
     * True if this box belongs to at least one clue
     */
    public boolean isPartOfClues() {
        return !getIsPartOfClues().isEmpty();
    }

    /**
     * Get a clue that this box is part of from the specified list
     *
     * If there are more than one clues from the same list, returns
     * first in ClueID order
     *
     * Null returned if no clue
     */
    public ClueID getIsPartOfClue(String listName) {
        if (listName == null)
            return null;

        for (ClueID cid : getIsPartOfClues()) {
            if (listName.equals(cid.getListName()))
                return cid;
        }

        return null;
    }

    /**
     * @param position if part of a clue, the position in the
     * word
     */
    public void setCluePosition(ClueID clueId, int position) {
        cluePositions.put(clueId, position);
    }

    /**
     * Get position of box in clue
     *
     * @return postion or -1 if not in clue
     */
    public int getCluePosition(ClueID clueId) {
        Integer pos = cluePositions.get(clueId);
        return (pos == null) ? -1 : pos;
    }

    public Bar getBarTop() { return barTop; }
    public Bar getBarBottom() { return barBottom; }
    public Bar getBarLeft() { return barLeft; }
    public Bar getBarRight() { return barRight; }
    public boolean hasBars() {
        return
            getBarTop() != Bar.NONE
            || getBarBottom() != Bar.NONE
            || getBarLeft() != Bar.NONE
            || getBarRight() != Bar.NONE;
    }
    public boolean isBarredTop() { return barTop != Bar.NONE; }
    public boolean isBarredBottom() { return barBottom != Bar.NONE; }
    public boolean isBarredLeft() { return barLeft != Bar.NONE; }
    public boolean isBarredRight() { return barRight != Bar.NONE; }
    public boolean isSolidBarredTop() { return barTop == Bar.SOLID; }
    public boolean isSolidBarredBottom() { return barBottom == Bar.SOLID; }
    public boolean isSolidBarredLeft() { return barLeft == Bar.SOLID; }
    public boolean isSolidBarredRight() { return barRight == Bar.SOLID; }

    /**
     * 3x3 array of text marks to put in box, can have null entries
     */
    public String[][] getMarks() { return marks; }
    public boolean hasMarks() { return marks != null; }

    /**
     * 3x3 array of text marks to put in box
     *
     * row x col, can have null entries
     */
    public void setMarks(String[][] marks) {
        if (marks != null) {
            if (marks.length != 3) {
                throw new IllegalArgumentException("Marks array must be 3x3.");
            }
            for (int row = 0; row < marks.length; row++) {
                if (marks[row] == null || marks[row].length != 3) {
                    throw new IllegalArgumentException(
                        "Marks array must be 3x3."
                    );
                }
            }
        }
        this.marks = marks;
    }

    public void setBarTop(Bar barTop) {
        this.barTop = barTop;
    }

    public void setBarBottom(Bar barBottom) {
        this.barBottom = barBottom;
    }

    public void setBarLeft(Bar barLeft) {
        this.barLeft = barLeft;
    }

    public void setBarRight(Bar barRight) {
        this.barRight = barRight;
    }

    public boolean hasColor() { return color != NOCOLOR; }

    /**
     * 24-bit 0x00rrggbb when has color
     */
    public int getColor() { return color; }

    public boolean hasTextColor() { return textColor != NOCOLOR; }
    public int getTextColor() { return textColor; }
    public boolean hasBarColor() { return barColor != NOCOLOR; }
    public int getBarColor() { return barColor; }

    /**
     * Set as 24-bit 0x00rrggbb
     */
    public void setColor(int color) { this.color = color; }
    public void setTextColor(int textColor) { this.textColor = textColor; }
    public void setBarColor(int barColor) { this.barColor = barColor; }
}
