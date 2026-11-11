package com.pixelgram.android.music

import android.content.Context
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions = CastOptions.Builder()
        .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
        .setStopReceiverApplicationWhenEndingSession(true)
        .setRemoteToLocalEnabled(true)
        .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}

data class CastDevice(val id: String, val name: String, val description: String, val selected: Boolean)

data class CastState(
    val available: Boolean = true,
    val devices: List<CastDevice> = emptyList(),
    val connecting: String? = null,
    val connected: String? = null,
    val ready: Boolean = false,
    val routes: Int = 0,
    val error: String? = null,
)

class CastLink(private val context: Context) {
    private val stateFlow = MutableStateFlow(CastState())
    val state: StateFlow<CastState> = stateFlow.asStateFlow()
    private val server by lazy { CastServer(context) }
    private var castContext: CastContext? = null
    private var session: CastSession? = null
    private var scanning = false
    var onConnected: (() -> Unit)? = null
    var onDisconnected: ((Long) -> Unit)? = null
    var onStatus: (() -> Unit)? = null
    private var lastPosition = 0L

    private val fallbackSelector by lazy {
        MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
            .build()
    }
    private val selector: MediaRouteSelector get() = castContext?.mergedSelector ?: fallbackSelector
    private val router by lazy { MediaRouter.getInstance(context) }

    private val routes = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refresh()
        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refresh()
    }

    private val remoteCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            client?.let { lastPosition = it.approximateStreamPosition }
            onStatus?.invoke()
        }
    }

    private val sessions = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            stateFlow.value = stateFlow.value.copy(connecting = session.castDevice?.friendlyName)
        }
        override fun onSessionStarted(session: CastSession, sessionId: String) = attach(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = attach(session)
        override fun onSessionStartFailed(session: CastSession, error: Int) = detach()
        override fun onSessionEnding(session: CastSession) {
            client?.let { lastPosition = it.approximateStreamPosition }
        }
        override fun onSessionEnded(session: CastSession, error: Int) = detach()
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = detach()
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    val client: RemoteMediaClient? get() = session?.remoteMediaClient
    val active: Boolean get() = session?.isConnected == true && client != null

    private var initStarted = false

    fun init() {
        if (castContext != null || initStarted) return
        initStarted = true
        runCatching {
            CastContext.getSharedInstance(context, androidx.core.content.ContextCompat.getMainExecutor(context))
                .addOnSuccessListener { cast ->
                    castContext = cast
                    stateFlow.value = stateFlow.value.copy(ready = true, error = null)
                    cast.sessionManager.addSessionManagerListener(sessions, CastSession::class.java)
                    cast.sessionManager.currentCastSession?.takeIf { it.isConnected }?.let(::attach)
                    if (scanning) {
                        runCatching {
                            router.removeCallback(routes)
                            router.addCallback(selector, routes, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
                        }
                    }
                    refresh()
                }
                .addOnFailureListener { e -> stateFlow.value = stateFlow.value.copy(available = false, error = e.message ?: e.javaClass.simpleName) }
        }.onFailure { e -> stateFlow.value = stateFlow.value.copy(available = false, error = e.message ?: e.javaClass.simpleName) }
    }

    fun openDialog(activity: Context) {
        init()
        runCatching {
            router.setRouterParams(
                androidx.mediarouter.media.MediaRouterParams.Builder()
                    .setOutputSwitcherEnabled(true)
                    .setTransferToLocalEnabled(true)
                    .build(),
            )
        }
        if (runCatching { androidx.mediarouter.app.SystemOutputSwitcherDialogController.showDialog(activity) }.getOrDefault(false)) return
        runCatching {
            if (active) {
                androidx.mediarouter.app.MediaRouteControllerDialog(activity).show()
            } else {
                androidx.mediarouter.app.MediaRouteChooserDialog(activity).apply { routeSelector = selector }.show()
            }
        }
    }

    fun scan(on: Boolean) {
        init()
        if (on == scanning) return
        scanning = on
        runCatching {
            if (on) {
                router.addCallback(selector, routes, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
                refresh()
            } else {
                router.removeCallback(routes)
            }
        }.onFailure { stateFlow.value = stateFlow.value.copy(available = false) }
    }

    fun connect(id: String) {
        val route = router.routes.firstOrNull { it.id == id } ?: return
        stateFlow.value = stateFlow.value.copy(connecting = route.name)
        route.select()
    }

    fun disconnect() {
        client?.let { lastPosition = it.approximateStreamPosition }
        castContext?.sessionManager?.endCurrentSession(true)
        runCatching { router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED) }
    }

    fun load(file: File, mime: String, title: String, artist: String, cover: File?, position: Long, autoplay: Boolean) {
        val remote = client ?: return
        server.clear()
        val url = server.publish(file, mime) ?: return
        val metadata = com.google.android.gms.cast.MediaMetadata(com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE, title)
            putString(com.google.android.gms.cast.MediaMetadata.KEY_ARTIST, artist)
            cover?.let { art -> server.publish(art, "image/jpeg")?.let { addImage(WebImage(android.net.Uri.parse(it))) } }
        }
        val info = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mime)
            .setMetadata(metadata)
            .build()
        remote.load(MediaLoadRequestData.Builder().setMediaInfo(info).setAutoplay(autoplay).setCurrentTime(position).build())
    }

    fun toggle() {
        client?.togglePlayback()
    }

    fun seek(position: Long) {
        client?.seek(MediaSeekOptions.Builder().setPosition(position).build())
    }

    fun setSpeed(speed: Float) {
        client?.setPlaybackRate(speed.toDouble())
    }

    val position: Long get() = client?.approximateStreamPosition ?: lastPosition
    val duration: Long get() = client?.streamDuration ?: 0L
    val playing: Boolean get() = client?.let { it.isPlaying || it.isBuffering || it.isLoadingNextItem } == true
    val finished: Boolean get() = client?.let { it.playerState == MediaStatus.PLAYER_STATE_IDLE && it.idleReason == MediaStatus.IDLE_REASON_FINISHED } == true

    private fun attach(session: CastSession) {
        this.session = session
        session.remoteMediaClient?.registerCallback(remoteCallback)
        stateFlow.value = stateFlow.value.copy(connecting = null, connected = session.castDevice?.friendlyName ?: "Chromecast")
        refresh()
        onConnected?.invoke()
    }

    private fun detach() {
        val was = session != null
        session?.remoteMediaClient?.unregisterCallback(remoteCallback)
        session = null
        server.clear()
        stateFlow.value = stateFlow.value.copy(connecting = null, connected = null)
        refresh()
        if (was) onDisconnected?.invoke(lastPosition)
    }

    private fun refresh() {
        val all = runCatching { router.routes.count { !it.isDefault } }.getOrDefault(0)
        val list = runCatching {
            router.routes.filter { !it.isDefault && it.isEnabled && it.matchesSelector(selector) }
                .map { CastDevice(it.id, it.name, it.description.orEmpty(), it.isSelected) }
        }.getOrDefault(emptyList())
        stateFlow.value = stateFlow.value.copy(devices = list, routes = all)
    }
}
