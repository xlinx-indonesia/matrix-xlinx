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

package im.vector.app.features.home.xspace.conference

import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import java.net.URL

class JitsiCallViewModel @AssistedInject constructor(
        @Assisted initialState: JitsiCallViewState,
        @Assisted val args: XlinxSpaceActivity.Args,
        private val session: Session,
        private val stringProvider: StringProvider
) : VectorViewModel<JitsiCallViewState, JitsiCallViewActions, JitsiCallViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: JitsiCallViewState, args: XlinxSpaceActivity.Args): JitsiCallViewModel
    }

    init {
        val me = session.getUser(session.myUserId)?.toMatrixItem()
        val userInfo = JitsiMeetUserInfo().apply {
            displayName = args.nickName
            avatar = me?.avatarUrl?.let { session.contentUrlResolver().resolveFullSize(it) }?.let { URL(it) }
        }
        val roomName = args.roomId

        setState {
            copy(
                    userInfo = userInfo,
                    jitsiUrl = "https://x-linx.space",
                    subject = roomName
            )
        }
    }

    override fun handle(action: JitsiCallViewActions) {
    }

    companion object : MvRxViewModelFactory<JitsiCallViewModel, JitsiCallViewState> {

        const val ENABLE_VIDEO_OPTION = "ENABLE_VIDEO_OPTION"

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: JitsiCallViewState): JitsiCallViewModel? {
            val callActivity: XlinxSpaceActivity = viewModelContext.activity()
            val callArgs: XlinxSpaceActivity.Args = viewModelContext.args()
            return callActivity.viewModelFactory.create(state, callArgs)
        }

        override fun initialState(viewModelContext: ViewModelContext): JitsiCallViewState? {
            val args: XlinxSpaceActivity.Args = viewModelContext.args()

            return JitsiCallViewState(
                    roomId = args.roomId,
                    nickName = args.nickName,
                    enableVideo = args.enableVideo
            )
        }
    }
}
