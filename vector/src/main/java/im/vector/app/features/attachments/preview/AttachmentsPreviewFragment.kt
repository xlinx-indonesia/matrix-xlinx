/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.attachments.preview

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yalantis.ucrop.UCrop
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.insertBeforeLast
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.OnSnapPositionChangeListener
import im.vector.app.core.utils.SnapOnScrollListener
import im.vector.app.core.utils.attachSnapHelperWithListener
import im.vector.app.features.attachments.preview.photoeditor.EmojiBSFragment
import im.vector.app.features.attachments.preview.photoeditor.PropertiesBSFragment
import im.vector.app.features.attachments.preview.photoeditor.StickerBSFragment
import im.vector.app.features.attachments.preview.photoeditor.TextEditorDialogFragment
import im.vector.app.features.attachments.preview.photoeditor.filters.FilterListener
import im.vector.app.features.attachments.preview.photoeditor.filters.FilterViewAdapter
import im.vector.app.features.attachments.preview.photoeditor.tools.EditingToolsAdapter
import im.vector.app.features.attachments.preview.photoeditor.tools.ToolType
import im.vector.app.features.media.createUCropWithDefaultSettings
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditor.OnSaveListener
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.PhotoFilter
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_attachments_preview.*
import kotlinx.android.synthetic.main.item_attachment_big_preview.*
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

@Parcelize
data class AttachmentsPreviewArgs(
        val attachments: List<ContentAttachmentData>
) : Parcelable

