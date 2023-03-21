package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.HtmlUtil;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the JPZ Crossword Compiler XML format.
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tested.
 *
 * Acrostic format is an extension from Alex Boisvert
 *
 * The (supported) XML format is:
 *
 * <crossword-compiler>
 *   <rectangular-puzzle>
 *     <metadata>
 *       <title>[Title]</title>
 *       <creator>[Author]</creator>
 *       <copyright>[Copyright]</copyright>
 *       <description>[Description]</description>
 *     </metadata>
 *     <crossword> or <acrostic>
 *       <grid width="[width]" height="[height]">
 *         <cell x="[x]" y="[y]" solution="[letter]" ?number="[number]"/>
 *         <cell x="[x]" y="[y]" type="block" .../>
 *         ...
 *       </grid>
 *       <clues ordering="normal">
 *         <title><b>Across [or] Down</b></title>
 *         <clue number="[number]" format="[length]" citation="[explanation]">
 *           [clue]
 *         </clue>
 *         <clue number="[number]" is-link="[ordering num]">
 *           [clue]
 *         </clue>
 *       </clues>
 *     </crossword> or </acrostic>
 *   </rectangular-puzzle>
 * </crossword-compiler>
 *
 * Other cell attributes include background-color, background-shape,
 * solve-state, top|left|right|bottom-bar, top-right-number. Also
 * assuming top-left-number, top-number, left-number,
 * bottom-right-number &c. are valid (no
 * examples found in wild).
 *
 * Does not unzip the JPZ, use StreamUnits.unzipOrPassThrough if needed.
 */
