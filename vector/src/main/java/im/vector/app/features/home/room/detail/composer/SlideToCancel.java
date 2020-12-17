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

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

import im.vector.app.features.home.room.detail.composer.util.ListenableFuture;
import im.vector.app.features.home.room.detail.composer.util.SettableFuture;
import im.vector.app.features.home.room.detail.composer.util.XlinxUtil;

public class SlideToCancel {

    private static final int  FADE_TIME                    = 150;

    private final View slideToCancelView;

    public SlideToCancel(View slideToCancelView) {
        this.slideToCancelView = slideToCancelView;
    }

    public void display() {
        XlinxUtil.fadeIn(this.slideToCancelView, FADE_TIME);
    }

    public ListenableFuture<Void> hide() {
        final SettableFuture<Void> future = new SettableFuture<>();

        AnimationSet animation = new AnimationSet(true);
        animation.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, slideToCancelView.getTranslationX(),
                Animation.ABSOLUTE, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0));
        animation.addAnimation(new AlphaAnimation(1, 0));

        animation.setDuration(MicrophoneRecorderView.ANIMATION_DURATION);
        animation.setFillBefore(true);
        animation.setFillAfter(false);

        slideToCancelView.postDelayed(() -> future.set(null), MicrophoneRecorderView.ANIMATION_DURATION);
        slideToCancelView.setVisibility(View.GONE);
        slideToCancelView.startAnimation(animation);

        return future;
    }

    public void moveTo(float offset) {
        Animation animation = new TranslateAnimation(Animation.ABSOLUTE, offset,
                Animation.ABSOLUTE, offset,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0);

        animation.setDuration(0);
        animation.setFillAfter(true);
        animation.setFillBefore(true);

        slideToCancelView.startAnimation(animation);
    }
}