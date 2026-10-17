/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.twoyi.R;

public class RomDownloader {

    private static final String TAG = "RomDownloader";

    public static final String GITHUB_REPO_ROM_URL = "https://github.com/sunandar3221/twoyi/releases/download/v0.5.4/rootfs.7z";
    public static final String GITHUB_OFFICIAL_ROM_URL = "https://github.com/twoyi/twoyi/releases/download/0.5.4/twoyi_0.5.4-03211927-release.apk";
    public static final String DEFAULT_ROM_URL = GITHUB_REPO_ROM_URL;

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

    public static boolean is7zArchive(File file) {
        if (file == null || !file.exists() || file.length() < 32) {
            return false;
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] magic = new byte[6];
            int read = is.read(magic);
            return read == 6
                    && magic[0] == '7'
                    && magic[1] == 'z'
                    && (magic[2] & 0xFF) == 0xBC
                    && (magic[3] & 0xFF) == 0xAF
                    && (magic[4] & 0xFF) == 0x27
                    && (magic[5] & 0xFF) == 0x1C;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isZipOrApk(File file) {
        if (file == null || !file.exists() || file.length() < 32) {
            return false;
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] magic = new byte[4];
            int read = is.read(magic);
            return read == 4
                    && magic[0] == 'P'
                    && magic[1] == 'K'
                    && (magic[2] & 0xFF) == 0x03
                    && (magic[3] & 0xFF) == 0x04;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void processRomFile(Context context, File sourceFile, File targetFile) throws IOException {
        if (isZipOrApk(sourceFile)) {
            try (ZipFile zipFile = new ZipFile(sourceFile)) {
                ZipEntry entry = zipFile.getEntry("assets/rootfs.7z");
                if (entry == null) {
                    entry = zipFile.getEntry("rootfs.7z");
                }
                if (entry == null) {
                    throw new IOException("APK tidak berisi paket assets/rootfs.7z");
                }

                if (targetFile.exists()) {
                    targetFile.delete();
                }

                try (InputStream zis = zipFile.getInputStream(entry);
                     OutputStream fos = new FileOutputStream(targetFile)) {
                    IOUtils.copy(zis, fos);
                }

                ZipEntry romIniEntry = zipFile.getEntry("assets/rom.ini");
                if (romIniEntry != null) {
                    File romIniFile = context.getFileStreamPath("rom.ini");
                    try (InputStream ris = zipFile.getInputStream(romIniEntry);
                         OutputStream ros = new FileOutputStream(romIniFile)) {
                        IOUtils.copy(ris, ros);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } else if (is7zArchive(sourceFile)) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            if (!sourceFile.renameTo(targetFile)) {
                IOUtils.copyFile(sourceFile, targetFile);
            }
        } else {
            throw new IOException("File bukan merupakan format rootfs.7z atau APK Twoyi yang valid.");
        }
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
                boolean triedFallback = false;

                while (true) {
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

                    int code = connection != null ? connection.getResponseCode() : -1;
                    if ((code == 404 || code == 403) && !triedFallback && !GITHUB_OFFICIAL_ROM_URL.equals(currentUrl)) {
                        Log.i(TAG, "Repo release returned " + code + ", falling back to official GitHub release");
                        if (connection != null) {
                            connection.disconnect();
                        }
                        currentUrl = GITHUB_OFFICIAL_ROM_URL;
                        triedFallback = true;
                        continue;
                    }

                    if (connection == null || code != HttpURLConnection.HTTP_OK) {
                        throw new RuntimeException("HTTP status " + code);
                    }
                    break;
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

                mMainHandler.post(() -> {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.setTitle(R.string.os_download_extracting_title);
                        mProgressDialog.setContent(R.string.extracting_tips);
                        mProgressDialog.setProgress(100);
                    }
                });

                processRomFile(mActivity, tempFile, targetFile);
                tempFile.delete();

                mMainHandler.post(() -> {
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
