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

@file:Suppress("DEPRECATION")

package im.vector.app.features.home.room.detail

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Parcelable
import android.os.Vibrator
import android.text.Spannable
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import butterknife.BindView
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.epoxy.addGlidePreloader
import com.airbnb.epoxy.glidePreloader
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.dialogs.CameraDialogChooserHelper
import im.vector.app.core.dialogs.ConfirmationDialogBuilder
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.dialogs.withColoredButton
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.extensions.trackItemsVisibilityChange
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.glide.GlideRequests
import im.vector.app.core.intent.getFilenameFromUri
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DateProvider
import im.vector.app.core.ui.views.ActiveCallView
import im.vector.app.core.ui.views.ActiveCallViewHolder
import im.vector.app.core.ui.views.ActiveConferenceView
import im.vector.app.core.ui.views.JumpToReadMarkerView
import im.vector.app.core.ui.views.NotificationAreaView
import im.vector.app.core.utils.Debouncer
import im.vector.app.core.utils.KeyboardStateUtils
import im.vector.app.core.utils.PERMISSIONS_FOR_AUDIO_IP_CALL
import im.vector.app.core.utils.PERMISSIONS_FOR_VIDEO_IP_CALL
import im.vector.app.core.utils.PERMISSIONS_FOR_WRITING_FILES
import im.vector.app.core.utils.TextUtils
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.createJSonViewerStyleProvider
import im.vector.app.core.utils.createUIHandler
import im.vector.app.core.utils.isValidUrl
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.saveMedia
import im.vector.app.core.utils.shareMedia
import im.vector.app.core.utils.shareText
import im.vector.app.core.utils.toast
import im.vector.app.features.attachments.AttachmentTypeSelectorView
import im.vector.app.features.attachments.AttachmentsHelper
import im.vector.app.features.attachments.ContactAttachment
import im.vector.app.features.attachments.preview.AttachmentsPreviewActivity
import im.vector.app.features.attachments.preview.AttachmentsPreviewArgs
import im.vector.app.features.attachments.toGroupedContentAttachmentData
import im.vector.app.features.call.SharedActiveCallViewModel
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.WebRtcPeerConnectionManager
import im.vector.app.features.command.Command
import im.vector.app.features.crypto.keysbackup.restore.KeysBackupRestoreActivity
import im.vector.app.features.crypto.util.toImageRes
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.composer.MicrophoneRecorderView
import im.vector.app.features.home.room.detail.composer.RecordTime
import im.vector.app.features.home.room.detail.composer.SlideToCancel
import im.vector.app.features.home.room.detail.composer.TextComposerView
import im.vector.app.features.home.room.detail.composer.util.AssertedSuccessListener
import im.vector.app.features.home.room.detail.composer.util.ListenableFuture
import im.vector.app.features.home.room.detail.composer.util.ServiceUtil
import im.vector.app.features.home.room.detail.composer.util.XlinxUtil
import im.vector.app.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.action.EventSharedAction
import im.vector.app.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.app.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageFileItem
import im.vector.app.features.home.room.detail.timeline.item.MessageImageVideoItem
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.MessageTextItem
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlRetriever
import im.vector.app.features.home.room.detail.widget.RoomWidgetsBottomSheet
import im.vector.app.features.home.xspace.conference.JitsiCallViewModel
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.html.PillImageSpan
import im.vector.app.features.html.PillsPostProcessor
import im.vector.app.features.invite.VectorInviteView
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.permalink.NavigationInterceptor
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.reactions.EmojiReactionPickerActivity
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.share.SharedData
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.widgets.WidgetActivity
import im.vector.app.features.widgets.WidgetArgs
import im.vector.app.features.widgets.WidgetKind
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.composer_layout.view.*
import kotlinx.android.synthetic.main.fragment_room_detail.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import omrecorder.AudioRecordConfig
import omrecorder.OmRecorder
import omrecorder.PullTransport
import omrecorder.PullableSource
import omrecorder.Recorder
import org.billcarsonfr.jsonviewer.JSonViewerDialog
import org.commonmark.parser.Parser
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageImageInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageStickerContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.crypto.model.event.WithHeldCode
import timber.log.Timber
import java.io.File
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_WRITE_AUDIO_PERMISSION = 300
private const val PICKER_REQUEST_CODE = 2929

@Parcelize
data class RoomDetailArgs(
        val roomId: String,
        val eventId: String? = null,
        val sharedData: SharedData? = null
) : Parcelable