public class JPZIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger("app.crossword.yourealwaysbe");

    public static class JPZIOException extends Exception {
        public JPZIOException(String msg) { super(msg); }
    }

    private static final String ACROSTIC_QUOTE_LISTNAME = "Quote";
    private static final String ACROSTIC_QUOTE_HINT = "Quote";

    private static class ClueInfo extends ClueID {
        private String clueNumber;
        private String hint;
        private String zoneID;
        private String citation;

        public ClueInfo(
            String listName, int index,
            String clueNumber, String hint, String zoneID,
            String citation
        ) {
            super(listName, index);
            this.clueNumber = clueNumber;
            this.hint = hint;
            this.zoneID = zoneID;
            this.citation = citation;
        }

        public String getClueNumber() { return clueNumber; }
        public String getHint() { return hint; }
        public String getZoneID() { return zoneID; }
        public String getCitation() { return citation; }
    }

    private static final String[][] markAttributes = new String[][] {
        new String[] {
            "top-left-number", "top-number", "top-right-number"
        },
        new String[] {
            "left-number", "center-number", "right-number"
        },
        new String[] {
            "bottom-left-number", "bottom-number", "bottom-right-number"
        }
    };

    private static class JPZXMLParser extends DefaultHandler {
        private String title = "";
        private String creator = "";
        private String copyright = "";
        private String description = "";
        private String completion;
        private String instructions;
        private int width;
        private int height;
        private Box[][] boxes;
        private boolean acrostic = false;
        private List<ClueInfo> clues = new LinkedList<>();
        private Map<String, Zone> zoneMap = new HashMap<>();
        private StringBuilder charBuffer = new StringBuilder();

        // sanity checks
        private boolean hasRectangularPuzzleEle = false;
        private boolean hasGridEle = false;
        private boolean hasCluesEle = false;

        public String getTitle() { return title; }
        public String getCreator() { return creator; }
        public String getCopyright() { return copyright; }
        public String getDescription() { return description; }
        public String getInstructions() { return instructions; }
        public String getCompletion() { return completion; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public Box[][] getBoxes() { return boxes; }
        public List<ClueInfo> getClues() { return clues; }
        public Map<String, Zone> getZoneMap() { return zoneMap; }
        public boolean isAcrostic() { return acrostic; }

        /**
         * Best assessment of whether read succeeded (i.e. was a JPZ
         * file)
         */
        public boolean isSuccessfulRead() {
            return hasRectangularPuzzleEle
                && hasGridEle
                && hasCluesEle
                && getWidth() > 0
                && getHeight() > 0
                && (getClues().size() > 0);
        }

        // Use several handlers to maintain three different modes:
        // outerXML, inGrid, and inClues

        private DefaultHandler outerXML = new DefaultHandler() {
            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("title")
                        || name.equalsIgnoreCase("creator")
                        || name.equalsIgnoreCase("copyright")
                        || name.equalsIgnoreCase("description")
                        || name.equalsIgnoreCase("instructions")
                        || name.equalsIgnoreCase("completion")) {
                    charBuffer.delete(0, charBuffer.length());
                } else {
                    charBuffer.append("<" + tagName + ">");
                }
            }

            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                charBuffer.append(ch, start, length);
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                String charData = charBuffer.toString().trim();

                if (name.equalsIgnoreCase("title")) {
                    title = charData;
                } else if (name.equalsIgnoreCase("creator")) {
                    creator = charData;
                } else if (name.equalsIgnoreCase("copyright")) {
                    copyright = charData;
                } else if (name.equalsIgnoreCase("description")) {
                    description = charData;
                } else if (name.equalsIgnoreCase("instructions")) {
                    instructions = charData;
                } else if (name.equalsIgnoreCase("completion")) {
                    completion = charData;
                } else {
                    charBuffer.append("</" + tagName + ">");
                }
            }
        };

        private DefaultHandler inGrid = new DefaultHandler() {
            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                try {
                    if (name.equalsIgnoreCase("grid")) {
                        JPZXMLParser.this.width
                            = Integer.parseInt(attributes.getValue("width"));
                        JPZXMLParser.this.height
                            = Integer.parseInt(attributes.getValue("height"));
                        JPZXMLParser.this.boxes = new Box[height][width];
                    } else if (name.equalsIgnoreCase("cell")) {
                        parseCell(attributes);
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read JPZ XML cell data: " + e);
                }
            }

            private void parseCell(Attributes attributes) {
                int x = Integer.parseInt(attributes.getValue("x")) - 1;
                int y = Integer.parseInt(attributes.getValue("y")) - 1;

                if (
                    0 <= x && x < JPZXMLParser.this.getWidth()
                    && 0 <= y && y < JPZXMLParser.this.getHeight()
                ) {
                    Box box = new Box();

                    // keep track of whether the cell is interesting
                    // uninteresting blocks get set to null
                    boolean hasData = false;
                    if (!isCell(attributes))
                        box.setBlock(true);

                    String solution = attributes.getValue("solution");
                    if (
                        solution != null && solution.length() > 0
                        && !Box.isBlock(box)
                    ) {
                        box.setSolution(solution);
                    }

                    String response = attributes.getValue("solve-state");
                    if (response != null && response.length() > 0) {
                        box.setInitialValue(response);
                        box.setResponse(response);
                        hasData = true;
                    } else {
                        box.setBlank();
                    }

                    String number = attributes.getValue("number");
                    if (number != null) {
                        box.setClueNumber(number);
                        hasData = true;
                    }

                    String shape
                        = attributes.getValue("background-shape");
                    if ("circle".equalsIgnoreCase(shape)) {
                        box.setShape(Box.Shape.CIRCLE);
                        hasData = true;
                    }

                    String color
                        = attributes.getValue("background-color");
                    if (color != null) {
                        int iColor = HtmlUtil.parseHtmlColor(color);
                        if (iColor >= 0) {
                            box.setColor(iColor);
                            hasData = true;
                        }
                    }

                    String topBar = attributes.getValue("top-bar");
                    if ("true".equalsIgnoreCase(topBar))
                        box.setBarTop(Box.Bar.SOLID);
                    String bottomBar = attributes.getValue("bottom-bar");
                    if ("true".equalsIgnoreCase(bottomBar))
                        box.setBarBottom(Box.Bar.SOLID);
                    String leftBar = attributes.getValue("left-bar");
                    if ("true".equalsIgnoreCase(leftBar))
                        box.setBarLeft(Box.Bar.SOLID);
                    String rightBar = attributes.getValue("right-bar");
                    if ("true".equalsIgnoreCase(rightBar))
                        box.setBarRight(Box.Bar.SOLID);
                    hasData |= box.hasBars();

                    boolean hasMarks = false;
                    String[][] marks = new String[3][3];
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            String mark = attributes.getValue(
                                markAttributes[row][col]
                            );
                            if (mark != null) {
                                marks[row][col] = mark;
                                hasMarks = true;
                            }
                        }
                    }
                    if (hasMarks) {
                        box.setMarks(marks);
                        hasData = true;
                    }

                    if (!box.isBlock() || hasData)
                        JPZXMLParser.this.boxes[y][x] = box;
                }
            }
        };

        private boolean isCell(Attributes attributes) {
            String cellType = attributes.getValue("type");
            if ("block".equalsIgnoreCase(cellType))
                return false;
            if ("void".equalsIgnoreCase(cellType))
                return false;
            if ("clue".equalsIgnoreCase(cellType))
                return false;
            return true;
        }

        private DefaultHandler inClues = new DefaultHandler() {
            private String inClueNum = null;
            private String inClueFormat = "";
            private String inListName = "No List";
            private String inClueZoneID = null;
            private String inClueCitation = null;
            private int inClueIndex = -1;

            private StringBuilder charBuffer = new StringBuilder();

            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim()
                    : strippedName;

                try {
                    if (name.equalsIgnoreCase("title")) {
                        charBuffer.delete(0, charBuffer.length());
                    } else if (name.equalsIgnoreCase("clue")) {
                        charBuffer.delete(0, charBuffer.length());

                        inClueNum = attributes.getValue("number");
                        inClueIndex += 1;

                        String link = attributes.getValue("is-link");
                        if (link == null) {
                            inClueFormat = attributes.getValue("format");
                            if (inClueFormat == null)
                                inClueFormat = "";

                            inClueCitation = attributes.getValue("citation");
                            inClueZoneID = attributes.getValue("word");

                            // clue appears in characters between start
                            // and end
                        }
                    } else {
                        charBuffer.append("<" + tagName + ">");
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read JPZ XML cell data: " + e);
                }
            }

            @Override
            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                charBuffer.append(ch, start, length);
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim()
                    : strippedName;

                if (name.equalsIgnoreCase("title")) {
                    inListName = HtmlUtil.unHtmlString(charBuffer.toString());
                    inClueIndex = -1;
                } else if (name.equalsIgnoreCase("clue")) {
                    String fullClue = charBuffer.toString();

                    if (inClueFormat.length() > 0) {
                        fullClue = String.format(
                            "%s (%s)", fullClue, inClueFormat
                        );
                    }

                    clues.add(
                        new ClueInfo(
                            inListName, inClueIndex, inClueNum,
                            fullClue, inClueZoneID,
                            inClueCitation
                        )
                    );

                    inClueNum = null;
                    inClueFormat = "";
                    inClueZoneID = null;
                    inClueCitation = null;
                } else {
                    charBuffer.append("</" + tagName + ">");
                }
            }
        };

        private DefaultHandler inWord = new DefaultHandler() {
            private String zoneID;
            private Zone zone;

            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("word")) {
                    zoneID = attributes.getValue("id");
                    zone = new Zone();

                    String x = attributes.getValue("x");
                    String y = attributes.getValue("y");
                    if (x != null && y != null)
                        parseCells(x, y);
                } else if (name.equalsIgnoreCase("cells")) {
                    parseCells(
                        attributes.getValue("x"),
                        attributes.getValue("y")
                    );
                }
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim()
                    : strippedName;

                if (name.equalsIgnoreCase("word")) {
                    if (zoneID != null)
                        zoneMap.put(zoneID, zone);
                    zoneID = null;
                    zone = null;
                }
            }

            /**
             * Parse cells data into zone
             *
             * E.g. x="1-3" y = "2" is (2, 1), (2, 2), (2, 3);
             */
            private void parseCells(String x, String y) {
                String[] xs = x.split("-");
                int xstart = Integer.valueOf(xs[0]) - 1;
                int xend = (xs.length > 1)
                    ? Integer.valueOf(xs[1]) - 1
                    : xstart;

                String[] ys = y.split("-");
                int ystart = Integer.valueOf(ys[0]) - 1;
                int yend = (ys.length > 1)
                    ? Integer.valueOf(ys[1]) - 1
                    : ystart;

                for (int row = ystart; row <= yend; row++)
                    for (int col = xstart; col <= xend; col++)
                        zone.addPosition(new Position(row, col));
            }
        };

        private DefaultHandler state = outerXML;

        @Override
        public void startElement(String nsURI,
                                 String strippedName,
                                 String tagName,
                                 Attributes attributes) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

            if (name.equalsIgnoreCase("rectangular-puzzle")) {
                hasRectangularPuzzleEle = true;
            } else if (name.equalsIgnoreCase("grid")) {
                hasGridEle = true;
                state = inGrid;
            } else if (name.equalsIgnoreCase("clues")) {
                hasCluesEle = true;
                state = inClues;
            } else if (name.equalsIgnoreCase("word")) {
                state = inWord;
            } else if (name.equalsIgnoreCase("acrostic")) {
                acrostic = true;
            }

            state.startElement(nsURI, name, tagName, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            state.characters(ch, start, length);
        }

        @Override
        public void endElement(String nsURI,
                               String strippedName,
                               String tagName) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

            state.endElement(nsURI, strippedName, tagName);

            if (name.equalsIgnoreCase("grid")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("clues")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("word")) {
                state = outerXML;
            }
        }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader xr = parser.getXMLReader();
        JPZXMLParser handler = new JPZXMLParser();
        xr.setContentHandler(handler);
        xr.parse(new InputSource(is));

        if (!handler.isSuccessfulRead())
            return null;

        if (handler.isAcrostic() || looksLikeAnAcrostic(handler)) {
            Puzzle puz = buildAcrostic(handler);
            return (puz == null) ? buildCrossword(handler) : puz;
        } else {
            return buildCrossword(handler);
        }
    }

    public static boolean convertPuzzle(InputStream is,
                                        DataOutputStream os,
                                        LocalDate d) {
        try {
            Puzzle puz = readPuzzle(is);
            puz.setDate(d);
            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.severe("Unable to convert JPZ file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Build crossword from completed handler
     */
    private static Puzzle buildCrossword(JPZXMLParser handler) {
        PuzzleBuilder builder = new PuzzleBuilder(handler.getBoxes());
        builder.setTitle(handler.getTitle())
            .setAuthor(handler.getCreator())
            .setCopyright(handler.getCopyright())
            .setIntroMessage(handler.getInstructions())
            .setNotes(handler.getDescription());

        setClues(builder, handler);
        setCompletionMessage(builder, handler);

        return builder.getPuzzle();
    }

    private static void setClues(PuzzleBuilder builder, JPZXMLParser handler) {
        Map<String, Zone> zones = handler.getZoneMap();

        List<ClueInfo> clues = handler.getClues();
        for (int i = 0; i < clues.size(); i++) {
            ClueInfo clue = clues.get(i);
            builder.addClue(new Clue(
                clue.getListName(),
                clue.getIndex(),
                clue.getClueNumber(),
                clue.getHint(),
                zones.get(clue.getZoneID())
            ));
        }
    }

    private static void setCompletionMessage(
        PuzzleBuilder builder, JPZXMLParser handler
    ) {
        StringBuilder fullCompletion = new StringBuilder();

        String completion = handler.getCompletion();
        if (completion != null) {
            fullCompletion.append(completion);
            fullCompletion.append("<br/>");
        }

        // sort lists into order then construct citations text
        Map<String, StringBuilder> listNotes = new HashMap<>();

        for (ClueInfo ci : handler.getClues()) {
            String number = ci.getClueNumber();
            String listName = ci.getListName();
            String citation = ci.getCitation();

            if (citation != null) {
                if (!listNotes.containsKey(listName))
                    listNotes.put(listName, new StringBuilder());

                listNotes.get(listName).append(
                    String.format("<p>%s: %s</p>", number, citation)
                );
            }
        }

        List<String> listNames = new ArrayList<>(listNotes.keySet());
        Collections.sort(listNames);

        for (String listName : listNames) {
            fullCompletion.append("<h1>" + listName + "</h1>");
            fullCompletion.append(listNotes.get(listName).toString());
        }

        builder.setCompletionMessage(fullCompletion.toString());
    }

    /**
     * Build acrostic from completed handler
     *
     * Should work when handler.isAcrostic() is true. Will try anyway.
     * Returns null if it's not possible to read it as an acrostic.
     */
    private static Puzzle buildAcrostic(JPZXMLParser handler) {
        Box[][] boxes = getAcrosticBoxes(handler);
        if (boxes == null)
            return null;

        PuzzleBuilder builder = new PuzzleBuilder(boxes);
        builder.setTitle(handler.getTitle())
            .setAuthor(handler.getCreator())
            .setCopyright(handler.getCopyright())
            .setIntroMessage(handler.getInstructions())
            .setNotes(handler.getDescription())
            .setKind(Puzzle.Kind.ACROSTIC);

        try {
            setAcrosticClues(builder, handler);
        } catch (JPZIOException e) {
            return null;
        }

        setCompletionMessage(builder, handler);

        return builder.getPuzzle();
    }

    /**
     * Find the part of an acrostic grid corresponding to the quote
     *
     * Returns null if not derivable
     */
    private static Box[][] getAcrosticBoxes(JPZXMLParser handler) {
        int lastRow = getAcrosticLastQuoteRow(handler);
        if (lastRow < 0)
            return null;

        Box[][] allBoxes = handler.getBoxes();
        Box[][] boxes = new Box[lastRow + 1][handler.getWidth()];
        for (int row = 0; row <= lastRow; row++) {
            for (int col = 0; col < handler.getWidth(); col++) {
                boxes[row][col] = allBoxes[row][col];
            }
        }

        return boxes;
    }

    /**
     * Guess if JPZ is an acrostic
     *
     * If exported from crossword nexus, the <acrostic> tag is changed to the
     * standard crossword one. So look for other evidence this is actually an
     * acrostic.
     */
    private static boolean looksLikeAnAcrostic(JPZXMLParser handler) {
        return getAcrosticQuoteZone(handler) != null;
    }

    /**
     * JPZ acrostics have quote boxes on top and separate answer boxes
     */
    private static int getAcrosticLastQuoteRow(JPZXMLParser handler) {
        Zone quoteZone = getAcrosticQuoteZone(handler);
        if (quoteZone == null)
            return -1;

        int lastRow = -1;
        for (Position pos : quoteZone) {
            if (lastRow < 0)
                lastRow = pos.getRow();
            else
                lastRow = Math.max(lastRow, pos.getRow());
        }

        return lastRow;
    }

    private static Zone getAcrosticQuoteZone(JPZXMLParser handler) {
        Zone quoteZone = null;
        List<ClueInfo> clues = handler.getClues();
        Map<String, Zone> zones = handler.getZoneMap();

        // look for quote clue, start from end as it's usually last
        ListIterator<ClueInfo> li = clues.listIterator(clues.size());
        while (li.hasPrevious()) {
            ClueInfo clue = li.previous();
            if (isAcrosticQuoteClue(clue)) {
                quoteZone = zones.get(clue.getZoneID());
                break;
            }
        }

        return quoteZone;
    }

    private static boolean isAcrosticQuoteClue(ClueInfo clue) {
        String number = clue.getClueNumber();
        String hint = clue.getHint();
        return (number == null || number.isEmpty())
            && "[QUOTE]".equalsIgnoreCase(hint);
    }

    /**
     * Add the clues to a builder for an acrostic
     *
     * builder must have the quote boxes (but then you can't get a
     * builder without giving the boxes)
     */
    private static void setAcrosticClues(
        PuzzleBuilder builder, JPZXMLParser handler
    ) throws JPZIOException {
        // the clue zones refer to "non-quote" cells, that map to quote
        // cells via the box number.

        Map<String, Zone> zones = handler.getZoneMap();
        Box[][] allBoxes = handler.getBoxes();
        Map<String, Position> numberPositions = builder.getNumberPositions();

        List<ClueInfo> clues = handler.getClues();
        for (int i = 0; i < clues.size(); i++) {
            ClueInfo clue = clues.get(i);
            if (!isAcrosticQuoteClue(clue)) {
                Zone indirectZone = zones.get(clue.getZoneID());
                Zone directZone = new Zone();
                for (Position pos : indirectZone) {
                    int row = pos.getRow();
                    int col = pos.getCol();
                    if (row < 0 || row >= allBoxes.length) {
                        throw new JPZIOException(
                            "Clue postion " + pos + " is outside of boxes."
                        );
                    }

                    if (col < 0 || col >= allBoxes[row].length) {
                        throw new JPZIOException(
                            "Clue postion " + pos + " is outside of boxes."
                        );
                    }

                    Box box = allBoxes[row][col];
                    if (Box.isBlock(box)) {
                        throw new JPZIOException(
                            "Clue contains position "
                            + pos + " which is a block"
                        );
                    }

                    if (!box.hasClueNumber()) {
                        throw new JPZIOException(
                            "Acrostic clue contains position "
                            + pos
                            + " which is a box not linked to the quote via"
                            + " its clue number."
                        );
                    }

                    Position truePos
                        = numberPositions.get(box.getClueNumber());
                    if (truePos == null) {
                        throw new JPZIOException(
                            "Acrostic clue contains position "
                            + pos
                            + " with number "
                            + box.getClueNumber()
                            + " that is not a position in the quote."
                        );
                    }

                    directZone.addPosition(truePos);
                }

                String listName = clue.getListName();
                int index = builder.getNextClueIndex(listName);
                builder.addClue(new Clue(
                    listName,
                    index,
                    null, // use label instead of clue number
                    clue.getClueNumber(),
                    clue.getHint(),
                    directZone
                ));
            } else {
                int index = builder.getNextClueIndex(ACROSTIC_QUOTE_LISTNAME);
                builder.addClue(new Clue(
                    ACROSTIC_QUOTE_LISTNAME,
                    index,
                    null,
                    null,
                    ACROSTIC_QUOTE_HINT,
                    zones.get(clue.getZoneID())
                ));
            }
        }
    }
}
