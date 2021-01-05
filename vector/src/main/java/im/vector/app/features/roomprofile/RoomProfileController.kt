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
 *
 */

package im.vector.app.features.roomprofile

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.expandableTextItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.home.ShortcutCreator
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class RoomProfileController @Inject constructor(
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val shortcutCreator: ShortcutCreator,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomProfileViewState>() {

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    interface Callback {
        fun onLearnMoreClicked()
        fun onEnableEncryptionClicked()
        fun onMemberListClicked()
        fun onBannedMemberListClicked()
        fun onNotificationsClicked()
        fun onUploadsClicked()
        fun createShortcut()
        fun onSettingsClicked()
        fun onLeaveRoomClicked()
        fun onRoomIdClicked()
    }

    override fun buildModels(data: RoomProfileViewState?) {
        if (data == null) {
            return
        }
        val roomSummary = data.roomSummary() ?: return

        // Topic
        roomSummary
                .topic
                .takeIf { it.isNotEmpty() }
                ?.let {
                    buildProfileSection(stringProvider.getString(R.string.room_settings_topic))
                    expandableTextItem {
                        id("topic")
                        content(it)
                        maxLines(2)
                    }
                }

        // Security
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_security))
        val learnMoreSubtitle = if (roomSummary.isEncrypted) {
            if (roomSummary.isDirect) R.string.direct_room_profile_encrypted_subtitle else R.string.room_profile_encrypted_subtitle
        } else {
            if (roomSummary.isDirect) R.string.direct_room_profile_not_encrypted_subtitle else R.string.room_profile_not_encrypted_subtitle
        }
        genericFooterItem {
            id("e2e info")
            centered(false)
            text(stringProvider.getString(learnMoreSubtitle))
        }
        buildEncryptionAction(data.actionPermissions, roomSummary)

        // More
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_more))
        buildProfileAction(
                id = "settings",
                title = stringProvider.getString(if (roomSummary.isDirect) {
                    R.string.direct_room_profile_section_more_settings
                } else {
                    R.string.room_profile_section_more_settings
                }),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_settings,
                action = { callback?.onSettingsClicked() }
        )
        buildProfileAction(
                id = "notifications",
                title = stringProvider.getString(R.string.room_profile_section_more_notifications),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_notification,
                action = { callback?.onNotificationsClicked() }
        )
        val numberOfMembers = roomSummary.joinedMembersCount ?: 0
        val hasWarning = roomSummary.isEncrypted && roomSummary.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Warning
        buildProfileAction(
                id = "member_list",
                title = stringProvider.getQuantityString(R.plurals.room_profile_section_more_member_list, numberOfMembers, numberOfMembers),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_member_list,
                accessory = R.drawable.ic_shield_warning.takeIf { hasWarning } ?: 0,
                action = { callback?.onMemberListClicked() }
        )

        if (data.bannedMembership.invoke()?.isNotEmpty() == true) {
            buildProfileAction(
                    id = "banned_list",
                    title = stringProvider.getString(R.string.room_settings_banned_users_title),
                    dividerColor = dividerColor,
                    icon = R.drawable.ic_settings_root_labs,
                    action = { callback?.onBannedMemberListClicked() }
            )
        }
        buildProfileAction(
                id = "uploads",
                title = stringProvider.getString(R.string.room_profile_section_more_uploads),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_uploads,
                action = { callback?.onUploadsClicked() }
        )
        if (shortcutCreator.canCreateShortcut()) {
            buildProfileAction(
                    id = "shortcut",
                    title = stringProvider.getString(R.string.room_settings_add_homescreen_shortcut),
                    dividerColor = dividerColor,
                    editable = false,
                    icon = R.drawable.ic_add_to_home_screen_24dp,
                    action = { callback?.createShortcut() }
            )
        }
        buildProfileAction(
                id = "leave",
                title = stringProvider.getString(if (roomSummary.isDirect) {
                    R.string.direct_room_profile_section_more_leave
                } else {
                    R.string.room_profile_section_more_leave
                }),
                dividerColor = dividerColor,
                divider = false,
                destructive = true,
                icon = R.drawable.ic_room_actions_leave,
                editable = false,
                action = { callback?.onLeaveRoomClicked() }
        )

        // Advanced
        if (vectorPreferences.developerMode()) {
            buildProfileSection(stringProvider.getString(R.string.room_settings_category_advanced_title))
            buildProfileAction(
                    id = "roomId",
                    title = stringProvider.getString(R.string.room_settings_room_internal_id),
                    subtitle = roomSummary.roomId,
                    dividerColor = dividerColor,
                    divider = false,
                    editable = false,
                    action = { callback?.onRoomIdClicked() }
            )
        }
    }

    private fun buildEncryptionAction(actionPermissions: RoomProfileViewState.ActionPermissions, roomSummary: RoomSummary) {
        if (!roomSummary.isEncrypted) {
            if (actionPermissions.canEnableEncryption) {
                buildProfileAction(
                        id = "enableEncryption",
                        title = stringProvider.getString(R.string.room_settings_enable_encryption),
                        dividerColor = dividerColor,
//                        icon = R.drawable.ic_shield_black,
                        icon = R.drawable.ic_shield_safe_xlinx,
                        divider = false,
                        editable = false,
                        action = { callback?.onEnableEncryptionClicked() }
                )
            } else {
                buildProfileAction(
                        id = "enableEncryption",
                        title = stringProvider.getString(R.string.room_settings_enable_encryption_no_permission),
                        dividerColor = dividerColor,
//                        icon = R.drawable.ic_shield_black,
                        icon = R.drawable.ic_shield_safe_xlinx,
                        divider = false,
                        editable = false
                )
            }
        }
    }
}
