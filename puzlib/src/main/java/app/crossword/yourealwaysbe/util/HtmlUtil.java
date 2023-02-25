
package app.crossword.yourealwaysbe.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class HtmlUtil {
    // IPuz tags not to strip from HTML (preserve line breaks)
    private static final Whitelist JSOUP_CLEAN_WHITELIST = new Whitelist();
    static {
        JSOUP_CLEAN_WHITELIST.addTags("br");
    }

    // all the colors from
    // https://www.w3schools.com/colors/colors_names.asp
    private static final Map<String, Integer> namedColors = new HashMap<>();
    static {
        namedColors.put("aliceblue", rgb("f0f8ff"));
        namedColors.put("antiquewhite", rgb("faebd7"));
        namedColors.put("aqua", rgb("00ffff"));
        namedColors.put("aquamarine", rgb("7fffd4"));
        namedColors.put("azure", rgb("f0ffff"));
        namedColors.put("beige", rgb("f5f5dc"));
        namedColors.put("bisque", rgb("ffe4c4"));
        namedColors.put("black", rgb("000000"));
        namedColors.put("blanchedalmond", rgb("ffebcd"));
        namedColors.put("blue", rgb("0000ff"));
        namedColors.put("blueviolet", rgb("8a2be2"));
        namedColors.put("brown", rgb("a52a2a"));
        namedColors.put("burlywood", rgb("deb887"));
        namedColors.put("cadetblue", rgb("5f9ea0"));
        namedColors.put("chartreuse", rgb("7fff00"));
        namedColors.put("chocolate", rgb("d2691e"));
        namedColors.put("coral", rgb("ff7f50"));
        namedColors.put("cornflowerblue", rgb("6495ed"));
        namedColors.put("cornsilk", rgb("fff8dc"));
        namedColors.put("crimson", rgb("dc143c"));
        namedColors.put("cyan", rgb("00ffff"));
        namedColors.put("darkblue", rgb("00008b"));
        namedColors.put("darkcyan", rgb("008b8b"));
        namedColors.put("darkgoldenrod", rgb("b8860b"));
        namedColors.put("darkgray", rgb("a9a9a9"));
        namedColors.put("darkgrey", rgb("a9a9a9"));
        namedColors.put("darkgreen", rgb("006400"));
        namedColors.put("darkkhaki", rgb("bdb76b"));
        namedColors.put("darkmagenta", rgb("8b008b"));
        namedColors.put("darkolivegreen", rgb("556b2f"));
        namedColors.put("darkorange", rgb("ff8c00"));
        namedColors.put("darkorchid", rgb("9932cc"));
        namedColors.put("darkred", rgb("8b0000"));
        namedColors.put("darksalmon", rgb("e9967a"));
        namedColors.put("darkseagreen", rgb("8fbc8f"));
        namedColors.put("darkslateblue", rgb("483d8b"));
        namedColors.put("darkslategray", rgb("2f4f4f"));
        namedColors.put("darkslategrey", rgb("2f4f4f"));
        namedColors.put("darkturquoise", rgb("00ced1"));
        namedColors.put("darkviolet", rgb("9400d3"));
        namedColors.put("deeppink", rgb("ff1493"));
        namedColors.put("deepskyblue", rgb("00bfff"));
        namedColors.put("dimgray", rgb("696969"));
        namedColors.put("dimgrey", rgb("696969"));
        namedColors.put("dodgerblue", rgb("1e90ff"));
        namedColors.put("firebrick", rgb("b22222"));
        namedColors.put("floralwhite", rgb("fffaf0"));
        namedColors.put("forestgreen", rgb("228b22"));
        namedColors.put("fuchsia", rgb("ff00ff"));
        namedColors.put("gainsboro", rgb("dcdcdc"));
        namedColors.put("ghostwhite", rgb("f8f8ff"));
        namedColors.put("gold", rgb("ffd700"));
        namedColors.put("goldenrod", rgb("daa520"));
        namedColors.put("gray", rgb("808080"));
        namedColors.put("grey", rgb("808080"));
        namedColors.put("green", rgb("008000"));
        namedColors.put("greenyellow", rgb("adff2f"));
        namedColors.put("honeydew", rgb("f0fff0"));
        namedColors.put("hotpink", rgb("ff69b4"));
        namedColors.put("indianred", rgb("cd5c5c"));
        namedColors.put("indigo", rgb("4b0082"));
        namedColors.put("ivory", rgb("fffff0"));
        namedColors.put("khaki", rgb("f0e68c"));
        namedColors.put("lavender", rgb("e6e6fa"));
        namedColors.put("lavenderblush", rgb("fff0f5"));
        namedColors.put("lawngreen", rgb("7cfc00"));
        namedColors.put("lemonchiffon", rgb("fffacd"));
        namedColors.put("lightblue", rgb("add8e6"));
        namedColors.put("lightcoral", rgb("f08080"));
        namedColors.put("lightcyan", rgb("e0ffff"));
        namedColors.put("lightgoldenrodyellow", rgb("fafad2"));
        namedColors.put("lightgray", rgb("d3d3d3"));
        namedColors.put("lightgrey", rgb("d3d3d3"));
        namedColors.put("lightgreen", rgb("90ee90"));
        namedColors.put("lightpink", rgb("ffb6c1"));
        namedColors.put("lightsalmon", rgb("ffa07a"));
        namedColors.put("lightseagreen", rgb("20b2aa"));
        namedColors.put("lightskyblue", rgb("87cefa"));
        namedColors.put("lightslategray", rgb("778899"));
        namedColors.put("lightslategrey", rgb("778899"));
        namedColors.put("lightsteelblue", rgb("b0c4de"));
        namedColors.put("lightyellow", rgb("ffffe0"));
        namedColors.put("lime", rgb("00ff00"));
        namedColors.put("limegreen", rgb("32cd32"));
        namedColors.put("linen", rgb("faf0e6"));
        namedColors.put("magenta", rgb("ff00ff"));
        namedColors.put("maroon", rgb("800000"));
        namedColors.put("mediumaquamarine", rgb("66cdaa"));
        namedColors.put("mediumblue", rgb("0000cd"));
        namedColors.put("mediumorchid", rgb("ba55d3"));
        namedColors.put("mediumpurple", rgb("9370db"));
        namedColors.put("mediumseagreen", rgb("3cb371"));
        namedColors.put("mediumslateblue", rgb("7b68ee"));
        namedColors.put("mediumspringgreen", rgb("00fa9a"));
        namedColors.put("mediumturquoise", rgb("48d1cc"));
        namedColors.put("mediumvioletred", rgb("c71585"));
        namedColors.put("midnightblue", rgb("191970"));
        namedColors.put("mintcream", rgb("f5fffa"));
        namedColors.put("mistyrose", rgb("ffe4e1"));
        namedColors.put("moccasin", rgb("ffe4b5"));
        namedColors.put("navajowhite", rgb("ffdead"));
        namedColors.put("navy", rgb("000080"));
        namedColors.put("oldlace", rgb("fdf5e6"));
        namedColors.put("olive", rgb("808000"));
        namedColors.put("olivedrab", rgb("6b8e23"));
        namedColors.put("orange", rgb("ffa500"));
        namedColors.put("orangered", rgb("ff4500"));
        namedColors.put("orchid", rgb("da70d6"));
        namedColors.put("palegoldenrod", rgb("eee8aa"));
        namedColors.put("palegreen", rgb("98fb98"));
        namedColors.put("paleturquoise", rgb("afeeee"));
        namedColors.put("palevioletred", rgb("db7093"));
        namedColors.put("papayawhip", rgb("ffefd5"));
        namedColors.put("peachpuff", rgb("ffdab9"));
        namedColors.put("peru", rgb("cd853f"));
        namedColors.put("pink", rgb("ffc0cb"));
        namedColors.put("plum", rgb("dda0dd"));
        namedColors.put("powderblue", rgb("b0e0e6"));
        namedColors.put("purple", rgb("800080"));
        namedColors.put("rebeccapurple", rgb("663399"));
        namedColors.put("red", rgb("ff0000"));
        namedColors.put("rosybrown", rgb("bc8f8f"));
        namedColors.put("royalblue", rgb("4169e1"));
        namedColors.put("saddlebrown", rgb("8b4513"));
        namedColors.put("salmon", rgb("fa8072"));
        namedColors.put("sandybrown", rgb("f4a460"));
        namedColors.put("seagreen", rgb("2e8b57"));
        namedColors.put("seashell", rgb("fff5ee"));
        namedColors.put("sienna", rgb("a0522d"));
        namedColors.put("silver", rgb("c0c0c0"));
        namedColors.put("skyblue", rgb("87ceeb"));
        namedColors.put("slateblue", rgb("6a5acd"));
        namedColors.put("slategray", rgb("708090"));
        namedColors.put("slategrey", rgb("708090"));
        namedColors.put("snow", rgb("fffafa"));
        namedColors.put("springgreen", rgb("00ff7f"));
        namedColors.put("steelblue", rgb("4682b4"));
        namedColors.put("tan", rgb("d2b48c"));
        namedColors.put("teal", rgb("008080"));
        namedColors.put("thistle", rgb("d8bfd8"));
        namedColors.put("tomato", rgb("ff6347"));
        namedColors.put("turquoise", rgb("40e0d0"));
        namedColors.put("violet", rgb("ee82ee"));
        namedColors.put("wheat", rgb("f5deb3"));
        namedColors.put("white", rgb("ffffff"));
        namedColors.put("whitesmoke", rgb("f5f5f5"));
        namedColors.put("yellow", rgb("ffff00"));
        namedColors.put("yellowgreen", rgb("9acd32"));
    }

    /**
     * Remove IPuz HTML from a string
     * @return decoded string or null if value was null
     */
    public static String unHtmlString(String value) {
        if (value == null)
            return null;

        // this is a bit hacky: any break tag is normalised to "\r?\n<br>"
        // by the clean method, we remove the \r\ns and turn <br> into \n
        return StringEscapeUtils.unescapeHtml4(
            Jsoup.clean(value, JSOUP_CLEAN_WHITELIST)
                .replace("\r", "")
                .replace("\n", "")
                .replace("<br>", "\n")
        );
    }

    /**
     * Return IPuz HTML encoding of string
     * @return encoded string or null if value was null
     */
    public static String htmlString(String value) {
        if (value == null)
            return null;

        return StringEscapeUtils.escapeHtml4(value)
            .replace("\r", "")
            .replace("\n", "<br/>")
            .replace("  ", " &nbsp;");
    }

    /**
     * Parse a colour as if it was html
     *
     * Ignores alpha channels since not supported by IPuz (our backend
     * format). Color always positive or 0 if success. I.e. 0x00rrggbb.
     */
    public static int parseHtmlColor(String color) {
        if (color == null || color.isEmpty())
            return -1;

        color = color.trim().toLowerCase();

        if (color.startsWith("#")) {
            try {
                if (color.length() > 4) {
                    return rgb(color.substring(1));
                } else if (color.length() == 4) {
                    String fullColor = ""
                        + color.charAt(1) + color.charAt(1)
                        + color.charAt(2) + color.charAt(2)
                        + color.charAt(3) + color.charAt(3);
                    return rgb(fullColor);
                } else {
                    return -1;
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (color.startsWith("rgb")) {
            // rgb or rgba
            try {
                String[] args = getFunArgs(color);
                if (args == null || args.length < 3)
                    return -1;

                int red = parse255String(args[0]);
                int green = parse255String(args[1]);
                int blue = parse255String(args[2]);

                return (red<<16) | (green<<8) | (blue<<0);
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (color.startsWith("hsl")) {
            // hsl or hsla
            try {
                String[] args = getFunArgs(color);
                if (args == null || args.length < 3)
                    return -1;

                int hue = Math.max(0, Math.min(360, Integer.valueOf(args[0])));
                int sat = parse255String(args[1]);
                int light = parse255String(args[2]);

                return hslToRgb(hue, sat, light);
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            color = color.replaceAll("\\s", "");
            Integer intColor = namedColors.get(color);
            if (intColor != null)
                return intColor;

            // be really generous, search for prefixes if failed.
            for (Map.Entry<String, Integer> entry : namedColors.entrySet()) {
                if (entry.getKey().startsWith(color))
                    return entry.getValue();
            }
            return -1;
        }
    }

    /**
     * Extracts values for args in fun(blah, blah, blah)
     *
     * Returns as many args as there are. Returns null if something goes
     * wrong.
     */
    private static String[] getFunArgs(String color) {
        if (color == null)
            return null;

        int start = color.indexOf("(");
        int end = color.indexOf(")");
        if (start < 0 || end < 0)
            return null;

        return color.substring(start + 1, end).split(",");
    }

    /**
     * Turns a value into 0-255
     *
     * Either 0-255 or x% of 255.
     */
    private static int parse255String(String value) {
        if (value == null)
            throw new NumberFormatException("null is not a number");

        int iValue = 0;

        value = value.trim();
        if (value.endsWith("%")) {
            String num = value.substring(0, value.length() - 1);
            iValue = (int) (Integer.valueOf(num) / 100.0 * 255);
        } else {
            iValue = Integer.valueOf(value);
        }

        return iValue & 0xff;
    }

    /**
     * Convert HSL to RGBA int
     *
     * hue should be 0-360
     * sat, light, alpha 0-255
     * Algorithm from https://en.wikipedia.org/wiki/HSL_and_HSV
     */
    private static int hslToRgb(int hue, int sat, int light) {
        int red = hslFun(0, hue, sat, light);
        int green = hslFun(8, hue, sat, light);
        int blue = hslFun(4, hue, sat, light);
        return (red<<16) | (green<<8) | (blue<<0);
    }

    /**
     * The f from rgb = (f(0), f(8), f(4)) on Wiki
     *
     * See hslToRgba.
     *
     * Returns 0-255. Limits to 0-255 if something is wrong.
     */
    private static int hslFun(int n, int hue, int sat, int light) {
        double h = (double) hue;
        double s = sat / 255.0;
        double l = light / 255.0;

        double a = s * Math.min(l, 1-l);
        double k = (n + h / 30.0) % 12.0;

        double f
            = l - a * Math.max(-1.0, Math.min(k - 3.0, Math.min(9.0 - k, 1.0)));

        return ((int) (f * 255)) & 0xff;
    }

    /**
     * Convert bbbbbb to int color with full alpha
     */
    private static int rgb(String rgb) {
        return Integer.valueOf(rgb, 16);
    }
}
