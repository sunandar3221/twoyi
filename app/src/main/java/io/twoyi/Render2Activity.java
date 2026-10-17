/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cleveroad.androidmanimation.LoadingAnimationView;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.twoyi.utils.AppKV;
import io.twoyi.utils.LogEvents;
import io.twoyi.utils.NavUtils;
import io.twoyi.utils.RomDownloader;
import io.twoyi.utils.RomManager;
import io.twoyi.utils.UIHelper;

/**
 * @author weishu
 * @date 2021/10/20.
 */
public class Render2Activity extends Activity implements View.OnTouchListener {

    private static final String TAG = "Render2Activity";

    private SurfaceView mSurfaceView;

    private ViewGroup mRootView;
    private LoadingAnimationView mLoadingView;
    private TextView mLoadingText;
    private View mLoadingLayout;
    private View mBootLogView;

    private final AtomicBoolean mIsExtracting = new AtomicBoolean(false);
    private static final int REQUEST_CHOOSE_ROM_FILE = 2001;
    private RomDownloader mRomDownloader;

    private final SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            Surface surface = holder.getSurface();
            WindowManager windowManager = getWindowManager();
            Display defaultDisplay = windowManager.getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            defaultDisplay.getRealMetrics(displayMetrics);

            float xdpi = displayMetrics.xdpi;
            float ydpi = displayMetrics.ydpi;

            Renderer.init(surface, RomManager.getLoaderPath(getApplicationContext()), xdpi, ydpi, (int) getBestFps());

            Log.i(TAG, "surfaceCreated");
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            Surface surface = holder.getSurface();
            Renderer.resetWindow(surface, 0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight());
            Log.i(TAG, "surfaceChanged: " + mSurfaceView.getWidth() + "x" + mSurfaceView.getHeight());
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            Renderer.removeWindow(holder.getSurface());
            Log.i(TAG, "surfaceDestroyed!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean started = TwoyiStatusManager.getInstance().isStarted();
        Log.i(TAG, "onCreate: " + savedInstanceState + " isStarted: " + started);

        if (started) {
            // we have been started, but WTF we are onCreate again? just reboot ourself.
            finish();
            RomManager.reboot(this);
            return;
        }

        // reset state
        TwoyiStatusManager.getInstance().reset();

        NavUtils.hideNavigation(getWindow());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.ac_render);
        mRootView = findViewById(R.id.root);

        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(mSurfaceCallback);

        mLoadingLayout = findViewById(R.id.loadingLayout);
        mLoadingView = findViewById(R.id.loading);
        mLoadingText = findViewById(R.id.loadingText);
        mBootLogView = findViewById(R.id.bootlog);

        mLoadingLayout.setVisibility(View.VISIBLE);
        mLoadingView.startAnimation();

        UITips.checkForAndroid12(this, this::bootSystem);

        mSurfaceView.setOnTouchListener(this);

