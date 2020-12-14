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

import kotlin.properties.Delegates

data class XlinxCallsItem(
        val targetRoomId: String = "",
        val targetName: String = "",
        val timestamp: String = "",
        val callMode: Int,
        val callType: Int
) {

    class Builder {
        private var targetRoomId : String = ""
        private var targetName : String = ""
        private var timestamp : String = ""
        private var callMode by Delegates.notNull<Int>()
        private var callType by Delegates.notNull<Int>()

        fun targetRoomId(targetRoomId: String) = apply { this.targetRoomId = targetRoomId }
        fun targetName(targetName: String) = apply { this.targetName = targetName }
        fun timestamp(timestamp: String) = apply { this.timestamp = timestamp }
        fun callMode(callMode: Int) = apply { this.callMode = callMode }
        fun callType(callType: Int) = apply { this.callType = callType }

        fun build() = XlinxCallsItem(
                targetRoomId,
                targetName,
                timestamp,
                callMode,
                callType
        )
    }
}

