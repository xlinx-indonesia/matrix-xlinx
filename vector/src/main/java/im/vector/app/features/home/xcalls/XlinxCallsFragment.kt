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

package im.vector.app.features.home.xcalls

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.tencent.mmkv.MMKV
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListAnimator
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_list.*
import kotlinx.android.synthetic.main.xlinx_calls_main_fragment.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

class XlinxCallsFragment @Inject constructor(
        private val sharedViewPool: RecyclerView.RecycledViewPool
) : VectorBaseFragment(), OnBackPressed, NotifsFabMenuView.Listener {

    private val mmkv: MMKV = MMKV.mmkvWithID("callhistory")
    private lateinit var xlinxCallsItemAdapter: XlinxCallsItemAdapter
    private lateinit var stateRestorer: LayoutManagerStateRestorer
    private var modelBuildListener: OnModelBuildFinishedListener? = null
//    private var callItems: ArrayList<XlinxCallsItem>? = null

    override fun getLayoutResId() = R.layout.xlinx_calls_main_fragment

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

//        setupRecyclerView()
//        addCallData("qwe123ID", "aselole", 123,2, 1)
    }

//    private fun setupRecyclerView() {
//        val layoutManager = LinearLayoutManager(context)
//        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
//        callListView.layoutManager = layoutManager
//        callListView.itemAnimator = RoomListAnimator()
//        callListView.setRecycledViewPool(sharedViewPool)
//        layoutManager.recycleChildrenOnDetach = true
//        xlinxCallsItemAdapter = XlinxCallsItemAdapter(callItems)
//        callListView.adapter = xlinxCallsItemAdapter
//        stateView.contentView = callListView
//    }
//
//    private fun addCallData(targetRoomId: String, targetName: String, timestamp: Long, callMode: Int, callType: Int) {
//        callItems.add(XlinxCallsItem(targetRoomId, targetName, timestamp, callMode, callType))
//        xlinxCallsItemAdapter.notifyItemInserted(callItems.size-1)
//    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    override fun onDestroyView() {
        callListView.cleanup()
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
