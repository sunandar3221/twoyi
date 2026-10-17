/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;

import com.microsoft.appcenter.crashes.Crashes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import io.twoyi.R;
import io.twoyi.utils.AppKV;
import io.twoyi.utils.LogEvents;
import io.twoyi.utils.RomManager;
import io.twoyi.utils.UIHelper;

/**
 * @author weishu
 * @date 2022/1/2.
 */

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_GET_FILE = 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        SettingsFragment settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.settingsFrameLayout, settingsFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.colorPrimary));
            actionBar.setTitle(R.string.title_settings);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);
        }

        private Preference findPreference(@StringRes int id) {
            String key = getString(id);
            return findPreference(key);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Preference importApp = findPreference(R.string.settings_key_import_app);
            Preference export = findPreference(R.string.settings_key_manage_files);

            Preference shutdown = findPreference(R.string.settings_key_shutdown);
            Preference reboot = findPreference(R.string.settings_key_reboot);
            Preference replaceRom = findPreference(R.string.settings_key_replace_rom);
            Preference factoryReset = findPreference(R.string.settings_key_factory_reset);

            Preference donate = findPreference(R.string.settings_key_donate);
            Preference sendLog = findPreference(R.string.settings_key_sendlog);
            Preference about = findPreference(R.string.settings_key_about);

            importApp.setOnPreferenceClickListener(preference -> {
                UIHelper.startActivity(getContext(), SelectAppActivity.class);
                return true;
            });

            export.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
                startActivity(intent);
                return true;
            });

            shutdown.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                activity.finishAffinity();
                RomManager.shutdown(activity);
                return true;
            });

            reboot.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                activity.finishAndRemoveTask();
                RomManager.reboot(activity);
                return true;
            });

            replaceRom.setOnPreferenceClickListener(preference -> {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

                // you can only select one rootfs
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                intent.setType("*/*"); // apk file
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(intent, REQUEST_GET_FILE);
                } catch (Throwable ignored) {
                    Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            factoryReset.setOnPreferenceClickListener(preference -> {
                UIHelper.getDialogBuilder(getActivity())
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.factory_reset_confirm_message)
                        .setPositiveButton(R.string.i_confirm_it, (dialog, which) -> {
                            AppKV.setBooleanConfig(getActivity(), AppKV.SHOULD_USE_THIRD_PARTY_ROM, false);
                            AppKV.setBooleanConfig(getActivity(), AppKV.FORCE_ROM_BE_RE_INSTALL, true);
                            dialog.dismiss();

                            RomManager.reboot(getActivity());
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            });

            Preference containerDpi = findPreference(R.string.settings_key_container_dpi);
            if (containerDpi != null) {
                int currentDpi = AppKV.getIntConfig(getActivity(), AppKV.CONTAINER_DPI, 0);
                updateDpiSummary(containerDpi, currentDpi);
                containerDpi.setOnPreferenceClickListener(preference -> {
                    showDpiChooserDialog(containerDpi);
                    return true;
                });
            }

            Preference containerFps = findPreference(R.string.settings_key_container_fps);
            if (containerFps != null) {
                int currentFps = AppKV.getIntConfig(getActivity(), AppKV.CONTAINER_FPS, 0);
                updateFpsSummary(containerFps, currentFps);
                containerFps.setOnPreferenceClickListener(preference -> {
                    showFpsChooserDialog(containerFps);
                    return true;
                });
            }

            Preference bootTimeout = findPreference(R.string.settings_key_boot_timeout);
            if (bootTimeout != null) {
                int currentTimeout = AppKV.getIntConfig(getActivity(), AppKV.BOOT_TIMEOUT_SECONDS, 35);
                bootTimeout.setSummary(getString(R.string.settings_boot_timeout_summary) + " (" + currentTimeout + "s)");
                bootTimeout.setOnPreferenceClickListener(preference -> {
                    showBootTimeoutDialog(bootTimeout);
                    return true;
                });
            }

            Preference keepScreenOnPref = findPreference(R.string.settings_key_keep_screen_on);
            if (keepScreenOnPref instanceof SwitchPreference) {
                SwitchPreference switchPref = (SwitchPreference) keepScreenOnPref;
                switchPref.setChecked(AppKV.getBooleanConfig(getActivity(), AppKV.KEEP_SCREEN_ON, true));
                switchPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    AppKV.setBooleanConfig(getActivity(), AppKV.KEEP_SCREEN_ON, enabled);
                    return true;
                });
            }

            Preference containerInfo = findPreference(R.string.settings_key_container_info);
            if (containerInfo != null) {
                containerInfo.setOnPreferenceClickListener(preference -> {
                    showContainerInfoDialog();
                    return true;
                });
            }

            donate.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                if (context instanceof Activity) {
                    UIHelper.showDonateDialog((Activity) context);
                    return true;
                }
                return false;
            });

            sendLog.setOnPreferenceClickListener(preference -> {
                Context context = getActivity();
                byte[] bugreport = LogEvents.getBugreport(context);
                File tmpLog = new File(context.getCacheDir(), "bugreport.zip");
                try {
                    Files.write(tmpLog.toPath(), bugreport);
                } catch (IOException e) {
                    Crashes.trackError(e);
                }
                Uri uri = FileProvider.getUriForFile(context, "io.twoyi.fileprovider", tmpLog);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setDataAndType(uri, "application/zip");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                context.startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_key_sendlog)));

                return true;
            });

            about.setOnPreferenceClickListener(preference -> {
                UIHelper.startActivity(getContext(), AboutActivity.class);
                return true;
            });
        }

        private void updateDpiSummary(Preference pref, int dpi) {
            if (dpi == 0) {
                pref.setSummary(getString(R.string.dpi_auto, DisplayMetrics.DENSITY_DEVICE_STABLE));
            } else {
                pref.setSummary(dpi + " DPI");
            }
        }

        private void showDpiChooserDialog(Preference pref) {
            CharSequence[] items = new CharSequence[]{
                    getString(R.string.dpi_auto, DisplayMetrics.DENSITY_DEVICE_STABLE),
                    getString(R.string.dpi_compact),
                    getString(R.string.dpi_medium),
                    getString(R.string.dpi_standard),
                    getString(R.string.dpi_large)
            };
            int[] values = new int[]{0, 240, 320, 400, 480};
            int current = AppKV.getIntConfig(getActivity(), AppKV.CONTAINER_DPI, 0);
            int checkedItem = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    checkedItem = i;
                    break;
                }
            }
            UIHelper.getDialogBuilder(getActivity())
                    .setTitle(R.string.settings_key_container_dpi)
                    .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                        int selectedVal = values[which];
                        AppKV.setIntConfig(getActivity(), AppKV.CONTAINER_DPI, selectedVal);
                        updateDpiSummary(pref, selectedVal);
                        dialog.dismiss();
                        Toast.makeText(getActivity(), R.string.settings_container_dpi_summary, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        private void updateFpsSummary(Preference pref, int fps) {
            if (fps == 0) {
                pref.setSummary(getString(R.string.fps_auto));
            } else {
                pref.setSummary(fps + " FPS");
            }
        }

        private void showFpsChooserDialog(Preference pref) {
            CharSequence[] items = new CharSequence[]{
                    getString(R.string.fps_auto),
                    getString(R.string.fps_30),
                    getString(R.string.fps_45),
                    getString(R.string.fps_60),
                    getString(R.string.fps_90),
                    getString(R.string.fps_120)
            };
            int[] values = new int[]{0, 30, 45, 60, 90, 120};
            int current = AppKV.getIntConfig(getActivity(), AppKV.CONTAINER_FPS, 0);
            int checkedItem = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    checkedItem = i;
                    break;
                }
            }
            UIHelper.getDialogBuilder(getActivity())
                    .setTitle(R.string.settings_key_container_fps)
                    .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                        int selectedVal = values[which];
                        AppKV.setIntConfig(getActivity(), AppKV.CONTAINER_FPS, selectedVal);
                        updateFpsSummary(pref, selectedVal);
                        dialog.dismiss();
                        Toast.makeText(getActivity(), R.string.settings_container_fps_summary, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        private void showBootTimeoutDialog(Preference pref) {
            CharSequence[] items = new CharSequence[]{
                    getString(R.string.timeout_15s),
                    getString(R.string.timeout_30s),
                    getString(R.string.timeout_45s),
                    getString(R.string.timeout_60s),
                    getString(R.string.timeout_90s)
            };
            int[] values = new int[]{15, 30, 45, 60, 90};
            int current = AppKV.getIntConfig(getActivity(), AppKV.BOOT_TIMEOUT_SECONDS, 35);
            int checkedItem = 2;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    checkedItem = i;
                    break;
                }
            }
            UIHelper.getDialogBuilder(getActivity())
                    .setTitle(R.string.settings_key_boot_timeout)
                    .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                        int selectedVal = values[which];
                        AppKV.setIntConfig(getActivity(), AppKV.BOOT_TIMEOUT_SECONDS, selectedVal);
                        pref.setSummary(getString(R.string.settings_boot_timeout_summary) + " (" + selectedVal + "s)");
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        private void showContainerInfoDialog() {
            Activity act = getActivity();
            boolean romExists = RomManager.romExist(act);
            RomManager.RomInfo romInfo = RomManager.getCurrentRomInfo(act);
            File sdcard = RomManager.getRomSdcardDir(act);
            String freeSpace = Formatter.formatFileSize(act, sdcard.getFreeSpace());
            String totalSpace = Formatter.formatFileSize(act, sdcard.getTotalSpace());

            StringBuilder sb = new StringBuilder();
            sb.append("Container Status: ").append(romExists ? "Installed" : "Not Installed").append("\n\n");
            sb.append("ROM Author: ").append(romInfo.author).append("\n");
            sb.append("ROM Version: ").append(romInfo.version).append("\n");
            sb.append("ROM Description: ").append(romInfo.desc).append("\n\n");
            sb.append("Internal Storage: ").append(freeSpace).append(" free / ").append(totalSpace).append("\n");
            sb.append("Host OS: Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
            sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
            sb.append("Architecture: arm64-v8a");

            UIHelper.getDialogBuilder(act)
                    .setTitle(R.string.settings_key_container_info)
                    .setMessage(sb.toString())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }


        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (!(requestCode == REQUEST_GET_FILE && resultCode == Activity.RESULT_OK)) {
                return;
            }

            if (data == null) {
                return;
            }

            Uri uri = data.getData();
            if (uri == null) {
                return;
            }

            Activity activity = getActivity();
            ProgressDialog dialog = UIHelper.getProgressDialog(activity);
            dialog.setCancelable(false);
            dialog.show();

            // start copy 3rd rom
            UIHelper.defer().when(() -> {

                File rootfs3rd = RomManager.get3rdRootfsFile(activity);

                ContentResolver contentResolver = activity.getContentResolver();
                try (InputStream inputStream = contentResolver.openInputStream(uri); OutputStream os = new FileOutputStream(rootfs3rd)) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = inputStream.read(buffer)) > 0) {
                        os.write(buffer, 0, count);
                    }
                }

                RomManager.RomInfo romInfo = RomManager.getRomInfo(rootfs3rd);
                return Pair.create(rootfs3rd, romInfo);
            }).done(result -> {

                File rootfs3rd = result.first;
                RomManager.RomInfo romInfo = result.second;
                UIHelper.dismiss(dialog);

                // copy finished, show dialog confirm
                if (romInfo.isValid()) {

                    String author = romInfo.author;
                    if ("weishu".equalsIgnoreCase(author) || "twoyi".equalsIgnoreCase(author)) {
                        Toast.makeText(activity, R.string.replace_rom_unofficial_tips, Toast.LENGTH_SHORT).show();
                        rootfs3rd.delete();
                        return;

                    }
                    UIHelper.getDialogBuilder(activity)
                            .setTitle(R.string.replace_rom_confirm_title)
                            .setMessage(getString(R.string.replace_rom_confirm_message, author, romInfo.version, romInfo.desc))
                            .setPositiveButton(R.string.i_confirm_it, (dialog1, which) -> {
                                AppKV.setBooleanConfig(activity, AppKV.SHOULD_USE_THIRD_PARTY_ROM, true);
                                AppKV.setBooleanConfig(activity, AppKV.FORCE_ROM_BE_RE_INSTALL, true);

                                dialog1.dismiss();

                                RomManager.reboot(getActivity());
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog12, which) -> dialog12.dismiss())
                            .show();
                } else {
                    Toast.makeText(activity, R.string.replace_rom_invalid, Toast.LENGTH_SHORT).show();
                    rootfs3rd.delete();
                }
            }).fail(result -> activity.runOnUiThread(() -> {
                Toast.makeText(activity, getResources().getString(R.string.install_failed_reason, result.getMessage()), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                activity.finish();
            }));

        }
    }
}
