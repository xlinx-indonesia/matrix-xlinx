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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.app.R
import im.vector.app.XlinxUtils
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.ui.ConversationItemBodyBubble
import im.vector.app.features.home.room.detail.timeline.ui.Outliner
import java.util.ArrayList

/**
 * Base timeline item that adds an optional information bar with the sender avatar, name and time
 * Adds associated click listeners (on avatar, displayname)
 */
abstract class AbsMessageItem<H : AbsMessageItem.Holder> : AbsBaseMessageItem<H>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    @EpoxyAttribute
    lateinit var attributes: Attributes

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var context: Context? = null

    private val _avatarClickListener = DebouncedClickListener(View.OnClickListener {
        attributes.avatarCallback?.onAvatarClicked(attributes.informationData)
    })
    private val _memberNameClickListener = DebouncedClickListener(View.OnClickListener {
        attributes.avatarCallback?.onMemberNameClicked(attributes.informationData)
    })

//    private val outliners: MutableList<Outliner> = ArrayList(2)

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    override fun bind(holder: H) {
        super.bind(holder)

        holder.avatarImageView.layoutParams = holder.avatarImageView.layoutParams?.apply {
            height = attributes.avatarSize
            width = attributes.avatarSize
        }
            holder.avatarImageView.visibility = View.VISIBLE
            holder.avatarImageView.setOnClickListener(_avatarClickListener)
            holder.memberNameView.visibility = View.VISIBLE
            holder.memberNameView.setOnClickListener(_memberNameClickListener)
            holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = attributes.informationData.time
            holder.memberNameView.text = attributes.informationData.memberName
            holder.memberNameView.setTextColor(attributes.getMemberNameColor())
            attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
            holder.avatarImageView.setOnLongClickListener(attributes.itemLongClickListener)
            holder.memberNameView.setOnLongClickListener(attributes.itemLongClickListener)

            if (attributes.informationData.sentByMe) {
                val background = R.drawable.message_bubble_background_sent_alone
//                val outliner = Outliner()
//                context?.resources?.let { outliner.setRadius(it.getDimensionPixelOffset(R.dimen.message_corner_radius)) }
//                context?.let { ContextCompat.getColor(it, R.color.core_grey_05) }?.let { outliner.setColor(it) }
//                outliners.clear()
//                outliners.add(outliner)

                holder.bodyBubble.setBackgroundResource(background)
//                holder.bodyBubble.setOutliners(outliners)

                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.addRule(RelativeLayout.END_OF, RelativeLayout.NO_ID)
                params.addRule(RelativeLayout.BELOW, R.id.messageMemberNameView)
                params.addRule(RelativeLayout.START_OF, RelativeLayout.NO_ID)
                params.addRule(RelativeLayout.ALIGN_PARENT_END)
                holder.bodyBubble.layoutParams = params
                holder.bodyBubble.isVisible = true
                holder.bodyBubble.updateLayoutParams<RelativeLayout.LayoutParams> {
                    updateMargins(0,0,0, XlinxUtils.dpToPx(4))
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    context?.getColor(R.color.notification_accent_color)?.let { holder.bodyBubble.background.setColorFilter(it, PorterDuff.Mode.MULTIPLY) }
                }

                holder.memberNameView.apply {
                    textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                    text = context.getString(R.string.message_item_sent_by_me)
                }
                holder.avatarImageView.visibility = View.GONE

                val memberNameViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                memberNameViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                memberNameViewParams.addRule(RelativeLayout.START_OF, R.id.messageTimeView)
                memberNameViewParams.addRule(RelativeLayout.END_OF, R.id.messageStartGuideline)
                holder.memberNameView.layoutParams = memberNameViewParams
                holder.memberNameView.updateLayoutParams<RelativeLayout.LayoutParams> {
                    updateMargins(XlinxUtils.dpToPx(8), XlinxUtils.dpToPx(4), XlinxUtils.dpToPx(4), 0)
                }

                val timeViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                timeViewParams.addRule(RelativeLayout.ALIGN_BASELINE, R.id.messageMemberNameView)
                timeViewParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                holder.timeView.layoutParams = timeViewParams
                holder.timeView.updateLayoutParams<RelativeLayout.LayoutParams> {
                    updateMargins(XlinxUtils.dpToPx(8), 0, XlinxUtils.dpToPx(8), 0)
                }

                holder.memberNameView.visibility = View.INVISIBLE

            } else {
                val background = R.drawable.message_bubble_background_sent_alone
//                val outliner = Outliner()
//                context?.resources?.let { outliner.setRadius(it.getDimensionPixelOffset(R.dimen.message_corner_radius)) }
//                outliner.setColor(attributes.getMemberNameColor())
//                outliners.clear()
//                outliners.add(outliner)

                holder.bodyBubble.setBackgroundResource(background)
//                holder.bodyBubble.setOutliners(outliners)

                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.addRule(RelativeLayout.END_OF, R.id.messageStartGuideline)
                params.addRule(RelativeLayout.BELOW, R.id.messageMemberNameView)
                holder.bodyBubble.layoutParams = params
                holder.bodyBubble.isVisible = true
//                holder.bodyBubble.background.setColorFilter(attributes.getMemberNameColor(), PorterDuff.Mode.MULTIPLY)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    context?.getColor(R.color.core_grey_05)?.let { holder.bodyBubble.background.setColorFilter(it, PorterDuff.Mode.MULTIPLY) }
                }
                holder.bodyBubble.updateLayoutParams<RelativeLayout.LayoutParams> {
                    updateMargins(0,0,XlinxUtils.dpToPx(52), XlinxUtils.dpToPx(4))
                }

                holder.memberNameView.apply {
                    textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                    text = attributes.informationData.memberName
                }

                val memberNameViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                memberNameViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                memberNameViewParams.addRule(RelativeLayout.END_OF, R.id.messageStartGuideline)
                holder.memberNameView.layoutParams = memberNameViewParams
                holder.memberNameView.updateLayoutParams<RelativeLayout.LayoutParams> {
                    updateMargins(XlinxUtils.dpToPx(8), XlinxUtils.dpToPx(4), XlinxUtils.dpToPx(4), 0)
                }

                val timeViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                timeViewParams.addRule(RelativeLayout.ALIGN_BASELINE, R.id.messageMemberNameView)
                timeViewParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.NO_ID)
                timeViewParams.addRule(RelativeLayout.END_OF, R.id.messageMemberNameView)
                holder.timeView.layoutParams = timeViewParams
                holder.timeView.updateLayoutParams<RelativeLayout.LayoutParams> {
                    updateMargins(XlinxUtils.dpToPx(8), 0, XlinxUtils.dpToPx(8), 0)
                }

                holder.avatarImageView.visibility = View.VISIBLE

                holder.memberNameView.visibility = View.VISIBLE
            }

        holder.bodyBubble.apply {
            updatePadding(0, XlinxUtils.dpToPx(3), 0, XlinxUtils.dpToPx(4))
        }

    }

    override fun unbind(holder: H) {
        attributes.avatarRenderer.clear(holder.avatarImageView)
        holder.avatarImageView.setOnClickListener(null)
        holder.avatarImageView.setOnLongClickListener(null)
        holder.memberNameView.setOnClickListener(null)
        holder.memberNameView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    private fun Attributes.getMemberNameColor() = messageColorProvider.getMemberNameTextColor(informationData.matrixItem)

    abstract class Holder(@IdRes stubId: Int) : AbsBaseMessageItem.Holder(stubId) {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val bodyBubble by bind<ConversationItemBodyBubble>(R.id.body_bubble)
    }

    /**
     * This class holds all the common attributes for timeline items.
     */
    data class Attributes(
            val avatarSize: Int,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val messageColorProvider: MessageColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: View.OnClickListener? = null,
            val memberClickListener: View.OnClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            val avatarCallback: TimelineEventController.AvatarCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val emojiTypeFace: Typeface? = null
    ) : AbsBaseMessageItem.Attributes {

        // Have to override as it's used to diff epoxy items
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Attributes

            if (avatarSize != other.avatarSize) return false
            if (informationData != other.informationData) return false

            return true
        }

        override fun hashCode(): Int {
            var result = avatarSize
            result = 31 * result + informationData.hashCode()
            return result
        }
    }
}