        if (AppKV.getBooleanConfig(this, AppKV.KEEP_SCREEN_ON, true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState: " + savedInstanceState);

        // we don't support state restore, just reboot.
        finish();
        RomManager.reboot(this);
    }

    private void bootSystem() {
        boolean romExist = RomManager.romExist(this);
        boolean factoryRomUpdated = RomManager.needsUpgrade(this);
        boolean forceInstall = AppKV.getBooleanConfig(getApplicationContext(), AppKV.FORCE_ROM_BE_RE_INSTALL, false);
        boolean use3rdRom = AppKV.getBooleanConfig(getApplicationContext(), AppKV.SHOULD_USE_THIRD_PARTY_ROM, false);

        boolean shouldExtractRom = !romExist || forceInstall || (!use3rdRom && factoryRomUpdated);

        if (shouldExtractRom) {
            if (!use3rdRom && !RomManager.hasAssetRom(this) && !RomManager.hasLocalRom(this)) {
                showDownloadRomDialog();
                return;
            }

            startExtractAndBoot(romExist, factoryRomUpdated, forceInstall, use3rdRom, null);
        } else {
            mRootView.addView(mSurfaceView, 0);
            showBootingProcedure();
        }
    }

    private void startExtractAndBoot(boolean romExist, boolean factoryRomUpdated, boolean forceInstall, boolean use3rdRom, Runnable onFinishExtract) {
        Log.i(TAG, "extracting rom...");
        showTipsForFirstBoot();

        new Thread(() -> {
            mIsExtracting.set(true);
            RomManager.extractRootfs(getApplicationContext(), romExist, factoryRomUpdated, forceInstall, use3rdRom);
            mIsExtracting.set(false);

            RomManager.initRootfs(getApplicationContext());

            if (onFinishExtract != null) {
                runOnUiThread(onFinishExtract);
            }

            runOnUiThread(() -> {
                mRootView.addView(mSurfaceView, 0);
                showBootingProcedure();
            });
        }, "extract-rom").start();
    }

    private void showDownloadRomDialog() {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.os_download_auto),
                getString(R.string.os_download_select_file),
                getString(R.string.os_download_custom_url)
        };

