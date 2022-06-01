
package app.crossword.yourealwaysbe.util.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

@TargetApi(24)
public class FileHandlerSAF extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerSAF.class.getCanonicalName());

    public static final String SAF_ROOT_URI_PREF
        = "safRootUri";
    private static final String SAF_CROSSWORDS_URI_PREF
        = "safCrosswordsFolderUri";
    private static final String SAF_ARCHIVE_URI_PREF
        = "safArchiveFolderUri";

    private static final String ARCHIVE_NAME = "archive";
    private static final String TEMP_NAME = "temp";

    private Uri rootUri;
    private Uri crosswordsFolderUri;
    private Uri archiveFolderUri;

    public static class Meta {
        private String name;
        private long lastModified;

        public Meta(String name, long lastModified) {
            this.name = name;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public long getLastModified() { return lastModified; }
    }

    public static boolean isSAFSupported() {
        return AndroidVersionUtils.Factory.getInstance().isSAFSupported();
    }

    /**
     * Construct FileHandler from context and folder URIs
     *
     * Context should be an application context, not an activity that
     * may go out of date.
     *
     * @param rootUri the tree the user has granted permission to
     * @param crosswordsFolderUri the document uri for the crosswords
     * folder
     * @param archiveFolderUri the document uri for the archive folder
     */
    public FileHandlerSAF(
        Context context,
        Uri rootUri,
        Uri crosswordsFolderUri,
        Uri archiveFolderUri
    ) {
        super(context);
        this.rootUri = rootUri;
        this.crosswordsFolderUri = crosswordsFolderUri;
        this.archiveFolderUri = archiveFolderUri;
    }

    @Override
    public DirHandle getCrosswordsDirectory() {
        return new DirHandle(crosswordsFolderUri);
    }

    @Override
    public DirHandle getArchiveDirectory() {
        return new DirHandle(archiveFolderUri);
    }

    @Override
    protected FileHandle getFileHandle(Uri uri) {
        Meta meta = getMetaFromUri(uri);
        if (meta != null)
            return new FileHandle(uri, meta);
        else
            return null;
    }

    @Override
    protected boolean exists(DirHandle dir) {
        return exists(getContentResolver(), dir.getUri());
    }

    @Override
    protected boolean exists(FileHandle file) {
        return exists(getContentResolver(), file.getUri());
    }

    @Override
    protected Iterable<FileHandle> listFiles(DirHandle dir) {
        ContentResolver resolver = getContentResolver();
        Uri dirUri = dir.getUri();
        String dirTreeId = DocumentsContract.getDocumentId(dirUri);
        Uri dirTreeUri = DocumentsContract.buildDocumentUriUsingTree(
            rootUri, dirTreeId
        );

        Uri childrenUri
            = DocumentsContract.buildChildDocumentsUriUsingTree(
                dirTreeUri, dirTreeId
            );

        ArrayList<FileHandle> files = new ArrayList<>();

        try (
            Cursor cursor = resolver.query(
                childrenUri,
                new String[] {
                    Document.COLUMN_DOCUMENT_ID,
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED,
                    Document.COLUMN_MIME_TYPE
                },
                null, null, null
            )
        ) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                long modified = cursor.getLong(2);
                String mimeType = cursor.getString(3);

                if (!Document.MIME_TYPE_DIR.equals(mimeType)) {
                    Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                        dirUri, id
                    );

                    files.add(new FileHandle(uri, new Meta(name, modified)));
                }
            }
        }

        return files;
    }

    @Override
    protected Uri getUri(DirHandle f) { return f.getUri(); }

    @Override
    protected Uri getUri(FileHandle f) { return f.getUri(); }

    @Override
    protected String getName(FileHandle f) {
        return f.getSAFMeta().getName();
    }

    @Override
    protected long getLastModified(FileHandle file) {
        return file.getSAFMeta().getLastModified();
    }

    @Override
    protected void deleteUnsync(FileHandle fileHandle) {
        try {
            DocumentsContract.deleteDocument(
                getContentResolver(),
                fileHandle.getUri()
            );
        } catch (FileNotFoundException e) {
            // seems like our work is done
        } catch (IllegalArgumentException e) {
            // if the file does not exist, this might be thrown since
            // Android cannot determine access permissions
            if (e.getCause() instanceof FileNotFoundException) {
                // ignore
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void moveToUnsync(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        try {
            DocumentsContract.moveDocument(
                getContentResolver(),
                fileHandle.getUri(),
                srcDirHandle.getUri(),
                destDirHandle.getUri()
            );
        } catch (FileNotFoundException | IllegalArgumentException e) {
            LOGGER.severe(
                "Attempt to move " + fileHandle + " to " +
                destDirHandle + " failed."
            );
            e.printStackTrace();
        }
    }

    @Override
    protected OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException {
        try {
            return getContentResolver().openOutputStream(
                fileHandle.getUri(), "wt"
            );
        } catch (IllegalArgumentException e) {
            // happens when e.g. file was deleted, so consider as IO
            throw new IOException(e);
        }
    }

    @Override
    protected InputStream getInputStream(FileHandle fileHandle)
        throws IOException {
        try {
            return getContentResolver().openInputStream(
                fileHandle.getUri()
            );
        } catch (IllegalArgumentException e) {
            // happens when e.g. file was deleted, so consider as IO
            throw new IOException(e);
        }
    }

    @Override
    public boolean isStorageMounted() {
        ContentResolver resolver = getContentResolver();
        try {
            return exists(resolver, crosswordsFolderUri)
                && exists(resolver, archiveFolderUri);
        } catch (UnsupportedOperationException e) {
            LOGGER.severe("Unsupported operation accessing SAF");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isStorageFull() {
        return SAFStorageCalculator.isStorageFull(
            getApplicationContext(), rootUri
        );
    }

    @Override
    protected FileHandle createFileHandle(
        DirHandle dir, String fileName, String mimeType
    ) {
        try {
            Uri uri = DocumentsContract.createDocument(
                getContentResolver(), dir.getUri(), mimeType, fileName
            );
            if (uri != null) {
                return new FileHandle(
                    uri, new Meta(fileName, System.currentTimeMillis())
                );
            }
        } catch (FileNotFoundException e) {
            // fall through
        }

        return null;
    }

    /**
     * Initialise a crosswords directory in the give rootUri
     *
     * This will search the contents of rootUri to see if directories
     * already exist (and use them if so). If not, it will create the
     * required folders.
     *
     * Once this has been called, then readHandlerFromPrefs can be used
     * to retrieve a handler using these directories.
     *
     * @param applicationContext the application context
     * @param rootUri the root permitted folder for storage
     * @return an initiated file handler if successful, else null
     */
    public static FileHandlerSAF initialiseSAFPrefs(
        Context applicationContext, Uri rootUri
    ) {
        try {
            SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(
                    applicationContext
                );
            ContentResolver resolver = applicationContext.getContentResolver();
            String dirId
                = DocumentsContract.getTreeDocumentId(rootUri);
            Uri dirUri
                = DocumentsContract.buildDocumentUriUsingTree(
                    rootUri, dirId
                );

            Uri crosswordsFolderUri = dirUri;
            Uri archiveFolderUri = null;

            // first iterate over directory looking for subdirs with the
            // right name.

            Uri childrenUri
                = DocumentsContract.buildChildDocumentsUriUsingTree(
                    rootUri, dirId
                );

            try (
                Cursor cursor = resolver.query(
                    childrenUri,
                    new String[] {
                        Document.COLUMN_DISPLAY_NAME,
                        Document.COLUMN_MIME_TYPE,
                        Document.COLUMN_DOCUMENT_ID
                    },
                    null, null, null
                )
            ) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String mimeType = cursor.getString(1);
                    String id = cursor.getString(2);

                    if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                        if (ARCHIVE_NAME.equals(name)) {
                            archiveFolderUri
                                = DocumentsContract.buildDocumentUriUsingTree(
                                    rootUri, id
                                );
                        }
                    }
                }
            }

            // if not found, create new

            if (archiveFolderUri == null) {
                archiveFolderUri = DocumentsContract.createDocument(
                    resolver, dirUri, Document.MIME_TYPE_DIR, ARCHIVE_NAME
                );
            }

            // if all ok, save to prefs and keep permission
            if (crosswordsFolderUri != null && archiveFolderUri != null) {

                // persist permissions
                int takeFlags = (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
                resolver.takePersistableUriPermission(rootUri, takeFlags);

                // save locations
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(
                    SAF_ROOT_URI_PREF, rootUri.toString()
                );
                editor.putString(
                    SAF_CROSSWORDS_URI_PREF, crosswordsFolderUri.toString()
                );
                editor.putString(
                    SAF_ARCHIVE_URI_PREF, archiveFolderUri.toString()
                );
                editor.apply();

                return new FileHandlerSAF(
                    applicationContext,
                    rootUri,
                    crosswordsFolderUri, archiveFolderUri
                );
            }
        } catch (Exception e) {
            LOGGER.severe("Unable to (re-)configure SAF directory.");
        }

        return null;
    }

    /**
     * Read handler using locations stored in shared prefs.
     *
     * Requires initialiseSAFPrefs to have been called first. Returns
     * null if the handler could not be created. (E.g. if there are no
     * configured directories.) Will reinitialise if the rootUri is
     * still available.
     */
    public static FileHandlerSAF readHandlerFromPrefs(
        Context applicationContext
    ) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(applicationContext);

        FileHandlerSAF fileHandler = null;

        String rootFolder = prefs.getString(SAF_ROOT_URI_PREF, null);
        String crosswordsFolder
            = prefs.getString(SAF_CROSSWORDS_URI_PREF, null);
        String archiveFolder
            = prefs.getString(SAF_ARCHIVE_URI_PREF, null);

        if (rootFolder != null
                && crosswordsFolder != null
                && archiveFolder != null) {
            Uri rootFolderUri = Uri.parse(rootFolder);
            Uri crosswordsFolderUri = Uri.parse(crosswordsFolder);
            Uri archiveFolderUri = Uri.parse(archiveFolder);

            ContentResolver resolver = applicationContext.getContentResolver();

            try {
                if (exists(resolver, crosswordsFolderUri)
                        && exists(resolver, archiveFolderUri)) {
                    fileHandler = new FileHandlerSAF(
                        applicationContext,
                        rootFolderUri,
                        crosswordsFolderUri, archiveFolderUri
                    );
                }
            } catch (SecurityException e) {
                LOGGER.severe("Permission not granted to configured SAF directories.");
            } catch (UnsupportedOperationException e) {
                LOGGER.severe("Unsupported operation with SAF");
                e.printStackTrace();
            }
        }

        if (fileHandler == null && rootFolder != null) {
            fileHandler = initialiseSAFPrefs(
                applicationContext, Uri.parse(rootFolder)
            );
        }

        return fileHandler;
    }

    private static boolean exists(ContentResolver resolver, Uri uri) {
        try (
            Cursor c = resolver.query(
                uri,
                new String[] {
                    Document.COLUMN_DOCUMENT_ID
                },
                null, null, null
            )
        ) {
            return c.getCount() > 0;
        } catch (IllegalArgumentException e) {
            // if the file does not exist, this is thrown
            return false;
        }
    }

    private Meta getMetaFromUri(Uri uri) {
        try (
            Cursor c = getContentResolver().query(
                uri,
                new String[] {
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED
                },
                null, null, null
            )
        ) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                return new Meta(
                    c.getString(0),
                    // avoid exception crash if last modified is not known
                    // e.g. when opening firefox download urls
                    getLongColumnWithDefault(c, 1, System.currentTimeMillis())
                );
            } else {
                return null;
            }
        }
    }

    private long getLongColumnWithDefault(
        Cursor c, int columnIndex, long defaultValue
    ) {
        try {
            return c.getLong(1);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    private ContentResolver getContentResolver() {
        return getApplicationContext().getContentResolver();
    }
}
