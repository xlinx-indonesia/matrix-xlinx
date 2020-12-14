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
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import kotlinx.android.synthetic.main.xlinx_item_calls_history.view.*

class XlinxCallsItemAdapter(
        private val calls: ArrayList<XlinxCallsItem>
) : RecyclerView.Adapter<XlinxCallsItemAdapter.CallHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): XlinxCallsItemAdapter.CallHolder {
        val inflatedView = parent.inflate(R.layout.xlinx_item_calls_history, false)
        return CallHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: XlinxCallsItemAdapter.CallHolder, position: Int) {
        val itemCall = calls[position]
        holder.bindCallItem(itemCall)
    }

    override fun getItemCount(): Int = calls.size




    //1
    class CallHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        //2
        private var view: View = v
        private var callItem: XlinxCallsItem? = null

        //3
        init {
            v.setOnClickListener(this)
        }

        //4
        @SuppressLint("LogNotTimber")
        override fun onClick(v: View) {
            Log.d("RecyclerView", "CLICK!")
        }

        fun bindCallItem(callItem: XlinxCallsItem) {
            this.callItem = callItem
            view.roomNameView.text                  = callItem.targetName
            view.roomLastEventTimeView.text         = callItem.timestamp.toString()
            view.roomLastEventView.text             = callItem.callMode.toString()
        }

        companion object {
            //5
            private val CALL_ITEM_KEY = "CALLSITEM"
        }
    }

}

