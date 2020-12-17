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
import im.vector.lib.multipicker.entity.MultiPickerAudioType

/**
 * Audio file picker implementation
 */
class AudioPicker : Picker<MultiPickerAudioType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected audio files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerAudioType> {
        val audioList = mutableListOf<MultiPickerAudioType>()

        getSelectedUriList(data).forEach { selectedUri ->
            val projection = arrayOf(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.SIZE
            )

            context.contentResolver.query(
                    selectedUri,
                    projection,
                    null,
                    null,
                    null
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

                if (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    var duration = 0L

                    context.contentResolver.openFileDescriptor(selectedUri, "r")?.use { pfd ->
                        val mediaMetadataRetriever = MediaMetadataRetriever()
                        mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                        duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                    }

                    audioList.add(
                            MultiPickerAudioType(
                                    name,
                                    size,
                                    context.contentResolver.getType(selectedUri),
                                    selectedUri,
                                    duration
                            )
                    )
                }
            }
        }
        return audioList
    }

    fun getAudioFile(audioUri: Uri, audioName: String, audioSize: Long, audioDuration: Long, mimeTypeX: String): List<MultiPickerAudioType> {
        val audioList = mutableListOf<MultiPickerAudioType>()

//        var duration = 0L

//        context.contentResolver.openFileDescriptor(audioUri, "r")?.use { pfd ->
//            val mediaMetadataRetriever = MediaMetadataRetriever()
//            mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
//            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
//        }

//        val mediaMetadataRetriever = MediaMetadataRetriever()
//        mediaMetadataRetriever.setDataSource(audioUri.path)
//        val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()

        audioList.add(
                MultiPickerAudioType(
                        audioName,
                        audioSize,
                        mimeTypeX,
                        audioUri,
                        audioDuration
                )
        )

        return audioList
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "audio/*"
        }
    }
}
