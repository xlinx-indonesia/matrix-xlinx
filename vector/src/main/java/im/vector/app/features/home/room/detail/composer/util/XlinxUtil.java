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

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import im.vector.app.features.home.room.detail.composer.RecordTime;

public class XlinxUtil {

    public static final int  FADE_TIME                    = 150;

    private static volatile Handler handler;

    private static Animation getAlphaAnimation(float from, float to, int duration) {
        final Animation anim = new AlphaAnimation(from, to);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.setDuration(duration);
        return anim;
    }

    public static void animateIn(final @NonNull View view, final @NonNull Animation animation) {
        if (view.getVisibility() == View.VISIBLE) return;

        view.clearAnimation();
        animation.reset();
        animation.setStartTime(0);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

    public static ListenableFuture<Boolean> animateOut(final @NonNull View view, final @NonNull Animation animation, final int visibility) {
        final SettableFuture future = new SettableFuture();
        if (view.getVisibility() == visibility) {
            future.set(true);
        } else {
            view.clearAnimation();
            animation.reset();
            animation.setStartTime(0);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(visibility);
                    future.set(true);
                }
            });
            view.startAnimation(animation);
        }
        return future;
    }

    public static void fadeIn(final @NonNull View view, final int duration) {
        animateIn(view, getAlphaAnimation(0f, 1f, duration));
    }

    public static ListenableFuture<Boolean> fadeOut(final @NonNull View view, final int duration) {
        return fadeOut(view, duration, View.GONE);
    }

    public static ListenableFuture<Boolean> fadeOut(@NonNull View view, int duration, int visibility) {
        return animateOut(view, getAlphaAnimation(1f, 0f, duration), visibility);
    }


    private static Handler getHandler() {
        if (handler == null) {
            synchronized (XlinxUtil.class) {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return handler;
    }

    public static void runOnMainDelayed(final @NonNull Runnable runnable, long delayMillis) {
        getHandler().postDelayed(runnable, delayMillis);
    }

}