class RoomDetailFragment @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val timelineEventController: TimelineEventController,
        autoCompleterFactory: AutoCompleter.Factory,
        private val permalinkHandler: PermalinkHandler,
        private val notificationDrawerManager: NotificationDrawerManager,
        val roomDetailViewModelFactory: RoomDetailViewModel.Factory,
        private val eventHtmlRenderer: EventHtmlRenderer,
        private val vectorPreferences: VectorPreferences,
        private val colorProvider: ColorProvider,
        private val notificationUtils: NotificationUtils,
        private val webRtcPeerConnectionManager: WebRtcPeerConnectionManager,
        private val matrixItemColorProvider: MatrixItemColorProvider,
        private val imageContentRenderer: ImageContentRenderer,
        private val roomDetailPendingActionStore: RoomDetailPendingActionStore,
        private val pillsPostProcessorFactory: PillsPostProcessor.Factory,
        private val dateFormatter: VectorDateFormatter
) :
        VectorBaseFragment(),
        TimelineEventController.Callback,
        VectorInviteView.Callback,
        JumpToReadMarkerView.Callback,
        AttachmentTypeSelectorView.Callback,
        AttachmentsHelper.Callback,
        CameraDialogChooserHelper.Callback,
        GalleryOrCameraDialogHelper.Listener,
        MicrophoneRecorderView.Listener,
        ActiveCallView.Callback {

    companion object {
        /**
         * Sanitize the display name.
         *
         * @param displayName the display name to sanitize
         * @return the sanitized display name
         */
        private fun sanitizeDisplayName(displayName: String): String {
            if (displayName.endsWith(ircPattern)) {
                return displayName.substring(0, displayName.length - ircPattern.length)
            }

            return displayName
        }

        private const val ircPattern = " (IRC)"
    }

    private val galleryOrCameraDialogHelper = GalleryOrCameraDialogHelper(this, colorProvider)
    private val cameraDialogChooserHelper = CameraDialogChooserHelper(this, colorProvider, this)


    private val roomDetailArgs: RoomDetailArgs by args()
    private val glideRequests by lazy {
        GlideApp.with(this)
    }
    private val pillsPostProcessor by lazy {
        pillsPostProcessorFactory.create(roomDetailArgs.roomId)
    }

    private val autoCompleter: AutoCompleter by lazy {
        autoCompleterFactory.create(roomDetailArgs.roomId)
    }
    private val roomDetailViewModel: RoomDetailViewModel by fragmentViewModel()
    private val debouncer = Debouncer(createUIHandler())

    private lateinit var scrollOnNewMessageCallback: ScrollOnNewMessageCallback
    private lateinit var scrollOnHighlightedEventCallback: ScrollOnHighlightedEventCallback

    override fun getLayoutResId() = R.layout.fragment_room_detail

    override fun getMenuRes() = R.menu.menu_timeline

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel
    private lateinit var sharedCallActionViewModel: SharedActiveCallViewModel

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var jumpToBottomViewVisibilityManager: JumpToBottomViewVisibilityManager
    private var modelBuildListener: OnModelBuildFinishedListener? = null

    private lateinit var attachmentsHelper: AttachmentsHelper
    private lateinit var keyboardStateUtils: KeyboardStateUtils

    private lateinit var attachmentTypeSelector: AttachmentTypeSelectorView

    private var lockSendButton = false
    private val activeCallViewHolder = ActiveCallViewHolder()

    private var voiceNoteCurrentPlaying: String = ""
    private var voiceNoteCurrentView: View? = null
    private lateinit var trackSelector: TrackSelector
    private lateinit var loadControl: LoadControl
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private lateinit var extractorsFactory: ExtractorsFactory
    private var exoPlayer: SimpleExoPlayer? = null


    @BindView(R.id.recorder_view)
    lateinit var microphoneRecorderView: MicrophoneRecorderView

    @BindView(R.id.quick_audio_toggle)
    lateinit var quickAudioToggle: View

    @BindView(R.id.recording_container)
    lateinit var recordingContainer: View

    @BindView(R.id.record_cancel)
    lateinit var recordLockCancel: View

    @BindView(R.id.record_time)
    lateinit var recordTimeView: TextView

    @BindView(R.id.microphone)
    lateinit var microphoneView: View

    @BindView(R.id.slide_to_cancel)
    lateinit var slideToCancelView: View

    private lateinit var recordTime: RecordTime

    private lateinit var slideToCancel: SlideToCancel

    @BindView(R.id.audioPlayerView)
    lateinit var audioPlayerContainer: View

    @BindView(R.id.mediaFileNameText)
    lateinit var audioPlayerVoiceNoteName: TextView

    @BindView(R.id.playerControlView)
    lateinit var audioPlayerController: PlayerControlView

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        sharedCallActionViewModel = activityViewModelProvider.get(SharedActiveCallViewModel::class.java)
        attachmentsHelper = AttachmentsHelper(requireContext(), this).register()
        keyboardStateUtils = KeyboardStateUtils(requireActivity())
        setupToolbar(roomToolbar)
        setupRecyclerView()
        setupComposer()
        setupInviteView()
        setupNotificationView()
        setupJumpToReadMarkerView()
        setupActiveCallView()
        setupJumpToBottomView()
        setupConfBannerView()
//        setupEmojiPopup()

        slideToCancel = SlideToCancel(slideToCancelView)
        microphoneRecorderView.setListener(this);
        recordTime = RecordTime(recordTimeView, microphoneView, TimeUnit.HOURS.toSeconds(1)) { microphoneRecorderView.cancelAction() }
        recordLockCancel.setOnClickListener { _: View? -> microphoneRecorderView.cancelAction() }


        roomToolbarContentView.debouncedClicks {
            navigator.openRoomProfile(requireActivity(), roomDetailArgs.roomId)
        }

        sharedActionViewModel
                .observe()
                .subscribe {
                    handleActions(it)
                }
                .disposeOnDestroyView()

        sharedCallActionViewModel
                .activeCall
                .observe(viewLifecycleOwner, Observer {
                    activeCallViewHolder.updateCall(it, webRtcPeerConnectionManager)
                    invalidateOptionsMenu()
                })

        roomDetailViewModel.selectSubscribe(this, RoomDetailViewState::tombstoneEventHandling, uniqueOnly("tombstoneEventHandling")) {
            renderTombstoneEventHandling(it)
        }

        roomDetailViewModel.selectSubscribe(RoomDetailViewState::sendMode, RoomDetailViewState::canSendMessage) { mode, canSend ->
            if (!canSend) {
                return@selectSubscribe
            }
            when (mode) {
                is SendMode.REGULAR -> renderRegularMode(mode.text)
                is SendMode.EDIT -> renderSpecialMode(mode.timelineEvent, R.drawable.ic_edit, R.string.edit, mode.text)
                is SendMode.QUOTE -> renderSpecialMode(mode.timelineEvent, R.drawable.ic_quote, R.string.quote, mode.text)
                is SendMode.REPLY -> renderSpecialMode(mode.timelineEvent, R.drawable.ic_reply, R.string.reply, mode.text)
            }
        }

//        roomDetailViewModel.selectSubscribe(RoomDetailViewState::syncState) { syncState ->
//            syncStateView.render(syncState)
//        }

        roomDetailViewModel.observeViewEvents {
            when (it) {
                is RoomDetailViewEvents.Failure -> showErrorInSnackbar(it.throwable)
                is RoomDetailViewEvents.OnNewTimelineEvents -> scrollOnNewMessageCallback.addNewTimelineEventIds(it.eventIds)
                is RoomDetailViewEvents.ActionSuccess -> displayRoomDetailActionSuccess(it)
                is RoomDetailViewEvents.ActionFailure -> displayRoomDetailActionFailure(it)
                is RoomDetailViewEvents.ShowMessage -> showSnackWithMessage(it.message, Snackbar.LENGTH_LONG)
                is RoomDetailViewEvents.NavigateToEvent -> navigateToEvent(it)
                is RoomDetailViewEvents.FileTooBigError -> displayFileTooBigError(it)
                is RoomDetailViewEvents.DownloadFileState -> handleDownloadFileState(it)
                is RoomDetailViewEvents.JoinRoomCommandSuccess -> handleJoinedToAnotherRoom(it)
                is RoomDetailViewEvents.SendMessageResult -> renderSendMessageResult(it)
                is RoomDetailViewEvents.ShowE2EErrorMessage -> displayE2eError(it.withHeldCode)
                RoomDetailViewEvents.DisplayPromptForIntegrationManager -> displayPromptForIntegrationManager()
                is RoomDetailViewEvents.OpenStickerPicker -> openStickerPicker(it)
                is RoomDetailViewEvents.DisplayEnableIntegrationsWarning -> displayDisabledIntegrationDialog()
                is RoomDetailViewEvents.OpenIntegrationManager -> openIntegrationManager()
                is RoomDetailViewEvents.OpenFile -> startOpenFileIntent(it)
                RoomDetailViewEvents.OpenActiveWidgetBottomSheet -> onViewWidgetsClicked()
                is RoomDetailViewEvents.ShowInfoOkDialog -> showDialogWithMessage(it.message)
                is RoomDetailViewEvents.JoinJitsiConference -> joinJitsiRoom(it.widget, it.withVideo)
                RoomDetailViewEvents.ShowWaitingView -> vectorBaseActivity.showWaitingView()
                RoomDetailViewEvents.HideWaitingView -> vectorBaseActivity.hideWaitingView()
                is RoomDetailViewEvents.RequestNativeWidgetPermission -> requestNativeWidgetPermission(it)
                is RoomDetailViewEvents.OpenRoom -> handleOpenRoom(it)
                RoomDetailViewEvents.OpenInvitePeople -> navigator.openInviteUsersToRoom(requireContext(), roomDetailArgs.roomId)
                RoomDetailViewEvents.OpenSetRoomAvatarDialog -> galleryOrCameraDialogHelper.show()
                RoomDetailViewEvents.OpenRoomSettings -> handleOpenRoomSettings()
                is RoomDetailViewEvents.ShowRoomAvatarFullScreen -> it.matrixItem?.let { item ->
                    navigator.openBigImageViewer(requireActivity(), it.view, item)
                }
                is RoomDetailViewEvents.StartChatEffect -> handleChatEffect(it.type)
                RoomDetailViewEvents.StopChatEffects -> handleStopChatEffects()
            }.exhaustive
        }

        if (savedInstanceState == null) {
            handleShareData()
        }

//        xrecorder_container.isVisible = false
//        syncStateView.render(SyncState.RestoreSlow)
//        syncStateView.isVisible = true
//        syncStateNoNetwork.isVisible = true
//        syncStateNoNetwork.text = "Restoring messages ..."

        initExoPlayer()
    }

    private fun handleChatEffect(chatEffect: ChatEffect) {
        when (chatEffect) {
            ChatEffect.CONFETTI -> {
                viewKonfetti.isVisible = true
                viewKonfetti.build()
                        .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                        .setDirection(0.0, 359.0)
                        .setSpeed(2f, 5f)
                        .setFadeOutEnabled(true)
                        .setTimeToLive(2000L)
                        .addShapes(Shape.Square, Shape.Circle)
                        .addSizes(Size(12))
                        .setPosition(-50f, viewKonfetti.width + 50f, -50f, -50f)
                        .streamFor(150, 3000L)
            }
            ChatEffect.SNOW -> {
                viewSnowFall.isVisible = true
                viewSnowFall.restartFalling()
            }
        }
    }
    private fun handleStopChatEffects() {
        TransitionManager.beginDelayedTransition(rootConstraintLayout)
        viewSnowFall.isVisible = false
        // when gone the effect is a bit buggy
        viewKonfetti.isInvisible = true
    }

    override fun onImageReady(uri: Uri?) {
        uri ?: return
        roomDetailViewModel.handle(
                RoomDetailAction.SetAvatarAction(
                        newAvatarUri = uri,
                        newAvatarFileName = getFilenameFromUri(requireContext(), uri) ?: UUID.randomUUID().toString()
                )
        )
    }

    private fun handleOpenRoomSettings() {
        navigator.openRoomProfile(
                requireContext(),
                roomDetailArgs.roomId,
                RoomProfileActivity.EXTRA_DIRECT_ACCESS_ROOM_SETTINGS
        )
    }

    private fun handleOpenRoom(openRoom: RoomDetailViewEvents.OpenRoom) {
        navigator.openRoom(requireContext(), openRoom.roomId, null)
    }

    private fun requestNativeWidgetPermission(it: RoomDetailViewEvents.RequestNativeWidgetPermission) {
        val tag = RoomWidgetPermissionBottomSheet::class.java.name
        val dFrag = childFragmentManager.findFragmentByTag(tag) as? RoomWidgetPermissionBottomSheet
        if (dFrag != null && dFrag.dialog?.isShowing == true && !dFrag.isRemoving) {
            return
        } else {
            RoomWidgetPermissionBottomSheet.newInstance(
                    WidgetArgs(
                            baseUrl = it.domain,
                            kind = WidgetKind.ROOM,
                            roomId = roomDetailArgs.roomId,
                            widgetId = it.widget.widgetId
                    )
            ).apply {
                directListener = { granted ->
                    if (granted) {
                        roomDetailViewModel.handle(RoomDetailAction.EnsureNativeWidgetAllowed(
                                widget = it.widget,
                                userJustAccepted = true,
                                grantedEvents = it.grantedEvents
                        ))
                    }
                }
            }
                    .show(childFragmentManager, tag)
        }
    }

    private val integrationManagerActivityResultLauncher = registerStartForActivityResult {
        // Noop
    }

    private fun openIntegrationManager(screen: String? = null) {
        navigator.openIntegrationManager(
                context = requireContext(),
                activityResultLauncher = integrationManagerActivityResultLauncher,
                roomId = roomDetailArgs.roomId,
                integId = null,
                screen = screen
        )
    }

    private fun setupConfBannerView() {
        activeConferenceView.callback = object : ActiveConferenceView.Callback {
            override fun onTapJoinAudio(jitsiWidget: Widget) {
                // need to check if allowed first
                roomDetailViewModel.handle(RoomDetailAction.EnsureNativeWidgetAllowed(
                        widget = jitsiWidget,
                        userJustAccepted = false,
                        grantedEvents = RoomDetailViewEvents.JoinJitsiConference(jitsiWidget, false))
                )
            }

            override fun onTapJoinVideo(jitsiWidget: Widget) {
                roomDetailViewModel.handle(RoomDetailAction.EnsureNativeWidgetAllowed(
                        widget = jitsiWidget,
                        userJustAccepted = false,
                        grantedEvents = RoomDetailViewEvents.JoinJitsiConference(jitsiWidget, true))
                )
            }

            override fun onDelete(jitsiWidget: Widget) {
                roomDetailViewModel.handle(RoomDetailAction.RemoveWidget(jitsiWidget.widgetId))
            }
        }
    }

//    private fun setupEmojiPopup() {
//        val emojiPopup = EmojiPopup
//                .Builder
//                .fromRootView(rootConstraintLayout)
//                .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
//                .setOnEmojiPopupShownListener { composerLayout?.composerEmojiButton?.setImageResource(R.drawable.ic_keyboard) }
//                .setOnEmojiPopupDismissListener { composerLayout?.composerEmojiButton?.setImageResource(R.drawable.ic_insert_emoji) }
//                .build(composerLayout.composerEditText)
//
//        composerLayout.composerEmojiButton.debouncedClicks {
//            emojiPopup.toggle()
//        }
//    }

