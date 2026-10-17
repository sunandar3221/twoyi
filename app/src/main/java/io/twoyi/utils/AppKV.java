/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author weishu
 * @date 2019/2/28.
 */
public class AppKV {


    private static final String PREF_NAME = "app_kv";

    public static final String ADD_APP_NOT_SHOW_SYSTEM= "add_app_not_show_system";
    public static final String ADD_APP_NOT_SHOW_ADDED = "add_app_not_show_added";
    public static final String SHOW_ANDROID12_TIPS = "show_android12_tips_v2";
    public static final String ADD_APP_NOT_SHOW_32BIT = "add_app_not_show_32bit";

    // 是否应该重新安装 ROM
    // 1. 恢复出厂设置
    // 2. 替换 ROM
    public static final String FORCE_ROM_BE_RE_INSTALL = "rom_should_be_re_install";

    // 是否应该使用第三方 ROM
    public static final String SHOULD_USE_THIRD_PARTY_ROM = "should_use_third_party_rom";

    // 自定义容器 DPI (0: 自动)
    public static final String CONTAINER_DPI = "container_dpi";

    // 自定义容器帧率/FPS (0: 自动匹配屏幕刷新率，或具体数值如 30, 45, 60, 90, 120)
    public static final String CONTAINER_FPS = "container_fps";

    // 启动超时时间（秒）
    public static final String BOOT_TIMEOUT_SECONDS = "boot_timeout_seconds";

    // 运行期间保持屏幕常亮
    public static final String KEEP_SCREEN_ON = "keep_screen_on";

    public static boolean getBooleanConfig(Context context,  String key, boolean fallback) {
        return getPref(context).getBoolean(key, fallback);
    }

    @SuppressLint("ApplySharedPref")
    public static void setBooleanConfig(Context context, String key, boolean value) {
        getPref(context).edit().putBoolean(key, value).commit();
    }

    public static int getIntConfig(Context context, String key, int fallback) {
        return getPref(context).getInt(key, fallback);
    }

    @SuppressLint("ApplySharedPref")
    public static void setIntConfig(Context context, String key, int value) {
        getPref(context).edit().putInt(key, value).commit();
    }

    public static String getStringConfig(Context context, String key, String fallback) {
        return getPref(context).getString(key, fallback);
    }

    @SuppressLint("ApplySharedPref")
    public static void setStringConfig(Context context, String key, String value) {
        getPref(context).edit().putString(key, value).commit();
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
