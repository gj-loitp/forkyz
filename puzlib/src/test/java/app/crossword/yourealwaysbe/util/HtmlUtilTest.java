
package app.crossword.yourealwaysbe.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HtmlUtilTest {

    @Test
    public void testColor() {
        checkColor("#000000", "000000");
        checkColor("black", "000000");
        checkColor("#ffffff", "ffffff");
        checkColor(" WHITe ", "ffffff");
        checkColor("light golden rod", "fafad2");
        checkColor("rgb(255, 0, 51%)", "ff0082");
        checkColor("hsl(64, 35%, 75%)", "d2d5a8");
        checkColor("hsl(120, 64%, 100%)", "ffffff");
        checkColor("hsl(120, 64%, 10%)", "092809");
        // alpha not supported, should be 82 at start of hex
        checkColor("rgba(0, 127, 51%, 0.51)", "007f82");
        checkColor("hsla(64, 35%, 75%, 0.51)", "d2d5a8");
    }

    private void checkColor(String input, String expected) {
        int i = HtmlUtil.parseHtmlColor(input);
        assertEquals(HtmlUtil.parseHtmlColor(input), argb(expected));
    }

    private int argb(String argb) {
        return (int) (Long.valueOf(argb, 16) + 0);
    }
}