//    private fun setupEmojiPopup() {
//        val emojiPopup = EmojiPopup
//                .Builder
//                .fromRootView(rootConstraintLayout)
//                .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
//                .setOnEmojiPopupShownListener { composerLayout?.composerEmojiButton?.setImageResource(R.drawable.ic_keyboard) }
//                .setOnEmojiPopupDismissListener { composerLayout?.composerEmojiButton?.setImageResource(R.drawable.ic_mic_24) }
//                .build(composerLayout.composerEditText)
//
//        composerLayout.composerEmojiButton.debouncedClicks {
//            emojiPopup.toggle()
//        }
//    }


    private fun joinJitsiRoom(jitsiWidget: Widget, enableVideo: Boolean) {
        navigator.openRoomWidget(requireContext(), roomDetailArgs.roomId, jitsiWidget, mapOf(JitsiCallViewModel.ENABLE_VIDEO_OPTION to enableVideo))
    }

    private fun openStickerPicker(event: RoomDetailViewEvents.OpenStickerPicker) {
        navigator.openStickerPicker(requireContext(), stickerActivityResultLauncher, roomDetailArgs.roomId, event.widget)
    }

    private fun startOpenFileIntent(action: RoomDetailViewEvents.OpenFile) {
        if (action.uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndTypeAndNormalize(action.uri, action.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                requireActivity().startActivity(intent)
            } else {
                requireActivity().toast(R.string.error_no_external_application_found)
            }
        }
    }

    private fun displayPromptForIntegrationManager() {
        // The Sticker picker widget is not installed yet. Propose the user to install it
        val builder = AlertDialog.Builder(requireContext())
        val v: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_no_sticker_pack, null)
        builder
                .setView(v)
                .setPositiveButton(R.string.yes) { _, _ ->
                    // Open integration manager, to the sticker installation page
                    openIntegrationManager(
                            screen = WidgetType.StickerPicker.preferred
                    )
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun handleJoinedToAnotherRoom(action: RoomDetailViewEvents.JoinRoomCommandSuccess) {
        updateComposerText("")
        lockSendButton = false
        navigator.openRoom(vectorBaseActivity, action.roomId)
    }

    private fun handleShareData() {
        when (val sharedData = roomDetailArgs.sharedData) {
            is SharedData.Text -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterRegularMode(sharedData.text, fromSharing = true))
            }
            is SharedData.Attachments -> {
                // open share edition
                onContentAttachmentsReady(sharedData.attachmentData)
            }
            null -> Timber.v("No share data to process")
        }.exhaustive
    }

    override fun onDestroyView() {
        timelineEventController.callback = null
        timelineEventController.removeModelBuildListener(modelBuildListener)
        activeCallView.callback = null
        modelBuildListener = null
        autoCompleter.clear()
        debouncer.cancelAll()
        timelineRecyclerView.cleanup()

        super.onDestroyView()
    }

    override fun onDestroy() {
        activeCallViewHolder.unBind(webRtcPeerConnectionManager)
        roomDetailViewModel.handle(RoomDetailAction.ExitTrackingUnreadMessagesState)
        super.onDestroy()
    }

    private fun setupJumpToBottomView() {
        jumpToBottomView.visibility = View.INVISIBLE
        jumpToBottomView.debouncedClicks {
            roomDetailViewModel.handle(RoomDetailAction.ExitTrackingUnreadMessagesState)
            jumpToBottomView.visibility = View.INVISIBLE
            if (!roomDetailViewModel.timeline.isLive) {
                scrollOnNewMessageCallback.forceScrollOnNextUpdate()
                roomDetailViewModel.timeline.restartWithEventId(null)
            } else {
                layoutManager.scrollToPosition(0)
            }
        }

        jumpToBottomViewVisibilityManager = JumpToBottomViewVisibilityManager(
                jumpToBottomView,
                debouncer,
                timelineRecyclerView,
                layoutManager
        )
    }

    private fun setupJumpToReadMarkerView() {
        jumpToReadMarkerView.callback = this
    }

    private fun setupActiveCallView() {
        activeCallViewHolder.bind(
                activeCallPiP,
                activeCallView,
                activeCallPiPWrap,
                this
        )
    }

    private fun navigateToEvent(action: RoomDetailViewEvents.NavigateToEvent) {
        val scrollPosition = timelineEventController.searchPositionOfEvent(action.eventId)
        if (scrollPosition == null) {
            scrollOnHighlightedEventCallback.scheduleScrollTo(action.eventId)
        } else {
            timelineRecyclerView.stopScroll()
            layoutManager.scrollToPosition(scrollPosition)
        }
    }

    private fun displayFileTooBigError(action: RoomDetailViewEvents.FileTooBigError) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(getString(R.string.error_file_too_big,
                        action.filename,
                        TextUtils.formatFileSize(requireContext(), action.fileSizeInBytes),
                        TextUtils.formatFileSize(requireContext(), action.homeServerLimitInBytes)
                ))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun handleDownloadFileState(action: RoomDetailViewEvents.DownloadFileState) {
        val activity = requireActivity()
        if (action.throwable != null) {
            activity.toast(errorFormatter.toHumanReadable(action.throwable))
        }
//        else if (action.file != null) {
//            addEntryToDownloadManager(activity, action.file, action.mimeType ?: "application/octet-stream")?.let {
//                // This is a temporary solution to help users find downloaded files
//                // there is a better way to do that
//                // On android Q+ this method returns the file URI, on older
//                // it returns null, and the download manager handles the notification
//                notificationUtils.buildDownloadFileNotification(
//                        it,
//                        action.file.name ?: "file",
//                        action.mimeType ?: "application/octet-stream"
//                ).let { notification ->
//                    notificationUtils.showNotificationMessage("DL", action.file.absolutePath.hashCode(), notification)
//                }
//            }
//        }
    }

    private fun setupNotificationView() {
        notificationAreaView.delegate = object : NotificationAreaView.Delegate {
            override fun onTombstoneEventClicked(tombstoneEvent: Event) {
                roomDetailViewModel.handle(RoomDetailAction.HandleTombstoneEvent(tombstoneEvent))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // We use a custom layout for this menu item, so we need to set a ClickListener
//        menu.findItem(R.id.open_matrix_apps)?.let { menuItem ->
//            menuItem.actionView.setOnClickListener {
//                onOptionsItemSelected(menuItem)
//            }
//        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.forEach {
            it.isVisible = roomDetailViewModel.isMenuItemVisible(it.itemId)
        }
        withState(roomDetailViewModel) { state ->
            // Set the visual state of the call buttons (voice/video) to enabled/disabled according to user permissions
            val callButtonsEnabled = when (state.asyncRoomSummary.invoke()?.joinedMembersCount) {
                1 -> false
                2 -> state.isAllowedToStartWebRTCCall
                else -> state.isAllowedToManageWidgets
            }
            setOf(R.id.voice_call, R.id.video_call).forEach {
                menu.findItem(it).icon?.alpha = if (callButtonsEnabled) 0xFF else 0x40
            }

//            val matrixAppsMenuItem = menu.findItem(R.id.open_matrix_apps)
//            val widgetsCount = state.activeRoomWidgets.invoke()?.size ?: 0
//            if (widgetsCount > 0) {
//                val actionView = matrixAppsMenuItem.actionView
//                actionView
//                        .findViewById<ImageView>(R.id.action_view_icon_image)
//                        .setColorFilter(ContextCompat.getColor(requireContext(), R.color.riotx_accent))
//                actionView.findViewById<TextView>(R.id.cart_badge).setTextOrHide("$widgetsCount")
//                matrixAppsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//            } else {
//                // icon should be default color no badge
//                val actionView = matrixAppsMenuItem.actionView
//                actionView
//                        .findViewById<ImageView>(R.id.action_view_icon_image)
//                        .setColorFilter(ThemeUtils.getColor(requireContext(), R.attr.riotx_text_secondary))
//                actionView.findViewById<TextView>(R.id.cart_badge).isVisible = false
//                matrixAppsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
//            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.invite -> {
                navigator.openInviteUsersToRoom(requireActivity(), roomDetailArgs.roomId)
                true
            }
            R.id.timeline_setting -> {
                navigator.openRoomProfile(requireActivity(), roomDetailArgs.roomId)
                true
            }
            R.id.resend_all -> {
                roomDetailViewModel.handle(RoomDetailAction.ResendAll)
                true
            }
//            R.id.open_matrix_apps -> {
//                roomDetailViewModel.handle(RoomDetailAction.ManageIntegrations)
//                true
//            }
            R.id.voice_call,
            R.id.video_call -> {
                handleCallRequest(item)
                true
            }
            R.id.hangup_call -> {
                roomDetailViewModel.handle(RoomDetailAction.EndCall)
                true
            }
            R.id.search -> {
                handleSearchAction()
                true
            }
            else                     -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleSearchAction() {
        if (session.getRoom(roomDetailArgs.roomId)?.isEncrypted() == false) {
            navigator.openSearch(requireContext(), roomDetailArgs.roomId)
        } else {
            showDialogWithMessage(getString(R.string.search_is_not_supported_in_e2e_room))
        }
    }

    private fun handleCallRequest(item: MenuItem) = withState(roomDetailViewModel) { state ->
        val roomSummary = state.asyncRoomSummary.invoke() ?: return@withState
        val isVideoCall = item.itemId == R.id.video_call
        when (roomSummary.joinedMembersCount) {
            1 -> {
                val pendingInvite = roomSummary.invitedMembersCount ?: 0 > 0
                if (pendingInvite) {
                    // wait for other to join
                    showDialogWithMessage(getString(R.string.cannot_call_yourself_with_invite))
                } else {
                    // You cannot place a call with yourself.
                    showDialogWithMessage(getString(R.string.cannot_call_yourself))
                }
            }
            2 -> {
                val activeCall = sharedCallActionViewModel.activeCall.value
                if (activeCall != null) {
                    // resume existing if same room, if not prompt to kill and then restart new call?
                    if (activeCall.roomId == roomDetailArgs.roomId) {
                        onTapToReturnToCall()
                    }
                    //                        else {
                    // TODO might not work well, and should prompt
                    //                            webRtcPeerConnectionManager.endCall()
                    //                            safeStartCall(it, isVideoCall)
                    //                        }
                } else if (!state.isAllowedToStartWebRTCCall) {
                    showDialogWithMessage(getString(
                            if (state.isDm()) {
                                R.string.no_permissions_to_start_webrtc_call_in_direct_room
                            } else {
                                R.string.no_permissions_to_start_webrtc_call
                            })
                    )
                } else {
                    safeStartCall(isVideoCall)
                }
            }
            else -> {
                // it's jitsi call
                // can you add widgets??
                if (!state.isAllowedToManageWidgets) {
                    // You do not have permission to start a conference call in this room
                    showDialogWithMessage(getString(
                            if (state.isDm()) {
                                R.string.no_permissions_to_start_conf_call_in_direct_room
                            } else {
                                R.string.no_permissions_to_start_conf_call
                            }
                    ))
                } else {
                    if (state.activeRoomWidgets()?.filter { it.type == WidgetType.Jitsi }?.any() == true) {
                        // A conference is already in progress!
                        showDialogWithMessage(getString(R.string.conference_call_in_progress))
                    } else {
                        AlertDialog.Builder(requireContext())
                                .setTitle(if (isVideoCall) R.string.video_meeting else R.string.audio_meeting)
                                .setMessage(R.string.audio_video_meeting_description)
                                .setPositiveButton(getString(R.string.create)) { _, _ ->
                                    // create the widget, then navigate to it..
                                    roomDetailViewModel.handle(RoomDetailAction.AddJitsiWidget(isVideoCall))
                                }
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show()
                    }
                }
            }
        }
    }

    private fun displayDisabledIntegrationDialog() {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.disabled_integration_dialog_title)
                .setMessage(R.string.disabled_integration_dialog_content)
                .setPositiveButton(R.string.settings) { _, _ ->
                    navigator.openSettings(requireActivity(), VectorSettingsActivity.EXTRA_DIRECT_ACCESS_GENERAL)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun safeStartCall(isVideoCall: Boolean) {
        if (vectorPreferences.preventAccidentalCall()) {
            AlertDialog.Builder(requireActivity())
                    .setMessage(if (isVideoCall) R.string.start_video_call_prompt_msg else R.string.start_voice_call_prompt_msg)
                    .setPositiveButton(if (isVideoCall) R.string.start_video_call else R.string.start_voice_call) { _, _ ->
                        safeStartCall2(isVideoCall)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            safeStartCall2(isVideoCall)
        }
    }

    private val startCallActivityResultLauncher = registerForPermissionsResult { allGranted ->
        if (allGranted) {
            (roomDetailViewModel.pendingAction as? RoomDetailAction.StartCall)?.let {
                roomDetailViewModel.pendingAction = null
                roomDetailViewModel.handle(it)
            }
        } else {
            context?.toast(R.string.permissions_action_not_performed_missing_permissions)
            cleanUpAfterPermissionNotGranted()
        }
    }

    private fun safeStartCall2(isVideoCall: Boolean) {
        val startCallAction = RoomDetailAction.StartCall(isVideoCall)
        roomDetailViewModel.pendingAction = startCallAction
        if (isVideoCall) {
            if (checkPermissions(PERMISSIONS_FOR_VIDEO_IP_CALL,
                            requireActivity(),
                            startCallActivityResultLauncher,
                            R.string.permissions_rationale_msg_camera_and_audio)) {
                roomDetailViewModel.pendingAction = null
                roomDetailViewModel.handle(startCallAction)
            }
        } else {
            if (checkPermissions(PERMISSIONS_FOR_AUDIO_IP_CALL,
                            requireActivity(),
                            startCallActivityResultLauncher,
                            R.string.permissions_rationale_msg_record_audio)) {
                roomDetailViewModel.pendingAction = null
                roomDetailViewModel.handle(startCallAction)
            }
        }
    }

    private fun renderRegularMode(text: String) {
        autoCompleter.exitSpecialMode()
        composerLayout.collapse()

        updateComposerText(text)
        composerLayout.sendButton.contentDescription = getString(R.string.send)
    }

    private fun renderSpecialMode(event: TimelineEvent,
                                  @DrawableRes iconRes: Int,
                                  @StringRes descriptionRes: Int,
                                  defaultContent: String) {
        autoCompleter.enterSpecialMode()
        // switch to expanded bar
        composerLayout.composerRelatedMessageTitle.apply {
            text = event.senderInfo.disambiguatedDisplayName
            setTextColor(matrixItemColorProvider.getColor(MatrixItem.UserItem(event.root.senderId ?: "@")))
        }

        val messageContent: MessageContent? = event.getLastMessageContent()
        val nonFormattedBody = messageContent?.body ?: ""
        var formattedBody: CharSequence? = null
        if (messageContent is MessageTextContent && messageContent.format == MessageFormat.FORMAT_MATRIX_HTML) {
            val parser = Parser.builder().build()
            val document = parser.parse(messageContent.formattedBody ?: messageContent.body)
            formattedBody = eventHtmlRenderer.render(document, pillsPostProcessor)
        }
        composerLayout.composerRelatedMessageContent.text = (formattedBody ?: nonFormattedBody)

        updateComposerText(defaultContent)

        composerLayout.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), iconRes))
        composerLayout.sendButton.contentDescription = getString(descriptionRes)

        avatarRenderer.render(event.senderInfo.toMatrixItem(), composerLayout.composerRelatedMessageAvatar)

        composerLayout.expand {
            if (isAdded) {
                // need to do it here also when not using quick reply
                focusComposerAndShowKeyboard()
            }
        }
        focusComposerAndShowKeyboard()
    }

    private fun updateComposerText(text: String) {
        // Do not update if this is the same text to avoid the cursor to move
        if (text != composerLayout.composerEditText.text.toString()) {
            // Ignore update to avoid saving a draft
            composerLayout.composerEditText.setText(text)
            composerLayout.composerEditText.setSelection(composerLayout.composerEditText.text?.length
                    ?: 0)
        }
    }

    override fun onResume() {
        super.onResume()
        notificationDrawerManager.setCurrentRoom(roomDetailArgs.roomId)
        roomDetailPendingActionStore.data?.let { handlePendingAction(it) }
        roomDetailPendingActionStore.data = null
    }

    private fun handlePendingAction(roomDetailPendingAction: RoomDetailPendingAction) {
        when (roomDetailPendingAction) {
            is RoomDetailPendingAction.JumpToReadReceipt ->
                roomDetailViewModel.handle(RoomDetailAction.JumpToReadReceipt(roomDetailPendingAction.userId))
            is RoomDetailPendingAction.MentionUser ->
                insertUserDisplayNameInTextEditor(roomDetailPendingAction.userId)
            is RoomDetailPendingAction.OpenOrCreateDm ->
                roomDetailViewModel.handle(RoomDetailAction.OpenOrCreateDm(roomDetailPendingAction.userId))
        }.exhaustive
    }

    override fun onPause() {
        super.onPause()

        notificationDrawerManager.setCurrentRoom(null)

        roomDetailViewModel.handle(RoomDetailAction.SaveDraft(composerLayout.composerEditText.text.toString()))
    }

    private val attachmentFileActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            attachmentsHelper.onImageResult(it.data)
        }
    }

    private val attachmentAudioActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            attachmentsHelper.onAudioResult(it.data)
        }
    }

    private val attachmentContactActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            attachmentsHelper.onContactResult(it.data)
        }
    }

    private val attachmentImageActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            attachmentsHelper.onImageResult(it.data)
        }
    }

    private val attachmentPhotoActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            attachmentsHelper.onPhotoResult()
        }
    }

    private val contentAttachmentActivityResultLauncher = registerStartForActivityResult { activityResult ->
        val data = activityResult.data ?: return@registerStartForActivityResult
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val sendData = AttachmentsPreviewActivity.getOutput(data)
            val keepOriginalSize = AttachmentsPreviewActivity.getKeepOriginalSize(data)
            roomDetailViewModel.handle(RoomDetailAction.SendMedia(sendData, !keepOriginalSize))
        }
    }

    private val emojiActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val eventId = EmojiReactionPickerActivity.getOutputEventId(activityResult.data)
            val reaction = EmojiReactionPickerActivity.getOutputReaction(activityResult.data)
            if (eventId != null && reaction != null) {
                roomDetailViewModel.handle(RoomDetailAction.SendReaction(eventId, reaction))
            }
        }
    }

    private val stickerActivityResultLauncher = registerStartForActivityResult { activityResult ->
        val data = activityResult.data ?: return@registerStartForActivityResult
        if (activityResult.resultCode == Activity.RESULT_OK) {
            WidgetActivity.getOutput(data).toModel<MessageStickerContent>()
                    ?.let { content ->
                        roomDetailViewModel.handle(RoomDetailAction.SendSticker(content))
                    }
        }
    }

