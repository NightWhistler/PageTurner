package net.nightwhistler.pageturner.activity;

import java.io.File;

public class FileItem {

    private CharSequence label;
    private File file;

    private boolean importOnClick;

    public FileItem( CharSequence label, File file, boolean importOnClick ) {
        this.label = label;
        this.file = file;
        this.importOnClick = importOnClick;
    }

    public CharSequence getLabel() {
        return label;
    }

    public File getFile() {
        return file;
    }

    public boolean isImportOnClick() {
        return importOnClick;
    }
}