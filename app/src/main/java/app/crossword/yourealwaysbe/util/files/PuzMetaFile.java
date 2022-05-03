
package app.crossword.yourealwaysbe.util.files;

import java.time.LocalDate;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;

public class PuzMetaFile
        implements Comparable<PuzMetaFile> {
    public PuzHandle handle;
    public MetaCache.MetaRecord meta;

    PuzMetaFile(PuzHandle handle, MetaCache.MetaRecord meta) {
        this.handle = handle;
        this.meta = meta;
    }

    public PuzHandle getPuzHandle() { return handle; }

    public int compareTo(PuzMetaFile other) {
        try {
            // because LocalDate is day-month-year, fall back to name
            int dateCmp = other.getDate().compareTo(this.getDate());
            if (dateCmp != 0)
                return dateCmp;
            return getHandler().getName(
                this.handle.getMainFileHandle()
            ).compareTo(
                getHandler().getName(
                    other.handle.getMainFileHandle()
                )
            );
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * True if the objects refer to the same underlying puzzle file
     */
    public boolean isSameMainFile(PuzHandle other) {
        return getPuzHandle().isSameMainFile(other);
    }

    /**
     * True if the objects refer to the same underlying puzzle file
     */
    public boolean isSameMainFile(PuzMetaFile other) {
        return isSameMainFile(other.getPuzHandle());
    }

    public boolean isInDirectory(DirHandle dirHandle) {
        return getPuzHandle().isInDirectory(dirHandle);
    }

    public boolean isUpdatable() {
        return (meta == null) ? false : meta.isUpdatable();
    }

    public String getCaption() {
        String caption = (meta == null) ? "" : meta.getTitle();
        return caption == null ? "" : caption;
    }

    public LocalDate getDate() {
        if (meta == null || meta.getDate() == null) {
            return getHandler().getModifiedDate(handle.getMainFileHandle());
        } else {
            return meta.getDate();
        }
    }

    public int getComplete() {
        return (meta == null)
            ? 0
            : (meta.isUpdatable() ? (-1) : meta.getPercentComplete());
    }

    public int getFilled() {
        return (meta == null)
            ? 0
            : (meta.isUpdatable() ? (-1) : meta.getPercentFilled());
    }

    public String getSource() {
        return ((meta == null) || (meta.getSource() == null))
            ? "Unknown"
            : meta.getSource();
    }

    public String getTitle() {
        if ((meta == null)
                || (meta.getSource() == null)
                || (meta.getSource().length() == 0)) {
            String fileName = getHandler().getName(handle.getMainFileHandle());
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return meta.getSource();
        }
    }

    public String getAuthor() {
        String author = (meta == null) ? null : meta.getAuthor();
        return (author == null) ? "" : author;
    }

    @Override
    public String toString(){
        return getHandler().getUri(handle.getMainFileHandle()).toString();
    }

    private FileHandler getHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }
}
