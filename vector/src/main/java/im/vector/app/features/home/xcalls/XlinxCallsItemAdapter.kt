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

package im.vector.app.features.home.xcalls

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.tencent.mmkv.MMKV
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.call.WebRtcPeerConnectionManager
import im.vector.app.features.home.AvatarRenderer
import kotlinx.android.synthetic.main.fragment_home_detail.*
import kotlinx.android.synthetic.main.xlinx_item_calls_history.view.*
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import java.net.URL
import javax.inject.Inject

class XlinxCallsItemAdapter(
        private val context: Context,
        private val calls: ArrayList<XlinxCallsItem>,
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val webRtcPeerConnectionManager: WebRtcPeerConnectionManager,
        ) : RecyclerView.Adapter<XlinxCallsItemAdapter.CallHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): XlinxCallsItemAdapter.CallHolder {
        val inflatedView = parent.inflate(R.layout.xlinx_item_calls_history, false)
        return CallHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: XlinxCallsItemAdapter.CallHolder, position: Int) {
        val itemCall = calls[position]
        holder.bindCallItem(context, itemCall, session, avatarRenderer, webRtcPeerConnectionManager)
    }

    override fun getItemCount(): Int = calls.size




    //1
    class CallHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        //2
        private var view: View = v
        private var callItem: XlinxCallsItem? = null
        private lateinit var session: Session
        private lateinit var webRtcPeerConnectionManager: WebRtcPeerConnectionManager
        private val mmkv: MMKV = MMKV.mmkvWithID("xlinxcallhistory")

        //3
        init {
            v.setOnClickListener(this)
        }

        //4
        @SuppressLint("LogNotTimber")
        override fun onClick(v: View) {
            Log.d("RecyclerView", "CLICK!")
            val room = callItem?.targetRoomId?.let { session.getRoom(it) }

            if (room != null) {
                room.roomSummary()?.otherMemberIds?.firstOrNull()?.let {
                    val displayName = room.roomSummary()?.displayName

                    val timestamp = System.currentTimeMillis().toString()
                    val currentCount: Long = mmkv.count() + 1

                    when (callItem?.callMode) {
                        1 -> {
                            mmkv.encode(timestamp, currentCount.toString() + ":::" + room.roomId + ":::" + displayName + ":::" + "1" + ":::" + "2" + ":::" + timestamp)
                            webRtcPeerConnectionManager.startOutgoingCall(room.roomId, it, true)
                        }
                        2 -> {
                            mmkv.encode(timestamp, currentCount.toString() + ":::" + room.roomId + ":::" + displayName + ":::" + "2" + ":::" + "2" + ":::" + timestamp)
                            webRtcPeerConnectionManager.startOutgoingCall(room.roomId, it, false)
                        }
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bindCallItem(context: Context, callItem: XlinxCallsItem, session: Session, avatarRenderer: AvatarRenderer, webRtcPeerConnectionManager: WebRtcPeerConnectionManager) {
            this.callItem = callItem
            this.session = session
            this.webRtcPeerConnectionManager = webRtcPeerConnectionManager
//            val me = session.getRoomMember(session.myUserId, callItem.targetRoomId)?.toMatrixItem()
            val matrixItem = session.getRoomSummary(callItem.targetRoomId)?.toMatrixItem()

            matrixItem?.let { avatarRenderer.render(it, view.roomAvatarImageView) }

            view.roomNameView.text                  = callItem.targetName
            view.roomLastEventTimeView.text         = callItem.timestamp.toString()

            when (callItem.callType) {
                1 ->    view.roomLastEventView.text = context.getString(R.string.callhistory_status_incomingcall)
                2 ->    view.roomLastEventView.text = context.getString(R.string.callhistory_status_outgoingcall)
            }

            when (callItem.callMode) {
                1 ->    view.callTypeStatus.setImageResource(R.drawable.ic_call_end)
                2 ->    view.callTypeStatus.setImageResource(R.drawable.ic_videocam)
            }

        }

        companion object {
            //5
            private val CALL_ITEM_KEY = "CALLSITEM"
        }
    }

}