// PRIVATE METHODS *****************************************************************************

    private fun setupRecyclerView() {
        timelineEventController.callback = this
        timelineEventController.timeline = roomDetailViewModel.timeline

        timelineRecyclerView.trackItemsVisibilityChange()
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        scrollOnNewMessageCallback = ScrollOnNewMessageCallback(layoutManager, timelineEventController)
        scrollOnHighlightedEventCallback = ScrollOnHighlightedEventCallback(timelineRecyclerView, layoutManager, timelineEventController)
        timelineRecyclerView.layoutManager = layoutManager
        timelineRecyclerView.itemAnimator = null
        timelineRecyclerView.setHasFixedSize(true)
        modelBuildListener = OnModelBuildFinishedListener {
            it.dispatchTo(stateRestorer)
            it.dispatchTo(scrollOnNewMessageCallback)
            it.dispatchTo(scrollOnHighlightedEventCallback)
            updateJumpToReadMarkerViewVisibility()
            jumpToBottomViewVisibilityManager.maybeShowJumpToBottomViewVisibilityWithDelay()
        }
        timelineEventController.addModelBuildListener(modelBuildListener)
        timelineRecyclerView.adapter = timelineEventController.adapter

        if (vectorPreferences.swipeToReplyIsEnabled()) {
            val quickReplyHandler = object : RoomMessageTouchHelperCallback.QuickReplayHandler {
                override fun performQuickReplyOnHolder(model: EpoxyModel<*>) {
                    (model as? AbsMessageItem)?.attributes?.informationData?.let {
                        val eventId = it.eventId
                        roomDetailViewModel.handle(RoomDetailAction.EnterReplyMode(eventId, composerLayout.composerEditText.text.toString()))
                    }
                }

                override fun canSwipeModel(model: EpoxyModel<*>): Boolean {
                    val canSendMessage = withState(roomDetailViewModel) {
                        it.canSendMessage
                    }
                    if (!canSendMessage) {
                        return false
                    }
                    return when (model) {
                        is MessageFileItem,
                        is MessageImageVideoItem,
                        is MessageTextItem -> {
                            return (model as AbsMessageItem).attributes.informationData.sendState == SendState.SYNCED
                        }
                        else               -> false
                    }
                }
            }
            val swipeCallback = RoomMessageTouchHelperCallback(requireContext(), R.drawable.ic_reply, quickReplyHandler)
            val touchHelper = ItemTouchHelper(swipeCallback)
            touchHelper.attachToRecyclerView(timelineRecyclerView)
        }
        timelineRecyclerView.addGlidePreloader(
                epoxyController = timelineEventController,
                requestManager = GlideApp.with(this),
                preloader = glidePreloader { requestManager, epoxyModel: MessageImageVideoItem, _ ->
                    imageContentRenderer.createGlideRequest(
                            epoxyModel.mediaData,
                            ImageContentRenderer.Mode.THUMBNAIL,
                            requestManager as GlideRequests
                    )
                })
    }

    private fun updateJumpToReadMarkerViewVisibility() {
        jumpToReadMarkerView?.post {
            withState(roomDetailViewModel) {
                val showJumpToUnreadBanner = when (it.unreadState) {
                    UnreadState.Unknown,
                    UnreadState.HasNoUnread -> false
                    is UnreadState.ReadMarkerNotLoaded -> true
                    is UnreadState.HasUnread -> {
                        if (it.canShowJumpToReadMarker) {
                            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                            val positionOfReadMarker = timelineEventController.getPositionOfReadMarker()
                            if (positionOfReadMarker == null) {
                                false
                            } else {
                                positionOfReadMarker > lastVisibleItem
                            }
                        } else {
                            false
                        }
                    }
                }
                jumpToReadMarkerView?.isVisible = showJumpToUnreadBanner
            }
        }
    }

    private fun setupComposer() {
        val composerEditText = composerLayout.composerEditText
        autoCompleter.setup(composerEditText)

        observerUserTyping()

        if (vectorPreferences.sendMessageWithEnter()) {
            // imeOptions="actionSend" only works with single line, so we remove multiline inputType
            composerEditText.inputType = composerEditText.inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE.inv()
            composerEditText.imeOptions = EditorInfo.IME_ACTION_SEND
        }

        composerEditText.setOnEditorActionListener { v, actionId, keyEvent ->
            val imeActionId = actionId and EditorInfo.IME_MASK_ACTION
            if (EditorInfo.IME_ACTION_DONE == imeActionId || EditorInfo.IME_ACTION_SEND == imeActionId) {
                sendTextMessage(v.text)
                true
            }
            // Add external keyboard functionality (to send messages)
            else if (null != keyEvent
                    && !keyEvent.isShiftPressed
                    && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER
                    && resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS) {
                sendTextMessage(v.text)
                true
            } else false
        }

        composerLayout.callback = object : TextComposerView.Callback {
            override fun onAddAttachment() {
                if (!::attachmentTypeSelector.isInitialized) {
                    attachmentTypeSelector = AttachmentTypeSelectorView(vectorBaseActivity, vectorBaseActivity.layoutInflater, this@RoomDetailFragment)
                }
                attachmentTypeSelector.show(composerLayout.attachmentButton, keyboardStateUtils.isKeyboardShowing)
            }

            override fun onSendMessage(text: CharSequence) {
                sendTextMessage(text)
            }

            override fun onCloseRelatedMessage() {
                roomDetailViewModel.handle(RoomDetailAction.EnterRegularMode(composerLayout.text.toString(), false))
            }

            override fun onRichContentSelected(contentUri: Uri): Boolean {
                return sendUri(contentUri)
            }
        }
    }

    private fun sendTextMessage(text: CharSequence) {
        if (lockSendButton) {
            Timber.w("Send button is locked")
            return
        }
        if (text.isNotBlank()) {
            // We collapse ASAP, if not there will be a slight anoying delay
            composerLayout.collapse(true)
            lockSendButton = true
            roomDetailViewModel.handle(RoomDetailAction.SendMessage(text, vectorPreferences.isMarkdownEnabled()))
        }
    }

    private fun observerUserTyping() {
        composerLayout.composerEditText.textChanges()
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .map { it.isNotEmpty() }
                .subscribe {
                    Timber.d("Typing: User is typing: $it")
                    roomDetailViewModel.handle(RoomDetailAction.UserIsTyping(it))
                }
                .disposeOnDestroyView()
    }

    private fun sendUri(uri: Uri): Boolean {
        val shareIntent = Intent(Intent.ACTION_SEND, uri)
        val isHandled = attachmentsHelper.handleShareIntent(requireContext(), shareIntent)
        if (!isHandled) {
            Toast.makeText(requireContext(), R.string.error_handling_incoming_share, Toast.LENGTH_SHORT).show()
        }
        return isHandled
    }

    private fun setupInviteView() {
        inviteView.callback = this
    }

    override fun invalidate() = withState(roomDetailViewModel) { state ->
        invalidateOptionsMenu()
        val summary = state.asyncRoomSummary()
        renderToolbar(summary, state.typingMessage)
        activeConferenceView.render(state)
        val inviter = state.asyncInviter()
        if (summary?.membership == Membership.JOIN) {
            jumpToBottomView.count = summary.notificationCount
            jumpToBottomView.drawBadge = summary.hasUnreadMessages
            scrollOnHighlightedEventCallback.timeline = roomDetailViewModel.timeline
            timelineEventController.update(state)
            inviteView.visibility = View.GONE
            if (state.tombstoneEvent == null) {
                if (state.canSendMessage) {
                    composerLayout.visibility = View.VISIBLE
                    composerLayout.setRoomEncrypted(summary.isEncrypted, summary.roomEncryptionTrustLevel)
                    notificationAreaView.render(NotificationAreaView.State.Hidden)
                } else {
                    composerLayout.visibility = View.GONE
                    notificationAreaView.render(NotificationAreaView.State.NoPermissionToPost)
                }
            } else {
                composerLayout.visibility = View.GONE
                notificationAreaView.render(NotificationAreaView.State.Tombstone(state.tombstoneEvent))
            }
        } else if (summary?.membership == Membership.INVITE && inviter != null) {
            inviteView.visibility = View.VISIBLE
            inviteView.render(inviter, VectorInviteView.Mode.LARGE, state.changeMembershipState)
            // Intercept click event
            inviteView.setOnClickListener { }
        } else if (state.asyncInviter.complete) {
            vectorBaseActivity.finish()
        }
    }

    private fun renderToolbar(roomSummary: RoomSummary?, typingMessage: String?) {
        if (roomSummary == null) {
            roomToolbarContentView.isClickable = false
        } else {
            roomToolbarContentView.isClickable = roomSummary.membership == Membership.JOIN
            roomToolbarTitleView.text = roomSummary.displayName
            avatarRenderer.render(roomSummary.toMatrixItem(), roomToolbarAvatarImageView)

            renderSubTitle(typingMessage, roomSummary.topic)
            roomToolbarDecorationImageView.let {
                it.setImageResource(roomSummary.roomEncryptionTrustLevel.toImageRes())
                it.isVisible = roomSummary.roomEncryptionTrustLevel != null
            }
        }
    }

    private fun renderSubTitle(typingMessage: String?, topic: String) {
        // TODO Temporary place to put typing data
        val subtitle = typingMessage?.takeIf { it.isNotBlank() } ?: topic
        roomToolbarSubtitleView.apply {
            setTextOrHide(subtitle)
            if (typingMessage.isNullOrBlank()) {
                setTextColor(ThemeUtils.getColor(requireContext(), R.attr.vctr_toolbar_secondary_text_color))
                setTypeface(null, Typeface.NORMAL)
            } else {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.riotx_accent))
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun renderTombstoneEventHandling(async: Async<String>) {
        when (async) {
            is Loading -> {
                // TODO Better handling progress
                vectorBaseActivity.showWaitingView()
                vectorBaseActivity.waiting_view_status_text.visibility = View.VISIBLE
                vectorBaseActivity.waiting_view_status_text.text = getString(R.string.joining_room)
            }
            is Success -> {
                navigator.openRoom(vectorBaseActivity, async())
                vectorBaseActivity.finish()
            }
            is Fail -> {
                vectorBaseActivity.hideWaitingView()
                vectorBaseActivity.toast(errorFormatter.toHumanReadable(async.error))
            }
        }
    }

    private fun renderSendMessageResult(sendMessageResult: RoomDetailViewEvents.SendMessageResult) {
        when (sendMessageResult) {
            is RoomDetailViewEvents.SlashCommandHandled -> {
                sendMessageResult.messageRes?.let { showSnackWithMessage(getString(it)) }
            }
            is RoomDetailViewEvents.SlashCommandError -> {
                displayCommandError(getString(R.string.command_problem_with_parameters, sendMessageResult.command.command))
            }
            is RoomDetailViewEvents.SlashCommandUnknown -> {
                displayCommandError(getString(R.string.unrecognized_command, sendMessageResult.command))
            }
            is RoomDetailViewEvents.SlashCommandResultOk -> {
                updateComposerText("")
            }
            is RoomDetailViewEvents.SlashCommandResultError -> {
                displayCommandError(errorFormatter.toHumanReadable(sendMessageResult.throwable))
            }
            is RoomDetailViewEvents.SlashCommandNotImplemented -> {
                displayCommandError(getString(R.string.not_implemented))
            }
        } // .exhaustive

        lockSendButton = false
    }

    private fun displayCommandError(message: String) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.command_error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun displayE2eError(withHeldCode: WithHeldCode?) {
        val msgId = when (withHeldCode) {
            WithHeldCode.BLACKLISTED -> R.string.crypto_error_withheld_blacklisted
            WithHeldCode.UNVERIFIED -> R.string.crypto_error_withheld_unverified
            WithHeldCode.UNAUTHORISED,
            WithHeldCode.UNAVAILABLE -> R.string.crypto_error_withheld_generic
            else                     -> R.string.notice_crypto_unable_to_decrypt_friendly_desc
        }
        AlertDialog.Builder(requireActivity())
                .setMessage(msgId)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun promptReasonToReportContent(action: EventSharedAction.ReportContentCustom) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_report_content, null)

        val input = layout.findViewById<TextInputEditText>(R.id.dialog_report_content_input)

        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.report_content_custom_title)
                .setView(layout)
                .setPositiveButton(R.string.report_content_custom_submit) { _, _ ->
                    val reason = input.text.toString()
                    roomDetailViewModel.handle(RoomDetailAction.ReportContent(action.eventId, action.senderId, reason))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun promptConfirmationToRedactEvent(action: EventSharedAction.Redact) {
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = action.askForReason,
                        confirmationRes = R.string.delete_event_dialog_content,
                        positiveRes = R.string.remove,
                        reasonHintRes = R.string.delete_event_dialog_reason_hint,
                        titleRes = R.string.delete_event_dialog_title
                ) { reason ->
                    roomDetailViewModel.handle(RoomDetailAction.RedactAction(action.eventId, reason))
                }
    }

    private fun displayRoomDetailActionFailure(result: RoomDetailViewEvents.ActionFailure) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(result.throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun displayRoomDetailActionSuccess(result: RoomDetailViewEvents.ActionSuccess) {
        when (val data = result.action) {
            is RoomDetailAction.ReportContent -> {
                when {
                    data.spam          -> {
                        AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.content_reported_as_spam_title)
                                .setMessage(R.string.content_reported_as_spam_content)
                                .setPositiveButton(R.string.ok, null)
                                .setNegativeButton(R.string.block_user) { _, _ ->
                                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(data.senderId))
                                }
                                .show()
                                .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                    }
                    data.inappropriate -> {
                        AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.content_reported_as_inappropriate_title)
                                .setMessage(R.string.content_reported_as_inappropriate_content)
                                .setPositiveButton(R.string.ok, null)
                                .setNegativeButton(R.string.block_user) { _, _ ->
                                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(data.senderId))
                                }
                                .show()
                                .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                    }
                    else               -> {
                        AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.content_reported_title)
                                .setMessage(R.string.content_reported_content)
                                .setPositiveButton(R.string.ok, null)
                                .setNegativeButton(R.string.block_user) { _, _ ->
                                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(data.senderId))
                                }
                                .show()
                                .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                    }
                }
            }
            is RoomDetailAction.RequestVerification -> {
                Timber.v("## SAS RequestVerification action")
                VerificationBottomSheet.withArgs(
                        roomDetailArgs.roomId,
                        data.userId
                ).show(parentFragmentManager, "REQ")
            }
            is RoomDetailAction.AcceptVerificationRequest -> {
                Timber.v("## SAS AcceptVerificationRequest action")
                VerificationBottomSheet.withArgs(
                        roomDetailArgs.roomId,
                        data.otherUserId,
                        data.transactionId
                ).show(parentFragmentManager, "REQ")
            }
            is RoomDetailAction.ResumeVerification -> {
                val otherUserId = data.otherUserId ?: return
                VerificationBottomSheet().apply {
                    arguments = Bundle().apply {
                        putParcelable(MvRx.KEY_ARG, VerificationBottomSheet.VerificationArgs(
                                otherUserId, data.transactionId, roomId = roomDetailArgs.roomId))
                    }
                }.show(parentFragmentManager, "REQ")
            }
        }
    }

