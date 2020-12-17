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

package im.vector.app.features.home.room.detail.composer;

import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.concurrent.TimeUnit;

import im.vector.app.features.home.room.detail.composer.util.ListenableFuture;
import im.vector.app.features.home.room.detail.composer.util.SettableFuture;
import im.vector.app.features.home.room.detail.composer.util.XlinxUtil;


public class RecordTime implements Runnable {

    private static final int  FADE_TIME                    = 150;

    private final @NonNull TextView recordTimeView;
    private final @NonNull View microphone;
    private final @NonNull Runnable onLimitHit;
    private final          long     limitSeconds;
    private                long     startTime;

    public RecordTime(@NonNull TextView recordTimeView, @NonNull View microphone, long limitSeconds, @NonNull Runnable onLimitHit) {
        this.recordTimeView = recordTimeView;
        this.microphone     = microphone;
        this.limitSeconds   = limitSeconds;
        this.onLimitHit     = onLimitHit;
    }

    @MainThread
    public void display() {
        this.startTime = System.currentTimeMillis();
        this.recordTimeView.setText(DateUtils.formatElapsedTime(0));
        XlinxUtil.fadeIn(this.recordTimeView, FADE_TIME);
        XlinxUtil.runOnMainDelayed(this, TimeUnit.SECONDS.toMillis(1));
        microphone.setVisibility(View.VISIBLE);
        microphone.startAnimation(pulseAnimation());
    }

    @MainThread
    public long hide() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        this.startTime = 0;
        XlinxUtil.fadeOut(this.recordTimeView, FADE_TIME, View.INVISIBLE);
        microphone.clearAnimation();
        XlinxUtil.fadeOut(this.microphone, FADE_TIME, View.INVISIBLE);
        return elapsedTime;
    }

    @Override
    @MainThread
    public void run() {
        long localStartTime = startTime;
        if (localStartTime > 0) {
            long elapsedTime = System.currentTimeMillis() - localStartTime;
            long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime);
            if (elapsedSeconds >= limitSeconds) {
                onLimitHit.run();
            } else {
                recordTimeView.setText(DateUtils.formatElapsedTime(elapsedSeconds));
                XlinxUtil.runOnMainDelayed(this, TimeUnit.SECONDS.toMillis(1));
            }
        }
    }

    private static Animation pulseAnimation() {
        AlphaAnimation animation = new AlphaAnimation(0, 1);

        animation.setInterpolator(pulseInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setDuration(1000);

        return animation;
    }

    private static Interpolator pulseInterpolator() {
        return input -> {
            input *= 5;
            if (input > 1) {
                input = 4 - input;
            }
            return Math.max(0, Math.min(1, input));
        };
    }
}
