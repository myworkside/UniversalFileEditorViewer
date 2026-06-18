package com.sumitupdat.universalfileeditorviewer.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val isControllerVisible: Boolean = true,
    val fileName: String = "",
    val isVideo: Boolean = true,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller = _controller.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val c = controllerFuture?.get()
            _controller.value = c
            setupController(c)
        }, MoreExecutors.directExecutor())
        
        startPositionUpdate()
    }

    private fun setupController(c: MediaController?) {
        c?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.update { it.copy(duration = c.duration.coerceAtLeast(0)) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleMode = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }
        })
    }

    private fun startPositionUpdate() {
        viewModelScope.launch {
            while (true) {
                _controller.value?.let {
                    _uiState.update { state -> 
                        state.copy(
                            currentPosition = it.currentPosition,
                            duration = it.duration.coerceAtLeast(0)
                        )
                    }
                }
                delay(1000)
            }
        }
    }

    fun playFile(file: File, isVideo: Boolean) {
        _uiState.update { it.copy(fileName = file.name, isVideo = isVideo) }
        viewModelScope.launch {
            while (_controller.value == null) delay(100)
            _controller.value?.let {
                it.setMediaItem(MediaItem.fromUri(file.absolutePath))
                it.prepare()
                it.play()
            }
        }
    }

    fun togglePlayPause() {
        _controller.value?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        _controller.value?.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        _controller.value?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleShuffle() {
        _controller.value?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        _controller.value?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }

    fun adjustVolume(delta: Float) {
        val newVolume = (_uiState.value.volume + delta).coerceIn(0f, 1f)
        _controller.value?.volume = newVolume
        _uiState.update { it.copy(volume = newVolume) }
    }

    fun adjustBrightness(delta: Float) {
        val newBrightness = (_uiState.value.brightness + delta).coerceIn(0f, 1f)
        _uiState.update { it.copy(brightness = newBrightness) }
    }

    fun toggleControllerVisibility() {
        _uiState.update { it.copy(isControllerVisible = !it.isControllerVisible) }
    }

    override fun onCleared() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }
}
