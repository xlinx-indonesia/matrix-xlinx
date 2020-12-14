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

package im.vector.app.features.home.xspace

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import butterknife.OnClick
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.features.call.conference.VectorJitsiActivity
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import im.vector.app.features.home.xspace.conference.XlinxSpaceActivity
import im.vector.app.features.login.LoginAction
import im.vector.app.features.notifications.NotificationDrawerManager
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_login_server_url_form.*
import kotlinx.android.synthetic.main.fragment_room_list.*
import kotlinx.android.synthetic.main.xlinx_space_main_fragment.*
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import java.net.URL
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

class XlinxSpaceFragment @Inject constructor(
        private val session: Session,
        private val notificationDrawerManager: NotificationDrawerManager,
) : VectorBaseFragment(), OnBackPressed, NotifsFabMenuView.Listener {

    private lateinit var stateRestorer: LayoutManagerStateRestorer

    override fun getLayoutResId() = R.layout.xlinx_space_main_fragment

    private var hasUnreadRooms = false

//    override fun getMenuRes() = R.menu.room_list

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.menu_home_mark_all_as_read -> {
//                roomListViewModel.handle(RoomListAction.MarkAllRoomsRead)
//                return true
//            }
//        }
//
//        return super.onOptionsItemSelected(item)
//    }

//    override fun onPrepareOptionsMenu(menu: Menu) {
//        menu.findItem(R.id.menu_home_mark_all_as_read).isVisible = hasUnreadRooms
//        super.onPrepareOptionsMenu(menu)
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpaceRoomDetails()
        setupSelfInformation()
    }

    private fun setupSpaceRoomDetails() {
        meetingUsername.textChanges()
                .subscribe {
                    createMeetingButton.isEnabled = it.isNotBlank()
                }
                .disposeOnDestroyView()

        meetingUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        meetingRoomName.textChanges()
                .subscribe {
                    createMeetingButton.isEnabled = it.isNotBlank()
                }
                .disposeOnDestroyView()

        meetingRoomName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupSelfInformation() {
        val me = session.getUser(session.myUserId)?.toMatrixItem()
        meetingUsername.setText(me?.getBestName())
    }

    @SuppressLint("SetTextI18n")
    @OnClick(R.id.createMeetingButton)
    fun submit() {
        val configUsername = meetingUsername.text.toString()
        val configRoomname = meetingRoomName.text.toString()

        when {
            configUsername.isBlank() -> {
                createMeetingButton.isEnabled = false
                Toast.makeText(requireContext(), "Please fill your nickname", Toast.LENGTH_LONG).show()
            }
            configRoomname.isBlank() -> {
                createMeetingButton.isEnabled = false
                Toast.makeText(requireContext(), "Please fill the room name", Toast.LENGTH_LONG).show()
            }
            else                -> {
                createMeetingButton.isEnabled = true
                requireContext().startActivity(XlinxSpaceActivity.newIntent(requireContext(), roomId = configRoomname, nickName = configUsername, enableVideo = true))
            }
        }
    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }


    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (createChatFabMenu.onBackPressed()) {
            return true
        }
        return false
    }

    override fun createDirectChat() {
        TODO("Not yet implemented")
    }

    override fun openRoomDirectory(initialFilter: String) {
        TODO("Not yet implemented")
    }
}
