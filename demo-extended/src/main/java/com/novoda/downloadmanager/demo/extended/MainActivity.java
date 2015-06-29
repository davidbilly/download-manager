package com.novoda.downloadmanager.demo.extended;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.novoda.downloadmanager.DownloadManagerBuilder;
import com.novoda.downloadmanager.demo.R;
import com.novoda.downloadmanager.lib.DownloadManager;
import com.novoda.downloadmanager.lib.NotificationVisibility;
import com.novoda.downloadmanager.lib.Query;
import com.novoda.downloadmanager.lib.Request;
import com.novoda.downloadmanager.lib.RequestBatch;

import java.util.List;

public class MainActivity extends AppCompatActivity implements QueryForDownloadsAsyncTask.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BIG_FILE = "http://download.thinkbroadband.com/50MB.zip";
    private static final String BEARD_IMAGE = "http://i.imgur.com/9JL2QVl.jpg";

    private DownloadManager downloadManager;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.novoda.notils.logger.simple.Log.setShowLogs(true);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.main_downloads_list);
        downloadManager = DownloadManagerBuilder.from(this)
                .withVerboseLogging()
                .build(getContentResolver());

        findViewById(R.id.single_download_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(@NonNull View v) {
                        enqueueSingleDownload();
                    }
                });

        findViewById(R.id.batch_download_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(@NonNull View v) {
                        enqueueBatch();
                    }
                });

        setupQueryingExample();

        IntentFilter batchIntentFilter = new IntentFilter(DownloadManager.ACTION_BATCH_COMPLETE);
        registerReceiver(batchCompletedReceiver, batchIntentFilter);
    }

    private final BroadcastReceiver batchCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long batchId = intent.getLongExtra(DownloadManager.EXTRA_BATCH_ID, -1);
            Toast.makeText(context, "Batch completed with id: " + batchId, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "BATCH COMPLETED: " + batchId);
        }
    };

    private void enqueueSingleDownload() {
        Uri uri = Uri.parse(BIG_FILE);
        final Request request = new Request(uri)
                .setTitle("A Single Beard")
                .setDescription("Fine facial hair")
                .setBigPictureUrl(BEARD_IMAGE)
                .setDestinationInInternalFilesDir(Environment.DIRECTORY_MOVIES, "example.beard")
                .setNotificationVisibility(NotificationVisibility.ACTIVE_OR_COMPLETE);

        long requestId = downloadManager.enqueue(request);
        Log.d(TAG, "Download enqueued with request ID: " + requestId);
    }

    private void enqueueBatch() {
        final RequestBatch batch = new RequestBatch.Builder()
                .withTitle("Large Beard Shipment")
                .withDescription("Goatees galore")
                .withBigPictureUrl(BEARD_IMAGE)
                .withVisibility(NotificationVisibility.ACTIVE_OR_COMPLETE)
                .build();

        Uri uri = Uri.parse(BIG_FILE);
        final Request request = new Request(uri);
        request.setDestinationInInternalFilesDir(Environment.DIRECTORY_MOVIES, "beard.shipment");
        request.setExtra("beard_1");

        batch.addRequest(request);
        request.setExtra("beard_2");
        batch.addRequest(request);
        long batchId = downloadManager.enqueue(batch);
        Log.d(TAG, "Download enqueued with batch ID: " + batchId);

    }

    private void setupQueryingExample() {
        queryForDownloads();
        findViewById(R.id.main_refresh_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(@NonNull View v) {
                        queryForDownloads();
                    }
                });
        listView.setEmptyView(findViewById(R.id.main_no_downloads_view));
    }

    private void queryForDownloads() {
        QueryForDownloadsAsyncTask.newInstance(downloadManager, this).execute(new Query());
    }

    @Override
    public void onQueryResult(List<Download> downloads) {
        listView.setAdapter(new DownloadAdapter(downloads));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(batchCompletedReceiver);
        super.onDestroy();
    }
}