// TimelineEventController.Callback ************************************************************

    override fun onUrlClicked(url: String, title: String): Boolean {
        permalinkHandler
                .launch(requireActivity(), url, object : NavigationInterceptor {
                    override fun navToRoom(roomId: String?, eventId: String?): Boolean {
                        // Same room?
                        if (roomId == roomDetailArgs.roomId) {
                            // Navigation to same room
                            if (eventId == null) {
                                showSnackWithMessage(getString(R.string.navigate_to_room_when_already_in_the_room))
                            } else {
                                // Highlight and scroll to this event
                                roomDetailViewModel.handle(RoomDetailAction.NavigateToEvent(eventId, true))
                            }
                            return true
                        }
                        // Not handled
                        return false
                    }

                    override fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
                        openRoomMemberProfile(userId)
                        return true
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { managed ->
                    if (!managed) {
                        if (title.isValidUrl() && url.isValidUrl() && URL(title).host != URL(url).host) {
                            AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.external_link_confirmation_title)
                                    .setMessage(
                                            getString(R.string.external_link_confirmation_message, title, url)
                                                    .toSpannable()
                                                    .colorizeMatchingText(url, colorProvider.getColorFromAttribute(R.attr.riotx_text_primary_body_contrast))
                                                    .colorizeMatchingText(title, colorProvider.getColorFromAttribute(R.attr.riotx_text_primary_body_contrast))
                                    )
                                    .setPositiveButton(R.string._continue) { _, _ ->
                                        openUrlInExternalBrowser(requireContext(), url)
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                                    .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                        } else {
                            // Open in external browser, in a new Tab
                            openUrlInExternalBrowser(requireContext(), url)
                        }
                    }
                }
                .disposeOnDestroyView()
        // In fact it is always managed
        return true
    }

    override fun onUrlLongClicked(url: String): Boolean {
        if (url != getString(R.string.edited_suffix) && url.isValidUrl()) {
            // Copy the url to the clipboard
            copyToClipboard(requireContext(), url, true, R.string.link_copied_to_clipboard)
        }
        return true
    }

    override fun onEventVisible(event: TimelineEvent) {
        roomDetailViewModel.handle(RoomDetailAction.TimelineEventTurnsVisible(event))
    }

    override fun onEventInvisible(event: TimelineEvent) {
        roomDetailViewModel.handle(RoomDetailAction.TimelineEventTurnsInvisible(event))
    }

    override fun onEncryptedMessageClicked(informationData: MessageInformationData, view: View) {
        vectorBaseActivity.notImplemented("encrypted message click")
    }

    override fun onImageMessageClicked(messageImageContent: MessageImageInfoContent, mediaData: ImageContentRenderer.Data, view: View) {
        navigator.openMediaViewer(
                activity = requireActivity(),
                roomId = roomDetailArgs.roomId,
                mediaData = mediaData,
                view = view
        ) { pairs ->
            pairs.add(Pair(roomToolbar, ViewCompat.getTransitionName(roomToolbar) ?: ""))
            pairs.add(Pair(composerLayout, ViewCompat.getTransitionName(composerLayout) ?: ""))
        }
    }

    override fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View) {
        navigator.openMediaViewer(
                activity = requireActivity(),
                roomId = roomDetailArgs.roomId,
                mediaData = mediaData,
                view = view
        ) { pairs ->
            pairs.add(Pair(roomToolbar, ViewCompat.getTransitionName(roomToolbar) ?: ""))
            pairs.add(Pair(composerLayout, ViewCompat.getTransitionName(composerLayout) ?: ""))
        }
    }


//    @Suppress("DEPRECATED_IDENTITY_EQUALS")
//    @Throws(IOException::class)
//    fun getFinalURL(url: String?): String? {
//        val SDK_INT = Build.VERSION.SDK_INT
//        val redirectUrl: String?
//        if (SDK_INT > 8) {
//            val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder()
//                    .permitAll().build()
//            StrictMode.setThreadPolicy(policy)
//            //your codes here
//            val con: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
//            con.instanceFollowRedirects = false
//            con.connect()
//            con.inputStream
//            if (con.responseCode === HttpURLConnection.HTTP_MOVED_PERM || con.responseCode === HttpURLConnection.HTTP_MOVED_TEMP) {
//                redirectUrl = con.getHeaderField("Location")
//                return getFinalURL(redirectUrl)
//            }
//            return url
//        }
//        return url
//    }

    private fun initExoPlayer() {
        trackSelector = DefaultTrackSelector(requireContext())
        loadControl = DefaultLoadControl()
        dataSourceFactory = DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), "xlinx"), null)
        extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)

        exoPlayer = ExoPlayerFactory.newSimpleInstance(requireContext(), trackSelector, loadControl)
        audioPlayerController.player = exoPlayer
        audioPlayerController.setShowFastForwardButton(true)
        audioPlayerController.setShowRewindButton(true)
        audioPlayerController.setShowNextButton(false)
        audioPlayerController.setShowPreviousButton(false)
        audioPlayerController.showTimeoutMs = 0
    }

    val mHandler = Handler()

    @SuppressLint("LogNotTimber", "CutPasteId")
    override fun onAudioMessageClicked(messageAudioContent: MessageAudioContent, view: View) {
        val iconView                = view.findViewById<ImageView>(R.id.messageFileIconView)
        val fileNameView            = view.findViewById<TextView>(R.id.messageFilenameView)

        if (voiceNoteCurrentPlaying == messageAudioContent.body && exoPlayer!!.isPlaying) {
            iconView.setImageResource(R.drawable.exo_icon_play)
            audioPlayerContainer.isVisible = false
            exoPlayer!!.stop()
            return
        }

        if (voiceNoteCurrentPlaying !== messageAudioContent.body) {
            val lastIconView = voiceNoteCurrentView?.findViewById<ImageView>(R.id.messageFileIconView)
            lastIconView?.setImageResource(R.drawable.exo_icon_play)
        }

        exoPlayer!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_ENDED     -> {
                        iconView.setImageResource(R.drawable.exo_icon_play)
                        audioPlayerContainer.isVisible = false
                    }
                    Player.STATE_IDLE      -> {
                    }
                    Player.STATE_READY     -> {
                    }
                    else                   -> {
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {}
        })


        iconView.setImageResource(R.drawable.exo_icon_stop)
        audioPlayerVoiceNoteName.text = fileNameView.text
        audioPlayerContainer.isVisible = true

        session.fileService().downloadFile(
                messageContent = messageAudioContent,
                callback = object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        if (isAdded) {
                            val progressiveMediaSource: ProgressiveMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(Uri.fromFile(data))
                            voiceNoteCurrentPlaying = messageAudioContent.body
                            voiceNoteCurrentView = view
                            exoPlayer!!.prepare(progressiveMediaSource)
                            exoPlayer!!.playWhenReady = true
                        }
                    }
                }
        )
    }

