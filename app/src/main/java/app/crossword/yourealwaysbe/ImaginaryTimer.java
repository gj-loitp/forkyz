package app.crossword.yourealwaysbe;

import java.text.NumberFormat;


public class ImaginaryTimer {
    private static final long SECONDS = 1000L;
    private static final long MINUTES = 60L * SECONDS;
    private final NumberFormat format = NumberFormat.getInstance();
    private boolean running = false;
    private long elapsed;
    private long incept;

    public ImaginaryTimer(long elapsed) {
        this.elapsed = elapsed;
        this.format.setMinimumIntegerDigits(2);
    }

    public long getElapsed() {
        return this.running ? ((System.currentTimeMillis() - this.incept) + elapsed) : this.elapsed;
    }

    public void start() {
        this.incept = System.currentTimeMillis();
        running = true;
    }

    public void stop() {
        this.elapsed += (System.currentTimeMillis() - this.incept);
        running = false;
    }

    public String time() {
        long elapsed = this.getElapsed();
        long mins = elapsed / MINUTES;
        long secs = (elapsed - (MINUTES * mins)) / SECONDS;

        return new StringBuilder(Long.toString(mins)).append(":")
                                                     .append(this.format.format(secs))
                                                     .toString();
    }
}
