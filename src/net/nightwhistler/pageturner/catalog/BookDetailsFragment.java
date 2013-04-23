package net.nightwhistler.pageturner.catalog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;
import com.google.inject.Provider;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.activity.ReadingActivity;
import roboguice.inject.InjectView;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Fragment which shows the details of the book to be downloaded.
 */
public class BookDetailsFragment extends RoboSherlockFragment implements LoadFeedCallback {


    @Inject
    private Provider<LoadFakeFeedTask> loadFakeFeedTaskProvider;

    @Inject
    private Provider<DownloadFileTask> downloadFileTaskProvider;

    @Inject
    private Provider<DisplayMetrics> metricsProvider;

    @InjectView(R.id.mainLayout)
    private View mainLayout;

    @InjectView(R.id.itemAuthor)
    private TextView authorTextView;

    @InjectView(R.id.itemIcon)
    private ImageView icon;

    private Button buyNowButton;

    private Button downloadButton;

    private Button addToLibraryButton;

    private int displayDensity;

    private ProgressDialog downloadDialog;

    private LinkListener linkListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        DisplayMetrics metrics = metricsProvider.get();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int displayDensity = metrics.densityDpi;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.catalog_download, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        //FIXME: move to Activity, this has nothing to do with a fragment

        Intent intent = getActivity().getIntent();
        Uri uri = intent.getData();

        if (uri != null && uri.toString().startsWith("epub://")) {
            String downloadUrl = uri.toString().replace("epub://", "http://");
            startDownload(false, downloadUrl);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        this.downloadDialog = new ProgressDialog(getActivity());

        this.downloadDialog.setIndeterminate(false);
        this.downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.downloadDialog.setCancelable(true);
    }

    public void loadFakeFeed(Entry entry) {
        String base = entry.getFeed().getURL();


        LoadFakeFeedTask task = this.loadFakeFeedTaskProvider.get();
        task.setCallBack(this);
        task.setSingleEntry(entry);

        task.execute(base);
    }

    @Override
    public void setNewFeed(Feed feed, ResultType resultType) {
        //If we're here, the feed always has just 1 entry
        final Entry entry = feed.getEntries().get(0);


        if ( entry.getEpubLink() != null ) {

            String base = feed.getURL();

            try {
                final URL url = new URL(new URL(base), entry.getEpubLink().getHref());

                downloadButton.setOnClickListener( new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        startDownload(true, url.toExternalForm());
                    }
                });

                addToLibraryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startDownload(false, url.toExternalForm());
                    }
                });

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

        }

        if (entry.getBuyLink() != null) {

            buyNowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = entry.getBuyLink().getHref();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });
        }

        if (entry.getAuthor() != null) {
            String authorText = String.format(
                    getString(R.string.book_by), entry.getAuthor()
                    .getName());
            authorTextView.setText(authorText);
        } else {
            authorTextView.setText("");
        }

        final Link imgLink = Catalog.getImageLink(feed, entry);

        Catalog.loadBookDetails(getActivity(), mainLayout, entry, imgLink, false, displayDensity);

        linkListener = new LinkListener() {

            @Override
            public void linkUpdated() {
                Catalog.loadImageLink(getActivity(), icon, imgLink, false, displayDensity);
            }
        };
    }

    @Override
    public void onStop() {
        downloadDialog.dismiss();

        super.onStop();
    }


    @Override
    public void errorLoadingFeed(String error) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public void notifyLinkUpdated() {
        if ( linkListener != null ) {
            linkListener.linkUpdated();
            linkListener = null;
        }
    }

    @Override
    public void onLoadingStart() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onLoadingDone() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void startDownload(final boolean openOnCompletion, final String url) {

        DownloadFileTask.DownloadFileCallback callBack = new DownloadFileTask.DownloadFileCallback() {

            @Override
            public void onDownloadStart() {
                downloadDialog.setMessage(getString(R.string.downloading));
                downloadDialog.show();
            }

            @Override
            public void progressUpdate(long progress, long total, int percentage) {
                downloadDialog.setMax( Long.valueOf(total).intValue() );
                downloadDialog.setProgress(Long.valueOf(progress).intValue());
            }

            @Override
            public void downloadSuccess(File destFile) {

                downloadDialog.hide();

                if ( openOnCompletion ) {
                    Intent intent;

                    intent = new Intent(getActivity().getBaseContext(),
                            ReadingActivity.class);
                    intent.setData(Uri.parse(destFile.getAbsolutePath()));

                    startActivity(intent);
                    getActivity().finish();
                } else {
                    Toast.makeText(getActivity(), R.string.download_complete,
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void downloadFailed() {

                downloadDialog.hide();

                Toast.makeText(getActivity(), R.string.book_failed,
                        Toast.LENGTH_LONG).show();
            }
        };

        final DownloadFileTask task = this.downloadFileTaskProvider.get();

        DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                task.cancel(true);
            }
        };

        downloadDialog.setOnCancelListener(cancelListener);

        task.setCallBack(callBack);
        task.execute(url);

    }


}
