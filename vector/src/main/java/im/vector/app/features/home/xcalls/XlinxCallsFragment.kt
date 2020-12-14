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

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.tencent.mmkv.MMKV
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListAnimator
import im.vector.app.features.home.room.list.RoomListParams
import im.vector.app.features.home.room.list.RoomListViewModel
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_list.*
import kotlinx.android.synthetic.main.xlinx_calls_main_fragment.*
import org.matrix.android.sdk.api.session.Session
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

class XlinxCallsFragment @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val sharedViewPool: RecyclerView.RecycledViewPool,
) : VectorBaseFragment(), OnBackPressed, NotifsFabMenuView.Listener {

    private lateinit var stateRestorer: LayoutManagerStateRestorer

    private val mmkv: MMKV = MMKV.mmkvWithID("xlinxcallhistory")
    private lateinit var xlinxCallsItemAdapter: XlinxCallsItemAdapter
    private var callItems: ArrayList<XlinxCallsItem> = arrayListOf()

    override fun getLayoutResId() = R.layout.xlinx_calls_main_fragment

    override fun getMenuRes() = R.menu.xlinx_call_history

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_calls_clear_history -> {
                mmkv.clearAll()
                clearCallData()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_calls_clear_history).isVisible = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
//        addCallData("qwe123ID", "aselole", "123", 2, 1)
        loadCallData()
        invalidateOptionsMenu()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        callListView.layoutManager = layoutManager
        callListView.itemAnimator = RoomListAnimator()
        callListView.setRecycledViewPool(sharedViewPool)
        layoutManager.recycleChildrenOnDetach = true
        xlinxCallsItemAdapter = XlinxCallsItemAdapter(callItems, session, avatarRenderer)
        callListView.adapter = xlinxCallsItemAdapter
    }

    private fun addCallData(targetRoomId: String, targetName: String, timestamp: String, callMode: Int, callType: Int) {
        callItems.add(XlinxCallsItem(targetRoomId, targetName, timestamp, callMode, callType))
        xlinxCallsItemAdapter.notifyItemInserted(callItems.size - 1)
    }

    private fun loadCallData() {
        if (mmkv.count() < 1) {
            return
        }

        val dateFormatter: SimpleDateFormat = getDetailedXlinxDateFormatter(requireContext(), Locale.getDefault())

        val kvdb: Array<String> = mmkv.allKeys()

        for (s in kvdb) {
            val tempRaw: String = mmkv.decodeString(s)
            val tempData = tempRaw.split(":::".toRegex()).toTypedArray()
            val targetRoomId = tempData[1]
            val targetName = tempData[2]
            val callMode = tempData[3].toInt()
            val callType = tempData[4].toInt()
            val callDate = dateFormatter.format(tempData[5].toLong())

            addCallData(targetRoomId, targetName, callDate, callMode, callType)
        }

    }

    private fun getDetailedXlinxDateFormatter(context: Context, locale: Locale): SimpleDateFormat {
        val dateFormatPattern: String
        dateFormatPattern = if (DateFormat.is24HourFormat(context)) {
            getLocalizedPattern("MMM d, HH:mm", locale)
        } else {
            getLocalizedPattern("MMM d, hh:mm", locale)
        }
        return SimpleDateFormat(dateFormatPattern, locale)
    }

    private fun getLocalizedPattern(template: String, locale: Locale): String {
        return DateFormat.getBestDateTimePattern(locale, template)
    }

    private fun clearCallData() {
        val size: Int = callItems.size
        callItems.clear()
        xlinxCallsItemAdapter.notifyItemRangeRemoved(0, size)
    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    override fun onDestroyView() {
        clearCallData()
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
