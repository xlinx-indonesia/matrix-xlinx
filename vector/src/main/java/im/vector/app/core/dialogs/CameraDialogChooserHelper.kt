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

package im.vector.app.core.dialogs

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.yalantis.ucrop.UCrop
import im.vector.app.R
import im.vector.app.core.extensions.insertBeforeLast
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_VIDEO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.features.attachments.AttachmentsHelper
import im.vector.app.features.attachments.ContactAttachment
import im.vector.app.features.attachments.toContentAttachmentData
import im.vector.app.features.media.createUCropWithDefaultSettings
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.entity.MultiPickerImageType
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import timber.log.Timber
import java.io.File

/**
 * Use to let the user choose between Camera (with permission handling) and Gallery (with single image selection),
 * then edit the image
 * [Listener.onImageReady] will be called with an uri of a square image store in the cache of the application.
 * It's up to the caller to delete the file.
 */
class CameraDialogChooserHelper(
        // must implement GalleryOrCameraDialogHelper.Listener
        private val fragment: Fragment,
        private val colorProvider: ColorProvider,
        val callback: Callback
) {
//    interface Listener {
//        fun onCameraResultReady(uri: Uri?)
//    }

    interface Callback {
        fun onCameraContentAttachmentsReady(attachments: List<ContentAttachmentData>)
    }

    private val activity
        get() = fragment.requireActivity()

//    private val listener = fragment as? Listener ?: error("Fragment must implement GalleryOrCameraDialogHelper.Listener")

    private val takePhotoPermissionActivityResultLauncher = fragment.registerForPermissionsResult { allGranted ->
        if (allGranted) {
            doOpenCamera()
        }
    }

    private val takeVideoPermissionActivityResultLauncher = fragment.registerForPermissionsResult { allGranted ->
        if (allGranted) {
            doOpenVideoCamera()
        }
    }

    private val takePhotoActivityResultLauncher = fragment.registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            photoCameraUri?.let { uri ->
                MultiPicker.get(MultiPicker.CAMERA)
                        .getTakenPhoto(activity, uri)
                        ?.let {
                            callback.onCameraContentAttachmentsReady(
                                    listOf(it).map { it.toContentAttachmentData() }
                            )
                        }
            }
        }
    }

    private val takeVideoActivityResultLauncher = fragment.registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            videoCameraUri?.let { uri ->
                MultiPicker.get(MultiPicker.CAMERA)
                        .getTakenVideo(activity, uri)
                        ?.let {
                            callback.onCameraContentAttachmentsReady(
                                    listOf(it).map { it.toContentAttachmentData() }
                            )
                        }
            }
        }
    }


    private enum class Type {
        Photo,
        Video
    }

    fun show() {
        AlertDialog.Builder(activity)
                .setTitle(R.string.attachment_type_camera)
                .setItems(arrayOf(
                        fragment.getString(R.string.option_take_photo),
                        fragment.getString(R.string.option_take_video)
                )) { _, which ->
                    onCameraTypeSelected(if (which == 0) Type.Photo else Type.Video)
                }
                .setPositiveButton(R.string.cancel, null)
                .show()
    }

    private fun onCameraTypeSelected(type: Type) {
        when (type) {
            Type.Photo ->
                if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, activity, takePhotoPermissionActivityResultLauncher)) {
                    doOpenCamera()
                }
            Type.Video ->
                if (checkPermissions(PERMISSIONS_FOR_TAKING_VIDEO, activity, takeVideoPermissionActivityResultLauncher)) {
                    doOpenVideoCamera()
                }
        }
    }

    private var photoCameraUri: Uri? = null
    private var videoCameraUri: Uri? = null
    private fun doOpenCamera() {
        photoCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(activity, takePhotoActivityResultLauncher)
    }
    private fun doOpenVideoCamera() {
        videoCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingVideoFile(activity, takeVideoActivityResultLauncher)
    }
}
