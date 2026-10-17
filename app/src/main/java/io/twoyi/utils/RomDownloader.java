/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.twoyi.R;

public class RomDownloader {

    private static final String TAG = "RomDownloader";

    public static final String DEFAULT_ROM_URL = "https://github.com/twoyi/twoyi/releases/download/0.5.4/rootfs.7z";

    public interface DownloadCallback {
        void onSuccess(File downloadedFile);
        void onError(String errorMessage);
        void onCancelled();
    }

    private final Activity mActivity;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean mIsCancelled = new AtomicBoolean(false);
    private MaterialDialog mProgressDialog;

    public RomDownloader(Activity activity) {
        this.mActivity = activity;
    }

    public void cancel() {
        mIsCancelled.set(true);
    }

    public void startDownload(String downloadUrl, DownloadCallback callback) {
        if (TextUtils.isEmpty(downloadUrl)) {
            downloadUrl = DEFAULT_ROM_URL;
        }

        mIsCancelled.set(false);

        mProgressDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.os_download_progress_title)
                .content(R.string.extracting_tips)
                .iconRes(R.mipmap.ic_launcher)
                .progress(false, 100, true)
                .cancelable(false)
                .negativeText(android.R.string.cancel)
                .onNegative((dialog, which) -> {
                    cancel();
                    UIHelper.dismiss(dialog);
                    if (callback != null) {
                        callback.onCancelled();
                    }
                })
                .build();

        UIHelper.show(mProgressDialog);

        final String targetUrl = downloadUrl;
        UIHelper.GLOBAL_EXECUTOR.execute(() -> {
            File targetFile = mActivity.getFileStreamPath(RomManager.ROOTFS_NAME);
            File tempFile = new File(mActivity.getCacheDir(), "rootfs_download.tmp");

            HttpURLConnection connection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                String currentUrl = targetUrl;
                int redirects = 0;
                while (redirects < 5) {
                    URL url = new URL(currentUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(true);
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 Twoyi Downloader");
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                            || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                            || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                            || responseCode == 307
                            || responseCode == 308) {
                        String newUrl = connection.getHeaderField("Location");
                        connection.disconnect();
                        if (newUrl != null) {
                            currentUrl = newUrl;
                            redirects++;
                            continue;
                        }
                    }
                    break;
                }

                if (connection == null || connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    int code = connection != null ? connection.getResponseCode() : -1;
                    throw new RuntimeException("HTTP status " + code);
                }

                long totalBytes = connection.getContentLengthLong();
                inputStream = new BufferedInputStream(connection.getInputStream(), 32768);
                outputStream = new FileOutputStream(tempFile);

                byte[] buffer = new byte[32768];
                long downloadedBytes = 0;
                int read;
                long lastUpdateTime = 0;

                while ((read = inputStream.read(buffer)) != -1) {
                    if (mIsCancelled.get()) {
                        tempFile.delete();
                        return;
                    }

                    outputStream.write(buffer, 0, read);
                    downloadedBytes += read;

                    long now = System.currentTimeMillis();
                    if (now - lastUpdateTime > 200 || downloadedBytes == totalBytes) {
                        lastUpdateTime = now;
                        final long curBytes = downloadedBytes;
                        final int percent = totalBytes > 0 ? (int) ((curBytes * 100) / totalBytes) : 0;
                        mMainHandler.post(() -> {
                            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                mProgressDialog.setProgress(percent);
                                double curMb = curBytes / (1024.0 * 1024.0);
                                double totalMb = totalBytes / (1024.0 * 1024.0);
                                if (totalBytes > 0) {
                                    mProgressDialog.setContent(String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)", curMb, totalMb, percent));
                                } else {
                                    mProgressDialog.setContent(String.format(Locale.US, "%.1f MB", curMb));
                                }
                            }
                        });
                    }
                }

                outputStream.flush();
                outputStream.close();
                outputStream = null;

                if (mIsCancelled.get()) {
                    tempFile.delete();
                    return;
                }

                if (targetFile.exists()) {
                    targetFile.delete();
                }

                if (!tempFile.renameTo(targetFile)) {
                    IOUtils.copyFile(tempFile, targetFile);
                    tempFile.delete();
                }

                mMainHandler.post(() -> {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.setTitle(R.string.os_download_extracting_title);
                        mProgressDialog.setContent(R.string.extracting_tips);
                        mProgressDialog.setProgress(100);
                    }
                    if (callback != null) {
                        callback.onSuccess(targetFile);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                tempFile.delete();
                final String err = e.getMessage() != null ? e.getMessage() : e.toString();
                mMainHandler.post(() -> {
                    UIHelper.dismiss(mProgressDialog);
                    if (callback != null) {
                        callback.onError(err);
                    }
                });
            } finally {
                IOUtils.closeSilently(inputStream);
                IOUtils.closeSilently(outputStream);
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public void dismissDialog() {
        mMainHandler.post(() -> UIHelper.dismiss(mProgressDialog));
    }
}