        UIHelper.getDialogBuilder(this)
                .setTitle(R.string.os_download_title)
                .setCancelable(false)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            startOnlineDownload(RomDownloader.DEFAULT_ROM_URL);
                            break;
                        case 1:
                            openFilePickerForRom();
                            break;
                        case 2:
                            showCustomUrlDialog();
                            break;
                    }
                })
                .setNegativeButton(R.string.quit, (dialog, which) -> finish())
                .show();
    }

    private void startOnlineDownload(String url) {
        if (mRomDownloader == null) {
            mRomDownloader = new RomDownloader(this);
        }
        mRomDownloader.startDownload(url, new RomDownloader.DownloadCallback() {
            @Override
            public void onSuccess(File downloadedFile) {
                startExtractAndBoot(false, false, false, false, () -> {
                    if (mRomDownloader != null) {
                        mRomDownloader.dismissDialog();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                UIHelper.getDialogBuilder(Render2Activity.this)
                        .setTitle(R.string.os_download_failed_title)
                        .setMessage(getString(R.string.os_download_failed, errorMessage))
                        .setPositiveButton(R.string.retry, (dialog, which) -> showDownloadRomDialog())
                        .setNegativeButton(R.string.quit, (dialog, which) -> finish())
                        .show();
            }

            @Override
            public void onCancelled() {
                showDownloadRomDialog();
            }
        });
    }

    private void showCustomUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.os_custom_url_hint);
        input.setText(RomDownloader.DEFAULT_ROM_URL);
        input.setSelection(input.getText().length());

        UIHelper.getDialogBuilder(this)
                .setTitle(R.string.os_custom_url_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(url)) {
                        startOnlineDownload(url);
                    } else {
                        showDownloadRomDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> showDownloadRomDialog())
                .show();
    }

    private void openFilePickerForRom() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, REQUEST_CHOOSE_ROM_FILE);
        } catch (Throwable e) {
            Toast.makeText(this, "File picker error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showDownloadRomDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_ROM_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                handleSelectedRomUri(data.getData());
            } else {
                showDownloadRomDialog();
            }
        }
    }

    private void handleSelectedRomUri(Uri uri) {
        MaterialDialog progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog_title)
                .content(R.string.extracting_tips)
                .progress(true, 0)
                .cancelable(false)
                .show();

        UIHelper.GLOBAL_EXECUTOR.execute(() -> {
            File targetFile = getFileStreamPath(RomManager.ROOTFS_NAME);
            try (InputStream is = getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[32768];
                int count;
                while ((count = is.read(buffer)) > 0) {
                    os.write(buffer, 0, count);
                }
                os.flush();

                runOnUiThread(() -> {
                    UIHelper.dismiss(progressDialog);
                    startExtractAndBoot(false, false, false, false, null);
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy selected ROM file", e);
                targetFile.delete();
                runOnUiThread(() -> {
                    UIHelper.dismiss(progressDialog);
                    Toast.makeText(Render2Activity.this, getString(R.string.os_download_invalid_file), Toast.LENGTH_LONG).show();
                    showDownloadRomDialog();
                });
            }
        });
    }

    private void showTipsForFirstBoot() {
        mLoadingText.setText(R.string.extracting_tips);
        mRootView.postDelayed(() -> {
            if (mIsExtracting.get()) {
                mLoadingText.setText(R.string.first_boot_tips);
            }
        }, 5000);

        mRootView.postDelayed(() -> {
            if (mIsExtracting.get()) {
                mLoadingText.setText(R.string.first_boot_tips2);
            }
        }, 10 * 1000);

        mRootView.postDelayed(() -> {
            if (mIsExtracting.get()) {
                mLoadingText.setText(R.string.first_boot_tips3);
            }
        }, 15 * 1000);
    }

    private void showBootingProcedure() {
        // mLoadingText.setText(R.string.booting_tips);
        mLoadingText.setVisibility(View.GONE);
        mBootLogView.setVisibility(View.VISIBLE);
        new Thread(() -> {

            if (true) {
                boolean success = false;
                try {
                    int timeoutSeconds = AppKV.getIntConfig(getApplicationContext(), AppKV.BOOT_TIMEOUT_SECONDS, 35);
                    success = TwoyiStatusManager.getInstance().waitBoot(timeoutSeconds, TimeUnit.SECONDS);
                } catch (Throwable ignored) {
                }

                if (!success) {
                    LogEvents.trackBootFailure(getApplicationContext());

                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.boot_failed, Toast.LENGTH_SHORT).show());

                    // waiting for track
                    SystemClock.sleep(3000);

                    finish();
                    System.exit(0);
                    return;
                }
            }

            runOnUiThread(() -> {
                mLoadingView.stopAnimation();
                mLoadingLayout.setVisibility(View.GONE);
            });
        }, "waiting-boot").start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            NavUtils.hideNavigation(getWindow());
        }

        // Update global visibility.
        TwoyiStatusManager.getInstance().updateVisibility(hasFocus);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Renderer.handleTouch(event);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // TODO: 2021/10/26 Add Volume control
        }
        return super.onKeyDown(keyCode, event);
    }

    private long mLastBackTime = 0;

    @Override
    public void onBackPressed() {
        long now = SystemClock.elapsedRealtime();
        if (now - mLastBackTime < 2000) {
            showQuickOptionsDialog();
        } else {
            mLastBackTime = now;
            Renderer.sendKeycode(KeyEvent.KEYCODE_BACK);
            Toast.makeText(this, R.string.press_back_again_for_menu, Toast.LENGTH_SHORT).show();
        }
    }

    private void showQuickOptionsDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.menu_switch_host),
                getString(R.string.title_settings),
                getString(R.string.settings_key_reboot),
                getString(R.string.settings_key_shutdown)
        };
        UIHelper.getDialogBuilder(this)
                .setTitle(R.string.container_quick_menu_title)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            TwoyiStatusManager.getInstance().switchOs(Render2Activity.this);
                            break;
                        case 1:
                            UIHelper.startActivity(Render2Activity.this, io.twoyi.ui.SettingsActivity.class);
                            break;
                        case 2:
                            RomManager.reboot(Render2Activity.this);
                            break;
                        case 3:
                            RomManager.shutdown(Render2Activity.this);
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private float getBestFps() {
        WindowManager windowManager = getWindowManager();
        Display defaultDisplay = windowManager.getDefaultDisplay();
        Display.Mode[] supportedModes = defaultDisplay.getSupportedModes();
        float fps = 45;
        for (Display.Mode supportedMode : supportedModes) {
            float refreshRate = supportedMode.getRefreshRate();
            if (refreshRate > fps) {
                // fps = refreshRate;
            }
        }

        Log.w(TAG, "current fps: " + fps);
        return fps;
    }
}
