/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.XlinxUtils
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.media.ImageContentRenderer
import org.matrix.android.sdk.api.session.room.send.SendState
import kotlin.coroutines.coroutineContext

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute
    lateinit var mediaData: ImageContentRenderer.Data

    @EpoxyAttribute
    var playable: Boolean = false

    @EpoxyAttribute
    var mode = ImageContentRenderer.Mode.THUMBNAIL

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: View.OnClickListener? = null

    @EpoxyAttribute
    lateinit var imageContentRenderer: ImageContentRenderer

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    override fun bind(holder: Holder) {
        super.bind(holder)
        imageContentRenderer.render(mediaData, mode, holder.imageView)
        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(attributes.informationData.eventId, mediaData.isLocalFile, holder.progressLayout)
        } else {
            holder.progressLayout.isVisible = false
        }
        holder.imageView.setOnClickListener(clickListener)
        holder.imageView.setOnLongClickListener(attributes.itemLongClickListener)
        ViewCompat.setTransitionName(holder.imageView, "imagePreview_${id()}")
        holder.mediaContentView.setOnClickListener(attributes.itemClickListener)
        holder.mediaContentView.setOnLongClickListener(attributes.itemLongClickListener)
        // The sending state color will be apply to the progress text
        renderSendState(holder.imageView, null, holder.failedToSendIndicator)
        holder.playContentView.visibility = if (playable) View.VISIBLE else View.GONE

        holder.eventSendingIndicator.isVisible = when (attributes.informationData.sendState) {
            SendState.UNSENT,
            SendState.ENCRYPTING,
            SendState.SENDING -> true
            else              -> false
        }

        if (attributes.informationData.sentByMe) {
            holder.imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                marginStart = XlinxUtils.dpToPx(32)
                marginEnd = 0
                startToStart = ConstraintLayout.LayoutParams.UNSET
            }
        } else {
            holder.imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                marginEnd = XlinxUtils.dpToPx(32)
                marginStart = 0
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
    }

    override fun unbind(holder: Holder) {
        GlideApp.with(holder.view.context.applicationContext).clear(holder.imageView)
        imageContentRenderer.clear(holder.imageView)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        holder.imageView.setOnClickListener(null)
        holder.imageView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)
        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)

        val mediaContentView by bind<ViewGroup>(R.id.messageContentMedia)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)
        val eventSendingIndicator by bind<ProgressBar>(R.id.eventSendingIndicator)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMediaStub
    }

    open fun setMargins(v: View, l: Int, t: Int, r: Int, b: Int) {
        if (v.layoutParams is MarginLayoutParams) {
            val p = v.layoutParams as MarginLayoutParams
            p.setMargins(l, t, r, b)
            v.requestLayout()
        }
    }
}
