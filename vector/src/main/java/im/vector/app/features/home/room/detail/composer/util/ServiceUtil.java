/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.composer.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.job.JobScheduler;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import android.os.storage.StorageManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;

public class ServiceUtil {
    public static InputMethodManager getInputMethodManager(Context context) {
        return (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @RequiresApi(25)
    public static @Nullable ShortcutManager getShortcutManager(@NonNull Context context) {
        return ContextCompat.getSystemService(context, ShortcutManager.class);
    }

    public static WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Activity.WINDOW_SERVICE);
    }

    public static StorageManager getStorageManager(Context context) {
        return ContextCompat.getSystemService(context, StorageManager.class);
    }

    public static ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
    }

    public static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static TelephonyManager getTelephonyManager(Context context) {
        return (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public static AudioManager getAudioManager(Context context) {
        return (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static SensorManager getSensorManager(Context context) {
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public static PowerManager getPowerManager(Context context) {
        return (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    }

    public static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    }

    public static Vibrator getVibrator(Context context) {
        return  (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static DisplayManager getDisplayManager(@NonNull Context context) {
        return (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    public static AccessibilityManager getAccessibilityManager(@NonNull Context context) {
        return (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    public static ClipboardManager getClipboardManager(@NonNull Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @RequiresApi(26)
    public static JobScheduler getJobScheduler(Context context) {
        return (JobScheduler) context.getSystemService(JobScheduler.class);
    }

    @RequiresApi(22)
    public static @Nullable SubscriptionManager getSubscriptionManager(@NonNull Context context) {
        return (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    public static ActivityManager getActivityManager(@NonNull Context context) {
        return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public static LocationManager getLocationManager(@NonNull Context context) {
        return ContextCompat.getSystemService(context, LocationManager.class);
    }
}

