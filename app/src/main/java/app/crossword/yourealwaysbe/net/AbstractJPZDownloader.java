package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;

import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.JPZIO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

public abstract class AbstractJPZDownloader extends AbstractDownloader {

    protected AbstractJPZDownloader(
        String baseUrl, DirHandle downloadDirectory, String downloaderName
    ) {
        super(baseUrl, downloadDirectory, downloaderName);
    }

    protected Downloader.DownloadResult download(
        LocalDate date, String urlSuffix, Map<String, String> headers
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        DownloadResult jpzResult = download(date, urlSuffix, headers, false);

        if (jpzResult == null)
            return null;

        FileHandle jpzFile = jpzResult.getFileHandle();

        FileHandle puzFile = fileHandler.createFileHandle(
            downloadDirectory,
            this.createFileName(date),
            FileHandler.MIME_TYPE_PUZ
        );
        if (puzFile == null)
            return null;

        boolean success = false;

        try (
            InputStream is = fileHandler.getInputStream(jpzFile);
            DataOutputStream dos
                = new DataOutputStream(fileHandler.getOutputStream(puzFile));
        ) {
            JPZIO.convertJPZPuzzle(is, dos , date);
            fileHandler.delete(jpzFile);
            return new Downloader.DownloadResult(puzFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileHandler.delete(jpzFile);
            if (!success)
                fileHandler.delete(puzFile);
        }

        return null;
    }

    public String createFileName(LocalDate date) {
        return date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth() + "-" +
        this.getName().replaceAll(" ", "") + FileHandler.FILE_EXT_PUZ;
    }

    protected Downloader.DownloadResult download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers,
        boolean canDefer
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        FileHandle f = fileHandler.createFileHandle(
            downloadDirectory,
            this.createFileName(date)+".jpz",
            FileHandler.MIME_TYPE_GENERIC
        );
        if (f == null)
            return null;

        boolean success = false;

        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            LOG.info("Downloading from "+url);

            PuzzleMeta meta = new PuzzleMeta();
            meta.date = date;
            meta.source = getName();
            meta.sourceUrl = url.toString();
            meta.updatable = false;

            Uri fileUri = fileHandler.getUri(f);
            utils.storeMetas(fileHandler.getUri(f), meta, downloadDirectory);

            success = utils.downloadFile(url, f, headers, true, this.getName());

            if (success) {
                utils.removeMetas(fileHandler.getUri(f));
                success = true;
                return new Downloader.DownloadResult(f);
            } else if (canDefer) {
                return Downloader.DownloadResult.DEFERRED_FILE;
            } else {
                utils.removeMetas(fileUri);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!success)
                fileHandler.delete(f);
        }

        return null;
    }
}
