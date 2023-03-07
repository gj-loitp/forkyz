package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import app.crossword.yourealwaysbe.io.RCIJeuxMFJIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Abstract downloader for RCI Jeux puzzles.
 */
public class AbstractRCIJeuxMFJDateDownloader extends AbstractDateDownloader {
    private static final int ARCHIVE_LENGTH_DAYS = 364;
    private static final DateTimeFormatter titleDateFormat
        = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String sourceUrlFormat;
    private int baseCWNumber;
    private LocalDate baseDate;

    /**
     * Construct an Abstract downloader
     *
     * Note, sourceUrlFormat is not a date format pattern, it should
     * have a %d for the crossword number.
     */
    protected AbstractRCIJeuxMFJDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String sourceUrlFormat,
        String shareUrlFormatPattern,
        int baseCWNumber,
        LocalDate baseDate
    ) {
        super(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            new RCIJeuxMFJIO(),
            null,
            shareUrlFormatPattern
        );
        this.sourceUrlFormat = sourceUrlFormat;
        this.baseDate = baseDate;
        this.baseCWNumber = baseCWNumber;
    }

    @Override
    protected LocalDate getGoodFrom() {
        return LocalDate.now().minusDays(ARCHIVE_LENGTH_DAYS);
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        long cwNumber = getCrosswordNumber(date);
        return String.format(Locale.US, this.sourceUrlFormat, cwNumber);
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ){
        Puzzle puz = super.download(date, headers);
        if (puz != null) {
            puz.setTitle(getCrosswordTitle(date));
        }
        return puz;
    }

    private long getCrosswordNumber(LocalDate date) {
        Duration diff = Duration.between(
            this.baseDate.atStartOfDay(), date.atStartOfDay()
        );
        long delta = Math.floorDiv(
            getDownloadDates().length* (int)diff.toDays(), 7
        );
        return this.baseCWNumber + delta;
    }

    private String getCrosswordTitle(LocalDate date) {
        return getCrosswordNumber(date) + ", " + titleDateFormat.format(date);
    }
}