@Suppress("NAME_SHADOWING")
@SuppressLint("LogNotTimber")
class AttachmentsPreviewFragment @Inject constructor(
        private val attachmentMiniaturePreviewController: AttachmentMiniaturePreviewController,
        private val attachmentBigPreviewController: AttachmentBigPreviewController,
        private val colorProvider: ColorProvider
) : VectorBaseFragment(), AttachmentMiniaturePreviewController.Callback, OnPhotoEditorListener,
        View.OnClickListener,
        PropertiesBSFragment.Properties,
        EmojiBSFragment.EmojiListener,
        StickerBSFragment.StickerListener, EditingToolsAdapter.OnItemSelected, FilterListener {

    private val fragmentArgs: AttachmentsPreviewArgs by args()
    private val viewModel: AttachmentsPreviewViewModel by fragmentViewModel()

    var mPhotoEditor: PhotoEditor? = null
    private var mPhotoEditorView: PhotoEditorView? = null
    private var mPropertiesBSFragment: PropertiesBSFragment? = null
    private var mEmojiBSFragment: EmojiBSFragment? = null
    private var mStickerBSFragment: StickerBSFragment? = null
    private var mTxtCurrentTool: TextView? = null
    private var mWonderFont: Typeface? = null
    private var mRvTools: RecyclerView? = null
    private var mRvFilters: RecyclerView? = null
    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    private val mFilterViewAdapter: FilterViewAdapter = FilterViewAdapter(this)
    private var mRootView: ConstraintLayout? = null
    private val mConstraintSet = ConstraintSet()
    private var mIsFilterVisible = false
    private var imgUndo: ImageView? = null
    private var imgRedo: ImageView? = null
//    private var imgCamera: ImageView? = null
//    private var imgGallery: ImageView? = null
    private var imgSave: ImageView? = null
    private var imgClose: ImageView? = null
//    private var imgShare: ImageView? = null

    @VisibleForTesting
    var mSaveImageUri: Uri? = null


    override fun getLayoutResId() = R.layout.fragment_attachments_preview

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()
        setupRecyclerViews()
        setupToolbar(attachmentPreviewerToolbar)
        attachmentPreviewerSendButton.setOnClickListener {
            setResultAndFinish()
        }
        initPhotoEditorView(view)
        initPhotoEditorTools()
        showPhotoEditorTools(false)
    }

    private fun initPhotoEditorView(view: View) {
        mPhotoEditorView = view.findViewById(R.id.photoEditorView)
        mTxtCurrentTool = view.findViewById(R.id.txtCurrentTool)
        mRvTools = view.findViewById(R.id.rvConstraintTools)
        mRvFilters = view.findViewById(R.id.rvFilterView)
        mRootView = view.findViewById(R.id.rootView)

        imgUndo = view.findViewById(R.id.imgUndo)
        imgUndo?.setOnClickListener(this)

        imgRedo = view.findViewById(R.id.imgRedo)
        imgRedo?.setOnClickListener(this)

//        imgCamera = findViewById(R.id.imgCamera)
//        imgCamera.setOnClickListener(this)
//
//        imgGallery = findViewById(R.id.imgGallery)
//        imgGallery.setOnClickListener(this)

        imgSave = view.findViewById(R.id.imgSave)
        imgSave?.setOnClickListener(this)

        imgClose = view.findViewById(R.id.imgClose)
        imgClose?.setOnClickListener(this)

//        imgShare = findViewById(R.id.imgShare)
//        imgShare.setOnClickListener(this)
    }

    private fun showPhotoEditorTools(show: Boolean) {
        if (show) {
            mPhotoEditorView?.isVisible = true
            mTxtCurrentTool?.isVisible = false
            mRvFilters?.isVisible = true
            mRvTools?.isVisible = true
            imgUndo?.isVisible = true
            imgRedo?.isVisible = true
            imgSave?.isVisible = true
            imgClose?.isVisible = true
            attachmentPreviewerBottomContainer.isVisible = false
            attachmentPreviewerSendButton.isVisible = false
            attachmentPreviewerBigList.isVisible = false
            attachmentPreviewerToolbar.isVisible = false
        } else {
            mPhotoEditorView?.isVisible = false
            mTxtCurrentTool?.isVisible = false
            mRvFilters?.isVisible = false
            mRvTools?.isVisible = false
            imgUndo?.isVisible = false
            imgRedo?.isVisible = false
            imgSave?.isVisible = false
            imgClose?.isVisible = false
            attachmentPreviewerBottomContainer.isVisible = true
            attachmentPreviewerSendButton.isVisible = true
            attachmentPreviewerBigList.isVisible = true
            attachmentPreviewerToolbar.isVisible = true
        }
    }

    private fun initPhotoEditorTools() {
        mWonderFont = Typeface.createFromAsset(requireContext().assets, "beyond_wonderland.ttf")

        mPropertiesBSFragment = PropertiesBSFragment()
        mEmojiBSFragment = EmojiBSFragment()
        mStickerBSFragment = StickerBSFragment()
        mStickerBSFragment?.setStickerListener(this)
        mEmojiBSFragment?.setEmojiListener(this)
        mPropertiesBSFragment?.setPropertiesChangeListener(this)

        val llmTools = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        mRvTools?.layoutManager = llmTools
        mRvTools?.adapter = mEditingToolsAdapter

        val llmFilters = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        mRvFilters?.layoutManager = llmFilters
        mRvFilters?.adapter = mFilterViewAdapter
    }

    private val uCropActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            val resultUri = activityResult.data?.let { UCrop.getOutput(it) }
            if (resultUri != null) {
                viewModel.handle(AttachmentsPreviewAction.UpdatePathOfCurrentAttachment(resultUri))
            } else {
                Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.attachmentsPreviewRemoveAction -> {
                handleRemoveAction()
                true
            }
            R.id.attachmentsPreviewCropAction -> {
                handleEditAction()
                true
            }
            R.id.attachmentsPreviewEditAction -> {
                handlePhotoEditorAction()
                true
            }
            else                                -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) { state ->
            val editMenuItem = menu.findItem(R.id.attachmentsPreviewEditAction)
            val cropMenuItem = menu.findItem(R.id.attachmentsPreviewCropAction)
            val showEditMenuItem = state.attachments.getOrNull(state.currentAttachmentIndex)?.isEditable().orFalse()
            editMenuItem.setVisible(showEditMenuItem)
            cropMenuItem.setVisible(showEditMenuItem)
        }

        super.onPrepareOptionsMenu(menu)
    }

    override fun getMenuRes() = R.menu.vector_attachments_preview

    override fun onDestroyView() {
        super.onDestroyView()
        attachmentPreviewerMiniatureList.cleanup()
        attachmentPreviewerBigList.cleanup()
        attachmentMiniaturePreviewController.callback = null
    }

    override fun invalidate() = withState(viewModel) { state ->
        invalidateOptionsMenu()
        if (state.attachments.isEmpty()) {
            requireActivity().setResult(RESULT_CANCELED)
            requireActivity().finish()
        } else {
            attachmentMiniaturePreviewController.setData(state)
            attachmentBigPreviewController.setData(state)
            attachmentPreviewerBigList.scrollToPosition(state.currentAttachmentIndex)
            attachmentPreviewerMiniatureList.scrollToPosition(state.currentAttachmentIndex)
            attachmentPreviewerSendImageOriginalSize.text = resources.getQuantityString(R.plurals.send_images_with_original_size, state.attachments.size)
        }
    }

    override fun onAttachmentClicked(position: Int, contentAttachmentData: ContentAttachmentData) {
        viewModel.handle(AttachmentsPreviewAction.SetCurrentAttachment(position))
    }

    private fun setResultAndFinish() = withState(viewModel) {
        (requireActivity() as? AttachmentsPreviewActivity)?.setResultAndFinish(
                it.attachments,
                attachmentPreviewerSendImageOriginalSize.isChecked
        )
    }

    private fun applyInsets() {
        view?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        ViewCompat.setOnApplyWindowInsetsListener(attachmentPreviewerBottomContainer) { v, insets ->
            v.updatePadding(bottom = insets.systemWindowInsetBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(attachmentPreviewerToolbar) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.systemWindowInsetTop
            }
            insets
        }
    }

    private fun handleRemoveAction() {
        viewModel.handle(AttachmentsPreviewAction.RemoveCurrentAttachment)
    }

    private fun handleEditAction() = withState(viewModel) {
        val currentAttachment = it.attachments.getOrNull(it.currentAttachmentIndex) ?: return@withState
        val destinationFile = File(requireContext().cacheDir, currentAttachment.name.insertBeforeLast("_edited_image_${System.currentTimeMillis()}"))
        val uri = currentAttachment.queryUri
        createUCropWithDefaultSettings(colorProvider, uri, destinationFile.toUri(), currentAttachment.name)
                .getIntent(requireContext())
                .let { intent -> uCropActivityResultLauncher.launch(intent) }
    }

    private fun handlePhotoEditorAction() = withState(viewModel) {
        showPhotoEditorTools(true)
        val currentAttachment = it.attachments.getOrNull(it.currentAttachmentIndex) ?: return@withState
//        val destinationFile = File(requireContext().cacheDir, currentAttachment.name.insertBeforeLast("_edited_image_${System.currentTimeMillis()}"))
        val uri = currentAttachment.queryUri
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            mPhotoEditorView?.source?.setImageDrawable(Drawable.createFromStream(inputStream, uri.toString()))
            mPhotoEditor = PhotoEditor.Builder(requireContext(), mPhotoEditorView)
                    .setPinchTextScalable(true)
                    .build()
            mPhotoEditor?.setOnPhotoEditorListener(this)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerViews() {
        attachmentMiniaturePreviewController.callback = this

        attachmentPreviewerMiniatureList.let {
            it.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            it.setHasFixedSize(true)
            it.adapter = attachmentMiniaturePreviewController.adapter
        }

        attachmentPreviewerBigList.let {
            it.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            it.attachSnapHelperWithListener(
                    PagerSnapHelper(),
                    SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE,
                    object : OnSnapPositionChangeListener {
                        override fun onSnapPositionChange(position: Int) {
                            viewModel.handle(AttachmentsPreviewAction.SetCurrentAttachment(position))
                        }
                    })
            it.setHasFixedSize(true)
            it.adapter = attachmentBigPreviewController.adapter
        }
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment: TextEditorDialogFragment? = text?.let { TextEditorDialogFragment.show(requireActivity(), it, colorCode) }
        textEditorDialogFragment?.setOnTextEditorListener { inputText, colorCode ->
            val styleBuilder = TextStyleBuilder()
            styleBuilder.withTextColor(colorCode)
            mPhotoEditor!!.editText(rootView!!, inputText, styleBuilder)
            mTxtCurrentTool?.setText(R.string.app_name)
        }
    }

    override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d("photoeditor", "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]")
    }

    override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d("photoeditor", "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]")
    }

    override fun onStartViewChangeListener(viewType: ViewType) {
        Log.d("photoeditor", "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType) {
        Log.d("photoeditor", "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> mPhotoEditor?.undo()
            R.id.imgRedo -> mPhotoEditor?.redo()
            R.id.imgSave -> saveImage()
            R.id.imgClose -> buttonClose()
//            R.id.imgShare -> shareImage()
//            R.id.imgCamera -> {
//                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                startActivityForResult(cameraIntent, com.burhanrashid52.photoeditor.EditImageActivity.CAMERA_REQUEST)
//            }
//            R.id.imgGallery -> {
//                val intent = Intent()
//                intent.type = "image/*"
//                intent.action = Intent.ACTION_GET_CONTENT
//                startActivityForResult(Intent.createChooser(intent, "Select Picture"), com.burhanrashid52.photoeditor.EditImageActivity.PICK_REQUEST)
//            }
        }
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor!!.brushColor = colorCode
        mTxtCurrentTool?.setText(R.string.app_name)
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor!!.setOpacity(opacity)
        mTxtCurrentTool?.setText(R.string.app_name)
    }

    override fun onBrushSizeChanged(brushSize: Int) {
        mPhotoEditor!!.brushSize = brushSize.toFloat()
        mTxtCurrentTool?.setText(R.string.app_name)
    }

    override fun onEmojiClick(emojiUnicode: String?) {
        mPhotoEditor!!.addEmoji(emojiUnicode)
        mTxtCurrentTool?.setText(R.string.app_name)
    }

    override fun onStickerClick(bitmap: Bitmap?) {
        mPhotoEditor!!.addImage(bitmap)
        mTxtCurrentTool?.setText(R.string.app_name)
    }

    override fun onFilterSelected(photoFilter: PhotoFilter?) {
        mPhotoEditor!!.setFilterEffect(photoFilter)
    }

    override fun onToolSelected(toolType: ToolType?) {
        when (toolType) {
            ToolType.BRUSH -> {
                mPhotoEditor!!.setBrushDrawingMode(true)
                mTxtCurrentTool?.setText(R.string.app_name)
                showBottomSheetDialogFragment(mPropertiesBSFragment)
            }
            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(requireActivity())
                textEditorDialogFragment.setOnTextEditorListener { inputText, colorCode ->
                    val styleBuilder = TextStyleBuilder()
                    styleBuilder.withTextColor(colorCode)
                    mPhotoEditor!!.addText(inputText, styleBuilder)
                    mTxtCurrentTool?.setText(R.string.app_name)
                }
            }
            ToolType.ERASER -> {
                mPhotoEditor!!.brushEraser()
                mTxtCurrentTool?.setText(R.string.app_name)
            }
            ToolType.FILTER -> {
                mTxtCurrentTool?.setText(R.string.app_name)
                showFilter(true)
            }
            ToolType.EMOJI -> showBottomSheetDialogFragment(mEmojiBSFragment)
            ToolType.STICKER -> showBottomSheetDialogFragment(mStickerBSFragment)
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(requireActivity().supportFragmentManager, fragment.tag)
    }

    fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(mRootView)
        if (isVisible) {
            mConstraintSet.clear(mRvFilters!!.id, ConstraintSet.START)
            mConstraintSet.connect(mRvFilters!!.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START)
            mConstraintSet.connect(mRvFilters!!.id, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END)
        } else {
            mConstraintSet.connect(mRvFilters!!.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.END)
            mConstraintSet.clear(mRvFilters!!.id, ConstraintSet.END)
        }
        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        TransitionManager.beginDelayedTransition(mRootView!!, changeBounds)
        mConstraintSet.applyTo(mRootView)
    }

    @SuppressLint("MissingPermission")
    private fun saveImage() = withState(viewModel) {
        val currentAttachment = it.attachments.getOrNull(it.currentAttachmentIndex) ?: return@withState
        val destinationFile = File(requireContext().cacheDir, currentAttachment.name.insertBeforeLast("_edited_image_${System.currentTimeMillis()}"))
        try {
            val saveSettings = SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build()
            mPhotoEditor!!.saveAsFile(destinationFile.absolutePath, saveSettings, object : OnSaveListener {
                override fun onSuccess(imagePath: String) {
                    mSaveImageUri = Uri.fromFile(File(imagePath))
                    mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                    if (mSaveImageUri != null) {
                        viewModel.handle(AttachmentsPreviewAction.UpdatePathOfCurrentAttachment(mSaveImageUri!!))
                        showPhotoEditorTools(false)
                    } else {
                        Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(exception: Exception) {
                }
            })
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(getString(R.string.save))
        builder.setPositiveButton("Save") { _, _ ->
            run {
                saveImage()
                showPhotoEditorTools(false)
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            run {
                dialog.dismiss()
                showPhotoEditorTools(true)
            }
        }
        builder.setNeutralButton("Discard") { _, _ -> showPhotoEditorTools(false) }
        builder.create().show()
    }

    private fun buttonClose() {
        if (mIsFilterVisible) {
            showFilter(false)
            mTxtCurrentTool!!.setText(R.string.app_name)
        } else if (!mPhotoEditor!!.isCacheEmpty) {
            showSaveDialog()
        } else {
            showPhotoEditorTools(false)
        }
    }

//    override fun onBackPressed() {
//        if (mIsFilterVisible) {
//            showFilter(false)
//            mTxtCurrentTool!!.setText(R.string.app_name)
//        } else if (!mPhotoEditor!!.isCacheEmpty) {
//            showSaveDialog()
//        } else {
//            super.onBackPressed()
//        }
//    }
}
