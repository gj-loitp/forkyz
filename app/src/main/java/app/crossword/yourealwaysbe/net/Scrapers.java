package app.crossword.yourealwaysbe.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;

import app.crossword.yourealwaysbe.BrowseActivity;
import app.crossword.yourealwaysbe.PlayActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

import java.util.ArrayList;
import java.util.Set;

public class Scrapers {
    private ArrayList<PageScraper> scrapers = new ArrayList<PageScraper>();
    private Context context;
    private NotificationManager notificationManager;
    private boolean supressMessages;

    public Scrapers(SharedPreferences prefs, NotificationManager notificationManager, Context context) {
        this.notificationManager = notificationManager;
        this.context = context;

        if (prefs.getBoolean("scrapeCru", false)) {
            scrapers.add(new PageScraper(
                // certificate doesn't seem to work for me
                // "https://theworld.com/~wij/puzzles/cru/index.html",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
                "Cryptic Cru Workshop Archive",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html"
            ));
        }

        if (prefs.getBoolean("scrapeKegler", false)) {
            scrapers.add(new PageScraper(
                "https://kegler.gitlab.io/Block_style/index.html",
                "Kegler's Kryptics",
                "https://kegler.gitlab.io/"
            ));
        }

        this.supressMessages = prefs.getBoolean("supressMessages", false);
    }

    public void scrape() {
        int i = 1;
        String contentTitle = context.getString(R.string.puzzles_scraping);

        NotificationCompat.Builder not
            = new NotificationCompat.Builder(
                    context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
                )
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(contentTitle)
                .setWhen(System.currentTimeMillis());

        for (PageScraper scraper : scrapers) {
            try {
                String contentText = context.getString(
                    R.string.puzzles_downloading_from, scraper.getSourceName()
                );
                Intent notificationIntent
                    = new Intent(context, PlayActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent,
                    AndroidVersionUtils
                        .Factory.getInstance().immutablePendingIntentFlag()
                );
                not.setContentText(contentText).setContentIntent(contentIntent);

                if (!this.supressMessages && this.notificationManager != null) {
                    this.notificationManager.notify(0, not.build());
                }

                Set<String> downloaded = scraper.scrape();

                if (!this.supressMessages) {
                    for (String name : downloaded) {
                        postDownloadedNotification(i++, name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }
    }

    public void supressMessages(boolean b) {
        this.supressMessages = b;
    }

    private void postDownloadedNotification(int i, String name) {
        Intent notificationIntent = new Intent(
            Intent.ACTION_EDIT, null, context, BrowseActivity.class
        );
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            AndroidVersionUtils
                .Factory.getInstance().immutablePendingIntentFlag()
        );

        Notification not = new NotificationCompat.Builder(
                context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
            )
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(
                R.string.puzzle_downloaded, name
            ))
            .setContentIntent(contentIntent)
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }
}