//    override fun onFileMessageClicked(eventId: String, messageFileContent: MessageFileContent) {
//        val isEncrypted = messageFileContent.encryptedFileInfo != null
//        val action = RoomDetailAction.DownloadOrOpen(eventId, messageFileContent, isEncrypted)
//        // We need WRITE_EXTERNAL permission
// //        if (!isEncrypted || checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, PERMISSION_REQUEST_CODE_DOWNLOAD_FILE)) {
//            showSnackWithMessage(getString(R.string.downloading_file, messageFileContent.getFileName()))
//            roomDetailViewModel.handle(action)
// //        } else {
// //            roomDetailViewModel.pendingAction = action
// //        }
//    }

    private fun cleanUpAfterPermissionNotGranted() {
        // Reset all pending data
        roomDetailViewModel.pendingAction = null
        attachmentsHelper.pendingType = null
    }

//    override fun onAudioMessageClicked(messageAudioContent: MessageAudioContent) {
//        vectorBaseActivity.notImplemented("open audio file")
//    }

    override fun onLoadMore(direction: Timeline.Direction) {
        roomDetailViewModel.handle(RoomDetailAction.LoadMoreTimelineEvents(direction))
    }

    override fun onEventCellClicked(informationData: MessageInformationData, messageContent: Any?, view: View) {
        when (messageContent) {
            is MessageVerificationRequestContent -> {
                roomDetailViewModel.handle(RoomDetailAction.ResumeVerification(informationData.eventId, null))
            }
            is MessageWithAttachmentContent -> {
                val action = RoomDetailAction.DownloadOrOpen(informationData.eventId, informationData.senderId, messageContent)
                roomDetailViewModel.handle(action)
            }
            is EncryptedEventContent -> {
                roomDetailViewModel.handle(RoomDetailAction.TapOnFailedToDecrypt(informationData.eventId))
            }
        }
    }

    override fun onEventLongClicked(informationData: MessageInformationData, messageContent: Any?, view: View): Boolean {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val roomId = roomDetailArgs.roomId

        this.view?.hideKeyboard()

        MessageActionsBottomSheet
                .newInstance(roomId, informationData)
                .show(requireActivity().supportFragmentManager, "MESSAGE_CONTEXTUAL_ACTIONS")
        return true
    }

    override fun onAvatarClicked(informationData: MessageInformationData) {
        // roomDetailViewModel.handle(RoomDetailAction.RequestVerification(informationData.userId))
        openRoomMemberProfile(informationData.senderId)
    }

    private fun openRoomMemberProfile(userId: String) {
        navigator.openRoomMemberProfile(userId = userId, roomId = roomDetailArgs.roomId, context = requireActivity())
    }

    override fun onMemberNameClicked(informationData: MessageInformationData) {
        insertUserDisplayNameInTextEditor(informationData.senderId)
    }

    override fun onClickOnReactionPill(informationData: MessageInformationData, reaction: String, on: Boolean) {
        if (on) {
            // we should test the current real state of reaction on this event
            roomDetailViewModel.handle(RoomDetailAction.SendReaction(informationData.eventId, reaction))
        } else {
            // I need to redact a reaction
            roomDetailViewModel.handle(RoomDetailAction.UndoReaction(informationData.eventId, reaction))
        }
    }

    override fun onLongClickOnReactionPill(informationData: MessageInformationData, reaction: String) {
        ViewReactionsBottomSheet.newInstance(roomDetailArgs.roomId, informationData)
                .show(requireActivity().supportFragmentManager, "DISPLAY_REACTIONS")
    }

    override fun onEditedDecorationClicked(informationData: MessageInformationData) {
        ViewEditHistoryBottomSheet.newInstance(roomDetailArgs.roomId, informationData)
                .show(requireActivity().supportFragmentManager, "DISPLAY_EDITS")
    }

    override fun onTimelineItemAction(itemAction: RoomDetailAction) {
        roomDetailViewModel.handle(itemAction)
    }

    override fun getPreviewUrlRetriever(): PreviewUrlRetriever {
        return roomDetailViewModel.previewUrlRetriever
    }

    override fun onRoomCreateLinkClicked(url: String) {
        permalinkHandler
                .launch(requireContext(), url, object : NavigationInterceptor {
                    override fun navToRoom(roomId: String?, eventId: String?): Boolean {
                        requireActivity().finish()
                        return false
                    }
                })
                .subscribe()
                .disposeOnDestroyView()
    }

    override fun onReadReceiptsClicked(readReceipts: List<ReadReceiptData>) {
        DisplayReadReceiptsBottomSheet.newInstance(readReceipts)
                .show(requireActivity().supportFragmentManager, "DISPLAY_READ_RECEIPTS")
    }

    override fun onReadMarkerVisible() {
        updateJumpToReadMarkerViewVisibility()
        roomDetailViewModel.handle(RoomDetailAction.EnterTrackingUnreadMessagesState)
    }

    override fun onPreviewUrlClicked(url: String) {
        onUrlClicked(url, url)
    }

    override fun onPreviewUrlCloseClicked(eventId: String, url: String) {
        roomDetailViewModel.handle(RoomDetailAction.DoNotShowPreviewUrlFor(eventId, url))
    }

    private fun onShareActionClicked(action: EventSharedAction.Share) {
        if (action.messageContent is MessageTextContent) {
            shareText(requireContext(), action.messageContent.body)
        } else if (action.messageContent is MessageWithAttachmentContent) {
            session.fileService().downloadFile(
                    messageContent = action.messageContent,
                    callback = object : MatrixCallback<File> {
                        override fun onSuccess(data: File) {
                            if (isAdded) {
                                shareMedia(requireContext(), data, getMimeTypeFromUri(requireContext(), data.toUri()))
                            }
                        }
                    }
            )
        }
    }

    private val saveActionActivityResultLauncher = registerForPermissionsResult { allGranted ->
        if (allGranted) {
            sharedActionViewModel.pendingAction?.let {
                handleActions(it)
                sharedActionViewModel.pendingAction = null
            }
        } else {
            cleanUpAfterPermissionNotGranted()
        }
    }

    private fun onSaveActionClicked(action: EventSharedAction.Save) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && !checkPermissions(PERMISSIONS_FOR_WRITING_FILES, requireActivity(), saveActionActivityResultLauncher)) {
            sharedActionViewModel.pendingAction = action
            return
        }
        session.fileService().downloadFile(
                messageContent = action.messageContent,
                callback = object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        if (isAdded) {
                            saveMedia(
                                    context = requireContext(),
                                    file = data,
                                    title = action.messageContent.body,
                                    mediaMimeType = action.messageContent.mimeType ?: getMimeTypeFromUri(requireContext(), data.toUri()),
                                    notificationUtils = notificationUtils
                            )
                        }
                    }
                }
        )
    }

    private fun handleActions(action: EventSharedAction) {
        when (action) {
            is EventSharedAction.OpenUserProfile -> {
                openRoomMemberProfile(action.userId)
            }
            is EventSharedAction.AddReaction -> {
                emojiActivityResultLauncher.launch(EmojiReactionPickerActivity.intent(requireContext(), action.eventId))
            }
            is EventSharedAction.ViewReactions -> {
                ViewReactionsBottomSheet.newInstance(roomDetailArgs.roomId, action.messageInformationData)
                        .show(requireActivity().supportFragmentManager, "DISPLAY_REACTIONS")
            }
            is EventSharedAction.Copy -> {
                // I need info about the current selected message :/
                copyToClipboard(requireContext(), action.content, false)
                showSnackWithMessage(getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT)
            }
            is EventSharedAction.Redact -> {
                promptConfirmationToRedactEvent(action)
            }
            is EventSharedAction.Share -> {
                onShareActionClicked(action)
            }
            is EventSharedAction.Save -> {
                onSaveActionClicked(action)
            }
            is EventSharedAction.ViewEditHistory -> {
                onEditedDecorationClicked(action.messageInformationData)
            }
            is EventSharedAction.ViewSource -> {
                JSonViewerDialog.newInstance(
                        action.content,
                        -1,
                        createJSonViewerStyleProvider(colorProvider)
                ).show(childFragmentManager, "JSON_VIEWER")
            }
            is EventSharedAction.ViewDecryptedSource -> {
                JSonViewerDialog.newInstance(
                        action.content,
                        -1,
                        createJSonViewerStyleProvider(colorProvider)
                ).show(childFragmentManager, "JSON_VIEWER")
            }
            is EventSharedAction.QuickReact -> {
                // eventId,ClickedOn,Add
                roomDetailViewModel.handle(RoomDetailAction.UpdateQuickReactAction(action.eventId, action.clickedOn, action.add))
            }
            is EventSharedAction.Edit -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterEditMode(action.eventId, composerLayout.text.toString()))
            }
            is EventSharedAction.Quote -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterQuoteMode(action.eventId, composerLayout.text.toString()))
            }
            is EventSharedAction.Reply -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterReplyMode(action.eventId, composerLayout.text.toString()))
            }
            is EventSharedAction.CopyPermalink -> {
                val permalink = session.permalinkService().createPermalink(roomDetailArgs.roomId, action.eventId)
                copyToClipboard(requireContext(), permalink, false)
                showSnackWithMessage(getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT)
            }
            is EventSharedAction.Resend -> {
                roomDetailViewModel.handle(RoomDetailAction.ResendMessage(action.eventId))
            }
            is EventSharedAction.Remove -> {
                roomDetailViewModel.handle(RoomDetailAction.RemoveFailedEcho(action.eventId))
            }
            is EventSharedAction.Cancel -> {
                roomDetailViewModel.handle(RoomDetailAction.CancelSend(action.eventId))
            }
            is EventSharedAction.ReportContentSpam -> {
                roomDetailViewModel.handle(RoomDetailAction.ReportContent(
                        action.eventId, action.senderId, "This message is spam", spam = true))
            }
            is EventSharedAction.ReportContentInappropriate -> {
                roomDetailViewModel.handle(RoomDetailAction.ReportContent(
                        action.eventId, action.senderId, "This message is inappropriate", inappropriate = true))
            }
            is EventSharedAction.ReportContentCustom -> {
                promptReasonToReportContent(action)
            }
            is EventSharedAction.IgnoreUser -> {
                action.senderId?.let { askConfirmationToIgnoreUser(it) }
            }
            is EventSharedAction.OnUrlClicked -> {
                onUrlClicked(action.url, action.title)
            }
            is EventSharedAction.OnUrlLongClicked -> {
                onUrlLongClicked(action.url)
            }
            is EventSharedAction.ReRequestKey -> {
                roomDetailViewModel.handle(RoomDetailAction.ReRequestKeys(action.eventId))
            }
            is EventSharedAction.UseKeyBackup -> {
                context?.let {
                    startActivity(KeysBackupRestoreActivity.intent(it))
                }
            }
        }
    }

    private fun askConfirmationToIgnoreUser(senderId: String) {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.room_participants_action_ignore_title)
                .setMessage(R.string.room_participants_action_ignore_prompt_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.room_participants_action_ignore) { _, _ ->
                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(senderId))
                }
                .show()
                .withColoredButton(DialogInterface.BUTTON_POSITIVE)
    }

    /**
     * Insert a user displayName in the message editor.
     *
     * @param userId the userId.
     */
    @SuppressLint("SetTextI18n")
    private fun insertUserDisplayNameInTextEditor(userId: String) {
        val startToCompose = composerLayout.composerEditText.text.isNullOrBlank()

        if (startToCompose
                && userId == session.myUserId) {
            // Empty composer, current user: start an emote
            composerLayout.composerEditText.setText(Command.EMOTE.command + " ")
            composerLayout.composerEditText.setSelection(Command.EMOTE.length)
        } else {
            val roomMember = roomDetailViewModel.getMember(userId)
            // TODO move logic outside of fragment
            (roomMember?.displayName ?: userId)
                    .let { sanitizeDisplayName(it) }
                    .let { displayName ->
                        buildSpannedString {
                            append(displayName)
                            setSpan(
                                    PillImageSpan(
                                            glideRequests,
                                            avatarRenderer,
                                            requireContext(),
                                            MatrixItem.UserItem(userId, displayName, roomMember?.avatarUrl)
                                    )
                                            .also { it.bind(composerLayout.composerEditText) },
                                    0,
                                    displayName.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            append(if (startToCompose) ": " else " ")
                        }.let { pill ->
                            if (startToCompose) {
                                if (displayName.startsWith("/")) {
                                    // Ensure displayName will not be interpreted as a Slash command
                                    composerLayout.composerEditText.append("\\")
                                }
                                composerLayout.composerEditText.append(pill)
                            } else {
                                composerLayout.composerEditText.text?.insert(composerLayout.composerEditText.selectionStart, pill)
                            }
                        }
                    }
        }
        focusComposerAndShowKeyboard()
    }

    private fun focusComposerAndShowKeyboard() {
        if (composerLayout.isVisible) {
            composerLayout.composerEditText.showKeyboard(andRequestFocus = true)
        }
    }

    private fun showSnackWithMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(requireView(), message, duration).show()
    }

    private fun showDialogWithMessage(message: String) {
        AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), null)
                .show()
    }

