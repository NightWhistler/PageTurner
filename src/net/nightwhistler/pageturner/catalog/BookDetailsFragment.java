/*
 * Copyright (C) 2013 Alex Kuiper
 *
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.nightwhistler.pageturner.catalog;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;
import com.google.inject.Provider;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.activity.ReadingActivity;
import roboguice.inject.InjectView;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Fragment which shows the details of the book to be downloaded.
 */
public class BookDetailsFragment extends RoboSherlockFragment implements LoadFeedCallback {


    @Inject
    private Provider<LoadThumbnailTask> loadThumbnailTaskProvider;

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

    @InjectView(R.id.buyNowButton)
    private Button buyNowButton;

    @InjectView(R.id.firstDivider)
    @Nullable
    private View divider;

    @InjectView(R.id.readNowButton)
    private Button downloadButton;

    @InjectView(R.id.addToLibraryButton)
    private Button addToLibraryButton;

   // @InjectView(R.id.relatedLinksContainer)
   // ViewGroup altLinkParent;

    private int displayDensity;

       private Feed feed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.catalog_download, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        DisplayMetrics metrics = metricsProvider.get();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        this.displayDensity = metrics.densityDpi;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if ( this.feed != null ) {
            doSetFeed(feed);
        }
    }

    private void doSetFeed(Feed feed) {
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

        } else {
            downloadButton.setVisibility(View.GONE);
            addToLibraryButton.setVisibility(View.GONE);
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
        } else {
            buyNowButton.setVisibility(View.GONE);

            if ( divider != null ) {
                divider.setVisibility(View.GONE);
            }

        }


        if (entry.getAuthor() != null) {
            String authorText = String.format(
                    getString(R.string.book_by), entry.getAuthor()
                    .getName());
            authorTextView.setText(authorText);
        } else {
            authorTextView.setText("");
        }

        /*
        altLinkParent.removeAllViews();

        for ( final Link altLink: entry.getAlternateLinks() ) {
            TextView linkTextView = new TextView(getActivity());
            linkTextView.setTextAppearance( getActivity(), android.R.style.TextAppearance_Medium );
            linkTextView.setText( altLink.getTitle() );
            linkTextView.setBackgroundResource(android.R.drawable.list_selector_background );
            linkTextView.setTextColor(R.color.abs__bright_foreground_holo_light);

            linkTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((CatalogParent) getActivity()).loadFeedFromUrl(altLink.getHref());
                }
            } );

            altLinkParent.addView(linkTextView);
        }
        */

        final Link imgLink = Catalog.getImageLink(feed, entry);

        Catalog.loadBookDetails(mainLayout, entry, false);
        icon.setImageDrawable( getActivity().getResources().getDrawable(R.drawable.unknown_cover));

        LoadThumbnailTask task = this.loadThumbnailTaskProvider.get();
        task.setLoadFeedCallback(this);
        task.setBaseUrl(feed.getURL());

        task.execute(imgLink);
    }

    @Override
    public void setNewFeed(Feed feed, ResultType resultType) {
        this.feed = feed;
        if ( this.downloadButton != null ) {
            doSetFeed(feed);
        }
    }

    @Override
    public void errorLoadingFeed(String error) {
        Toast.makeText(getActivity(), error, Toast.LENGTH_LONG ).show();
    }

    @Override
    public void emptyFeedLoaded(Feed feed) {
        errorLoadingFeed( getActivity().getString(R.string.empty_opds_feed) );
    }

    private void setSupportProgressBarIndeterminateVisibility(boolean enable) {
        SherlockFragmentActivity activity = getSherlockActivity();
        if ( activity != null) {
            activity.setSupportProgressBarIndeterminateVisibility(enable);
        }
    }

    public void notifyLinkUpdated( Link link, Drawable drawable ) {

        if ( drawable != null ) {
            icon.setImageDrawable(drawable);
        }

       onLoadingDone();
    }

    @Override
    public void onLoadingStart() {
        setSupportProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onLoadingDone() {
        setSupportProgressBarIndeterminateVisibility(false);
    }

    public void startDownload(final boolean openOnCompletion, final String url) {

        final ProgressDialog downloadDialog = new ProgressDialog(getActivity());

        downloadDialog.setIndeterminate(false);
        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadDialog.setCancelable(true);

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

                downloadDialog.dismiss();

                if ( ! isAdded() ) {
                    return;
                }

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

                downloadDialog.dismiss();

                if ( isAdded() ) {
                    Toast.makeText(getActivity(), R.string.book_failed,
                        Toast.LENGTH_LONG).show();
                }
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
