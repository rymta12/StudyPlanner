package com.studyplanner.app.core.util

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

// Prebuilt tracks — free YouTube audio / internet archive streams
// User sirf ek baar download karta hai (stream), locally cache nahi hota
data class StudyTrack(
    val id: String,
    val name: String,
    val emoji: String,
    val url: String,
    val description: String,
)

val PREBUILT_TRACKS = listOf(
    StudyTrack(
        id = "lofi_1",
        name = "Lo-Fi Chill Beats",
        emoji = "🎵",
        url = "https://stream.zeno.fm/f3wvbbqmdg8uv",  // Zeno FM lo-fi (free stream)
        description = "Relaxed beats for deep focus"
    ),
    StudyTrack(
        id = "rain",
        name = "Rain Sounds",
        emoji = "🌧️",
        url = "https://stream.zeno.fm/yn65sqh5x8zuv",  // Rain ambient
        description = "Soothing rain for concentration"
    ),
    StudyTrack(
        id = "nature",
        name = "Forest Ambience",
        emoji = "🌿",
        url = "https://stream.zeno.fm/0r0xa792kwzuv",  // Nature sounds
        description = "Birds & nature for calm focus"
    ),
    StudyTrack(
        id = "white_noise",
        name = "White Noise",
        emoji = "🔊",
        url = "https://stream.zeno.fm/g5as7s4mwzzuv",  // White noise
        description = "Block distractions completely"
    ),
    StudyTrack(
        id = "classical",
        name = "Classical Focus",
        emoji = "🎹",
        url = "https://stream.zeno.fm/4d61wp47p8zuv",  // Classical
        description = "Mozart effect for memory"
    ),
    StudyTrack(
        id = "custom",
        name = "Custom URL",
        emoji = "🔗",
        url = "",
        description = "Apna koi bhi stream URL daalo"
    ),
)

data class MusicState(
    val isPlaying: Boolean = false,
    val selectedTrackId: String = "lofi_1",
    val customUrl: String = "",
    val isPaused: Boolean = false,   // break ke time pause
)

@Singleton
class StudyMusicManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: ExoPlayer? = null
    private val _state = MutableStateFlow(MusicState())
    val state = _state.asStateFlow()

    fun play(trackId: String, customUrl: String = "") {
        val track = PREBUILT_TRACKS.find { it.id == trackId } ?: PREBUILT_TRACKS[0]
        val url = if (trackId == "custom") customUrl else track.url
        if (url.isBlank()) return

        _state.update { it.copy(selectedTrackId = trackId, customUrl = customUrl) }

        releasePlayer()
        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            play()
        }
        _state.update { it.copy(isPlaying = true, isPaused = false) }
    }

    // Break ke time pause karo
    fun pause() {
        player?.pause()
        _state.update { it.copy(isPlaying = false, isPaused = true) }
    }

    // Break khatam — resume karo
    fun resume() {
        player?.play()
        _state.update { it.copy(isPlaying = true, isPaused = false) }
    }

    // Session complete ya user ne band kiya
    fun stop() {
        releasePlayer()
        _state.update { it.copy(isPlaying = false, isPaused = false) }
    }

    fun isCurrentlyPlaying() = player?.isPlaying == true

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }
}