// VectorInviteView.Callback

    override fun onAcceptInvite() {
        notificationDrawerManager.clearMemberShipNotificationForRoom(roomDetailArgs.roomId)
        roomDetailViewModel.handle(RoomDetailAction.AcceptInvite)
    }

    override fun onRejectInvite() {
        notificationDrawerManager.clearMemberShipNotificationForRoom(roomDetailArgs.roomId)
        roomDetailViewModel.handle(RoomDetailAction.RejectInvite)
    }

// JumpToReadMarkerView.Callback

    override fun onJumpToReadMarkerClicked() = withState(roomDetailViewModel) {
        jumpToReadMarkerView.isVisible = false
        if (it.unreadState is UnreadState.HasUnread) {
            roomDetailViewModel.handle(RoomDetailAction.NavigateToEvent(it.unreadState.firstUnreadEventId, false))
        }
        if (it.unreadState is UnreadState.ReadMarkerNotLoaded) {
            roomDetailViewModel.handle(RoomDetailAction.NavigateToEvent(it.unreadState.readMarkerId, false))
        }
    }

    override fun onClearReadMarkerClicked() {
        roomDetailViewModel.handle(RoomDetailAction.MarkAllAsRead)
    }

// AttachmentTypeSelectorView.Callback

    private val typeSelectedActivityResultLauncher = registerForPermissionsResult { allGranted ->
        if (allGranted) {
            val pendingType = attachmentsHelper.pendingType
            if (pendingType != null) {
                attachmentsHelper.pendingType = null
                launchAttachmentProcess(pendingType)
            }
        } else {
            cleanUpAfterPermissionNotGranted()
        }
    }

    override fun onTypeSelected(type: AttachmentTypeSelectorView.Type) {
        if (checkPermissions(type.permissionsBit, requireActivity(), typeSelectedActivityResultLauncher)) {
            launchAttachmentProcess(type)
        } else {
            attachmentsHelper.pendingType = type
        }
    }

    @Suppress("DEPRECATION")
    private fun launchAttachmentProcess(type: AttachmentTypeSelectorView.Type) {
        when (type) {
//            AttachmentTypeSelectorView.Type.CAMERA -> attachmentsHelper.openCamera(requireContext(), attachmentPhotoActivityResultLauncher)
            AttachmentTypeSelectorView.Type.CAMERA -> cameraDialogChooserHelper.show()
            AttachmentTypeSelectorView.Type.FILE -> attachmentsHelper.selectFile(attachmentFileActivityResultLauncher)
            AttachmentTypeSelectorView.Type.GALLERY -> attachmentsHelper.selectGallery(attachmentImageActivityResultLauncher)
//            AttachmentTypeSelectorView.Type.GALLERY -> {
//                val intent = Intent(requireContext(), Gallery::class.java)
//                intent.putExtra("title", R.string.attachment_type_gallery)
//                // Mode 1 for both images and videos selection, 2 for images only and 3 for videos!
//                intent.putExtra("mode", 1)
//                intent.putExtra("maxSelection", 5) // Optional
//                startActivityForResult(intent, PICKER_REQUEST_CODE)
//            }
            AttachmentTypeSelectorView.Type.AUDIO -> attachmentsHelper.selectAudio(attachmentAudioActivityResultLauncher)
            AttachmentTypeSelectorView.Type.CONTACT -> attachmentsHelper.selectContact(attachmentContactActivityResultLauncher)
            AttachmentTypeSelectorView.Type.STICKER -> roomDetailViewModel.handle(RoomDetailAction.SelectStickerAttachment)
        }.exhaustive
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        if (requestCode == PICKER_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK && data != null) {
                val uriList = mutableListOf<Uri>()
                val selectionResult = data.getStringArrayListExtra("result")
                try {
                    selectionResult?.forEach {
                        uriList.add(Uri.fromFile(File(it)))
                    }
                } finally {
                    attachmentsHelper.onPickerImageResult(uriList)
                }

            }
        }
    }


