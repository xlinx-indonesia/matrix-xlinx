/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import im.vector.app.XlinxUtils;

public class ConversationItemBodyBubble extends LinearLayout {

    @Nullable private List<Outliner>        outliners = Collections.emptyList();
    @Nullable private OnSizeChangedListener sizeChangedListener;

    public ConversationItemBodyBubble(Context context) {
        super(context);
    }

    public ConversationItemBodyBubble(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ConversationItemBodyBubble(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOutliners(@NonNull List<Outliner> outliners) {
        this.outliners = outliners;
    }

    public void setOnSizeChangedListener(@Nullable OnSizeChangedListener listener) {
        this.sizeChangedListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (XlinxUtils.isEmpty(outliners)) return;

        for (Outliner outliner : outliners) {
            outliner.draw(canvas, 0, getMeasuredWidth(), getMeasuredHeight(), 0);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (sizeChangedListener != null) {
            post(() -> {
                if (sizeChangedListener != null) {
                    sizeChangedListener.onSizeChanged(width, height);
                }
            });
        }
    }

    public interface OnSizeChangedListener {
        void onSizeChanged(int width, int height);
    }
}


