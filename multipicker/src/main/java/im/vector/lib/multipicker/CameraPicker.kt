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

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import im.vector.lib.multipicker.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of taking a photo with Camera
 */
class CameraPicker {

    /**
     * Start camera by using a ActivityResultLauncher
     * @return Uri of taken photo or null if the operation is cancelled.
     */
    fun startWithExpectingFile(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>): Uri? {
        val photoUri = createPhotoUri(context)
        val intent = createIntent().apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        activityResultLauncher.launch(intent)
        return photoUri
    }

    fun startWithExpectingVideoFile(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>): Uri? {
        val videoUri = createVideoUri(context)
        val intent = createVideoIntent().apply {
            putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
        }
        activityResultLauncher.launch(intent)
        return videoUri
    }

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return Taken photo or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenPhoto(context: Context, photoUri: Uri): MultiPickerImageType? {
        val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
        )

        context.contentResolver.query(
                photoUri,
                projection,
                null,
                null,
                null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

            if (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)

                val bitmap = ImageUtils.getBitmap(context, photoUri)
                val orientation = ImageUtils.getOrientation(context, photoUri)

                return MultiPickerImageType(
                        name,
                        size,
                        context.contentResolver.getType(photoUri),
                        photoUri,
                        bitmap?.width ?: 0,
                        bitmap?.height ?: 0,
                        orientation
                )
            }
        }
        return null
    }

    fun getTakenVideo(context: Context, videoUri: Uri): MultiPickerVideoType? {
        val projection = arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE
        )

        context.contentResolver.query(
                videoUri,
                projection,
                null,
                null,
                null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)

            if (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                var duration = 0L
                var width = 0
                var height = 0
                var orientation = 0

                context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                    duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                    width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
                    height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
                    orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
                }

                return MultiPickerVideoType(
                        name,
                        size,
                        context.contentResolver.getType(videoUri),
                        videoUri,
                        width,
                        height,
                        orientation,
                        duration
                )
            }
        }
        return null
    }

    private fun createIntent(): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    }

    private fun createVideoIntent(): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
    }

    companion object {
        fun createPhotoUri(context: Context): Uri {
            val file = createImageFile(context)
            val authority = context.packageName + ".multipicker.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        }

        fun createVideoUri(context: Context): Uri {
            val file = createVideoFile(context)
            val authority = context.packageName + ".multipicker.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        }

        private fun createImageFile(context: Context): File {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = context.filesDir
            return File.createTempFile(
                    "${timeStamp}_", /* prefix */
                    ".jpg", /* suffix */
                    storageDir /* directory */
            )
        }

        private fun createVideoFile(context: Context): File {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = context.filesDir
            return File.createTempFile(
                    "${timeStamp}_", /* prefix */
                    ".mp4", /* suffix */
                    storageDir /* directory */
            )
        }
    }
}
