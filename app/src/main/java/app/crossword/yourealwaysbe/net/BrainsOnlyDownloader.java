package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.io.BrainsOnlyIO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by keber_000 on 2/9/14.
 */
public class BrainsOnlyDownloader extends AbstractDateDownloader {

    private final DateTimeFormatter df
        = DateTimeFormatter.ofPattern("yyMMdd");

    public BrainsOnlyDownloader(
        String baseUrl, String fullName, String supportUrl
    ) {
        super(
            baseUrl,
            fullName,
            Downloader.DATE_DAILY,
            supportUrl,
            new BrainsOnlyIO());
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return df.format(date);
    }
}
