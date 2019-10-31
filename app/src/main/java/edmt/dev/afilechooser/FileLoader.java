package edmt.dev.afilechooser;

import android.content.Context;
import android.os.FileObserver;


import androidx.loader.content.AsyncTaskLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edmt.dev.afilechooser.utils.FileUtils;

public class FileLoader extends AsyncTaskLoader<List<File>> {

    private static final int FILE_OBSERVER_MASK = FileObserver.CREATE
            | FileObserver.DELETE | FileObserver.DELETE_SELF
            | FileObserver.MOVED_FROM | FileObserver.MOVED_TO
            | FileObserver.MODIFY | FileObserver.MOVE_SELF;

    private FileObserver mFileObserver;

    private List<File> mData;
    private String mPath;

    public FileLoader(Context context, String path) {
        super(context);
        this.mPath = path;
    }

    @Override
    public List<File> loadInBackground() {

        ArrayList<File> list = new ArrayList<File>();

        // Current directory File instance
        final File pathDir = new File(mPath);

        // List file in this directory with the directory filter
        final File[] dirs = pathDir.listFiles(FileUtils.sDirFilter);
        if (dirs != null) {
            // Sort the folders alphabetically
            Arrays.sort(dirs, FileUtils.sComparator);
            // Add each folder to the File list for the list adapter
            for (File dir : dirs)
                list.add(dir);
        }

        // List file in this directory with the file filter
        final File[] files = pathDir.listFiles(FileUtils.sFileFilter);
        if (files != null) {
            // Sort the files alphabetically
            Arrays.sort(files, FileUtils.sComparator);
            // Add each file to the File list for the list adapter
            for (File file : files)
                list.add(file);
        }

        return list;
    }

    @Override
    public void deliverResult(List<File> data) {
        if (isReset()) {
            onReleaseResources(data);
            return;
        }

        List<File> oldData = mData;
        mData = data;

        if (isStarted())
            super.deliverResult(data);

        if (oldData != null && oldData != data)
            onReleaseResources(oldData);
    }

    @Override
    protected void onStartLoading() {
        if (mData != null)
            deliverResult(mData);

        if (mFileObserver == null) {
            mFileObserver = new FileObserver(mPath, FILE_OBSERVER_MASK) {
                @Override
                public void onEvent(int event, String path) {
                    onContentChanged();
                }
            };
        }
        mFileObserver.startWatching();

        if (takeContentChanged() || mData == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (mData != null) {
            onReleaseResources(mData);
            mData = null;
        }
    }

    @Override
    public void onCanceled(List<File> data) {
        super.onCanceled(data);

        onReleaseResources(data);
    }

    protected void onReleaseResources(List<File> data) {

        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }
}