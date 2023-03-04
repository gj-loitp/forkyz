
package app.crossword.yourealwaysbe.view;

import android.content.Context;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;

public class PlayboardTextRenderer {
    public static String getLongClueText(
        Context context, Clue clue, boolean showCount
    ) {
        if (clue == null)
            return context.getString(R.string.unknown_hint);

        int wordLen = clue.hasZone() ? clue.getZone().size() : -1;

        if (showCount && wordLen >= 0) {
            if (clue.hasClueNumber()) {
                return context.getString(
                    R.string.clue_format_long_with_count,
                    clue.getClueID().getListName(),
                    clue.getClueNumber(),
                    clue.getHint(),
                    wordLen
                );
            } else {
                return context.getString(
                    R.string.clue_format_long_no_num_with_count,
                    clue.getClueID().getListName(),
                    clue.getHint(),
                    wordLen
                );
            }
        } else {
            if (clue.hasClueNumber()) {
                return context.getString(
                    R.string.clue_format_long,
                    clue.getClueID().getListName(),
                    clue.getClueNumber(),
                    clue.getHint()
                );
            } else {
                return context.getString(
                    R.string.clue_format_long_no_num,
                    clue.getClueID().getListName(),
                    clue.getHint()
                );
            }
        }
    }

    public static CharSequence getAccessibleCurrentClueWord(
        Context context, Playboard board, boolean showCount
    ) {
        if (board == null)
            return null;

        CharSequence clue = getAccessibleCurrentClue(
            context, board, showCount
        );

        CharSequence word = getAccessibleWordDescription(
            context, board, board.getCurrentWord()
        );

        if (clue == null && word == null) {
            return null;
        } else if (word == null) {
            return clue;
        } else if (clue == null) {
            return word;
        } else {
            return context.getString(
                R.string.announce_clue_word_response, clue, word
            );
        }
    }

    public static CharSequence getAccessibleCurrentClue(
        Context context, Playboard board, boolean showCount
    ) {
        if (board == null)
            return null;

        String clue = getLongClueText(context, board.getClue(), showCount);

        return (clue == null)
            ? null
            : context.getString(R.string.announce_clue, clue);
    }


    public static CharSequence getAccessibleCurrentBox(
        Context context, Playboard board
    ) {
        if (board == null)
            return null;

        Box box = board.getCurrentBox();
        Word word = board.getCurrentWord();
        Position pos = board.getHighlightLetter();

        int index = word.indexOf(pos);

        boolean firstBox = (index == 0);
        boolean lastBox = (index == word.getLength() - 1);

        return getAccessibleBoxDescription(
            context, board.getCurrentBox(), firstBox, lastBox
        );
    }

    public static CharSequence getAccessibleCurrentWord(
        Context context, Playboard board
    ) {
        if (board == null)
            return null;
        return getAccessibleWordDescription(
            context, board, board.getCurrentWord()
        );
    }

    /**
     * Description of box
     *
     * @param firstBox is the first box of highlighted clue
     * @param lastBox is the last box of highlighted clue
     */
    public static CharSequence getAccessibleBoxDescription(
        Context context, Box box, boolean firstBox, boolean lastBox
    ) {
        if (box == null)
            return null;

        String response = getAccessibleBoxResponseText(
            box, context.getString(R.string.cur_box_blank)
        );

        if (firstBox && lastBox) {
            return context.getString(
                R.string.announce_only_box_response, response
            );
        } else if (firstBox) {
            return context.getString(
                R.string.announce_first_box_response, response
            );
        } else if (lastBox) {
            return context.getString(
                R.string.announce_last_box_response, response
            );
        } else {
            return context.getString(R.string.announce_box_response, response);
        }
    }

    public static CharSequence getAccessibleWordDescription(
        Context context, Playboard board, Word word
    ) {
        if (word == null || board == null)
            return null;

        Box[] boxes = board.getWordBoxes(word);
        if (boxes == null)
            return null;

        return getAccessibleBoxesDescription(context, boxes);
    }

    public static CharSequence getAccessibleBoxesDescription(
        Context context, Box[] boxes
    ) {
        if (boxes == null)
            return null;

        String announceText = isAllBlank(boxes)
            ? context.getString(R.string.word_all_blank)
            : getAccessibleBoxesResponseText(context, boxes);

        return context.getString(R.string.announce_word_response, announceText);
    }

    private static String getAccessibleBoxesResponseText(
        Context context, Box[] boxes
    ) {
        String blank = context.getString(R.string.cur_box_blank);
        StringBuilder responseText = new StringBuilder();
        if (boxes != null) {
            for (Box box : boxes) {
                if (responseText.length() > 0)
                    responseText.append(" ");
                responseText.append(
                    getAccessibleBoxResponseText(box, blank)
                );
            }
        }
        return responseText.toString();
    }

    private static String getAccessibleBoxResponseText(
        Box box, String blankText
    ) {
        if (box == null)
            return null;
        else if (box.isBlank())
            return blankText;
        else
            return box.getResponse();
    }

    private static boolean isAllBlank(Box[] boxes) {
        for (Box box : boxes) {
            if (!Box.isBlock(box) && !box.isBlank())
                return false;
        }
        return true;
    }
}
