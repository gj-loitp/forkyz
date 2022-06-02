package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Wall Street Journal
 * URL: https://mazerlm.home.comcast.net/~mazerlm/wsjYYMMDD.puz
 * Date = Fridays
 */
public class WSJFridayDownloader extends AbstractDateDownloader {
    private static final String NAME
        = ForkyzApplication.getInstance().getString(R.string.wall_street_journal);
    private static final String SUPPORT_URL = "https://subscribe.wsj.com";
    NumberFormat nf = NumberFormat.getInstance();

    public WSJFridayDownloader() {
        super(
            "https://herbach.dnsalias.com/wsj/",
            NAME,
            DATE_FRIDAY,
            SUPPORT_URL,
            new IO()
        );
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "wsj" + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) +
        FileHandler.FILE_EXT_PUZ;
    }
}
