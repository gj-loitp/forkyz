package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Jonesin' Crosswords Downloader
 * URL: http://herbach.dnsalias.com/Jonesin/jzYYMMDD.puz
 * Date = Thursdays
 */
public class JonesinDownloader extends AbstractDownloader {
    private static final String NAME = "Jonesin' Crosswords";
    NumberFormat nf = NumberFormat.getInstance();

    public JonesinDownloader() {
        super("http://herbach.dnsalias.com/Jonesin/", getStandardDownloadDir(), NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_THURSDAY;
    }

    public String getName() {
        return NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return super.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "jz" + (date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + FileHandler.FILE_EXT_PUZ;
    }
}