// AttachmentsHelper.Callback

    override fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>) {
        val grouped = attachments.toGroupedContentAttachmentData()
        if (grouped.notPreviewables.isNotEmpty()) {
            // Send the not previewable attachments right now (?)
            roomDetailViewModel.handle(RoomDetailAction.SendMedia(grouped.notPreviewables, false))
        }
        if (grouped.previewables.isNotEmpty()) {
            val intent = AttachmentsPreviewActivity.newIntent(requireContext(), AttachmentsPreviewArgs(grouped.previewables))
            contentAttachmentActivityResultLauncher.launch(intent)
        }
    }

    override fun onCameraContentAttachmentsReady(attachments: List<ContentAttachmentData>) {
        val grouped = attachments.toGroupedContentAttachmentData()
        if (grouped.notPreviewables.isNotEmpty()) {
            // Send the not previewable attachments right now (?)
            roomDetailViewModel.handle(RoomDetailAction.SendMedia(grouped.notPreviewables, false))
        }
        if (grouped.previewables.isNotEmpty()) {
            val intent = AttachmentsPreviewActivity.newIntent(requireContext(), AttachmentsPreviewArgs(grouped.previewables))
            contentAttachmentActivityResultLauncher.launch(intent)
        }
    }

    override fun onAttachmentsProcessFailed() {
        Toast.makeText(requireContext(), R.string.error_attachment, Toast.LENGTH_SHORT).show()
    }

    override fun onContactAttachmentReady(contactAttachment: ContactAttachment) {
        super.onContactAttachmentReady(contactAttachment)
        val formattedContact = contactAttachment.toHumanReadable()
        roomDetailViewModel.handle(RoomDetailAction.SendMessage(formattedContact, false))
    }

    private fun onViewWidgetsClicked() {
        RoomWidgetsBottomSheet.newInstance()
                .show(childFragmentManager, "ROOM_WIDGETS_BOTTOM_SHEET")
    }

    override fun onTapToReturnToCall() {
        sharedCallActionViewModel.activeCall.value?.let { call ->
            VectorCallActivity.newIntent(
                    context = requireContext(),
                    callId = call.callId,
                    roomId = call.roomId,
                    otherUserId = call.otherUserId,
                    isIncomingCall = !call.isOutgoing,
                    isVideoCall = call.isVideoCall,
                    mode = null
            ).let {
                startActivity(it)
            }
        }
    }

//    interface Listener {
//        fun onRecorderStarted()
//        fun onRecorderLocked()
//        fun onRecorderFinished()
//        fun onRecorderCanceled()
//        fun onRecorderPermissionRequired()
//    }

    private var fileName: String = ""
//    private var fileString: String = ""
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var recorder: Recorder? = null
//    private lateinit var wavRecorder: WavRecorder? = null

    override fun onRecordPressed() {
//        fileName = "${requireContext().externalCacheDir?.absolutePath}/" + fileString
        val current = DateProvider.toTimestamp(DateProvider.currentLocalDateTime())
        val formattedDate = dateFormatter.format(current, DateFormatKind.MESSAGE_DETAIL)
        val me = session.getUser(session.myUserId)?.toMatrixItem()
        fileName = "VNR:::${me?.getBestName()}:::$formattedDate"

        recordTime.display()
        slideToCancel.display()

        XlinxUtil.fadeOut(quickAudioToggle, XlinxUtil.FADE_TIME, View.INVISIBLE)
        XlinxUtil.fadeOut(composerLayout.attachmentButton, XlinxUtil.FADE_TIME, View.INVISIBLE)
        XlinxUtil.fadeOut(composerLayout.composerShieldImageView, XlinxUtil.FADE_TIME, View.INVISIBLE)
        XlinxUtil.fadeOut(composerLayout.composerEditText, XlinxUtil.FADE_TIME, View.INVISIBLE)

        onRecordStarted()
    }

    override fun onRecordReleased() {
        val elapsedTime: Long = onRecordHideEvent()

        if (elapsedTime > 1000) {
            onRecordFinished();
        } else {
            Toast.makeText(requireContext(), getString(R.string.InputPanel_tap_and_hold_to_record_a_voice_message_release_to_send), Toast.LENGTH_LONG).show();
            onRecordCanceled();
        }
    }

    @Suppress("DEPRECATION")
    override fun onRecordCanceled() {
        onRecordHideEvent()

        val vibrator: Vibrator = ServiceUtil.getVibrator(requireContext())
        vibrator.vibrate(50)

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        stopRecording(false)
    }

    override fun onRecordLocked() {
        slideToCancel.hide()
        recordLockCancel.visibility = View.VISIBLE
    }

    override fun onRecordFinished() {
        stopRecording(true)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("LogNotTimber")
    override fun onRecordStarted() {
        val vibrator: Vibrator = ServiceUtil.getVibrator(requireContext())
        vibrator.vibrate(20)

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_WRITE_AUDIO_PERMISSION)
        } else {
            recorder = OmRecorder.wav(
                    PullTransport.Default(mic()) { audioChunk ->
                        Log.i("omrecorder", audioChunk.toString())
                    }, file())
        }


        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            startRecording()
        }
    }


    override fun onRecordMoved(offsetX: Float, absoluteX: Float) {
        slideToCancel.moveTo(offsetX)

        val direction = ViewCompat.getLayoutDirection(composerLayout)
        val position = absoluteX / recordingContainer.width

        if (direction == ViewCompat.LAYOUT_DIRECTION_LTR && position <= 0.5 ||
                direction == ViewCompat.LAYOUT_DIRECTION_RTL && position >= 0.6) {
            microphoneRecorderView.cancelAction()
        }
    }

    override fun onRecordPermissionRequired() {
        ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun onRecordHideEvent(): Long {
        recordLockCancel.visibility = View.GONE
        val future: ListenableFuture<Void> = slideToCancel.hide()
        val elapsedTime = recordTime.hide()
        future.addListener(object : AssertedSuccessListener<Void?>() {
            override fun onSuccess(result: Void?) {
                XlinxUtil.fadeIn(quickAudioToggle, XlinxUtil.FADE_TIME)
                XlinxUtil.fadeIn(composerLayout.attachmentButton, XlinxUtil.FADE_TIME)
                XlinxUtil.fadeIn(composerLayout.composerShieldImageView, XlinxUtil.FADE_TIME)
                XlinxUtil.fadeIn(composerLayout.composerEditText, XlinxUtil.FADE_TIME)
            }
        })
        return elapsedTime
    }

    private fun startRecording() {
        recorder?.startRecording()
    }

    private fun mic(): PullableSource? {
        return PullableSource.Default(
                AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                )
        )
    }

    @NonNull
    private fun file(): File? {
        val filesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!
        if (!filesDir.exists()) {
            if (filesDir.mkdirs()) {
            }
        }
        return File(filesDir, fileName)
    }

    private fun stopRecording(isSending: Boolean) {
        try {
            recorder?.stopRecording()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (isSending) {
                sendVoiceNote()
            }
        }
    }


    private fun sendVoiceNote() {
        val elapsedTime: Long = onRecordHideEvent()

        val audioSize = file()?.length()

        if (elapsedTime > 1000) {
            if (audioSize != null) {
                attachmentsHelper.onVoiceNoteResult(Uri.fromFile(file()), fileName, audioSize, elapsedTime, "audio/x-wav")
            }
        }
    }
}
