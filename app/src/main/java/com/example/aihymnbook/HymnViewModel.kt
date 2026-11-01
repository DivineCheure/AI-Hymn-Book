package com.example.aihymnbook

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class TimedLyric(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

class HymnViewModel : ViewModel() {

    var promptInput by mutableStateOf("")
    var generatedLyrics by mutableStateOf("Your generated hymn will appear here...")
    var isLoading by mutableStateOf(false)

    var isRecording by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var recordedFilePath: String? = null

    // Synchronization state
    var currentPlaybackPosition by mutableLongStateOf(0L)
    private var playbackJob: Job? = null

    // Sample synchronized lyric lines (Timed in milliseconds)
    var timedLyrics by mutableStateOf(
        listOf(
            TimedLyric(0L, 5000L, "Verse 1: Amazing grace! how sweet the sound"),
            TimedLyric(5001L, 10000L, "That saved a wretch like me!"),
            TimedLyric(10001L, 15000L, "I once was lost, but now am found,"),
            TimedLyric(15001L, 22000L, "Was blind, but now I see.")
        )
    )

    private var mediaRecorder: MediaRecorder? = null
    var audioPlayer: ExoPlayer? = null

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun generateHymn() {
        if (promptInput.isBlank() || isLoading) return
        isLoading = true
        generatedLyrics = "Composing your hymn..."

        viewModelScope.launch {
            try {
                val fullPrompt = "Write a traditional Christian church hymn with 3 verses and a chorus based on this theme: '$promptInput'. Use a poetic, reverent tone with an ABAB or AABB rhyme scheme."
                val response = generativeModel.generateContent(fullPrompt)
                generatedLyrics = response.text ?: "Could not generate lyrics."
            } catch (e: Exception) {
                generatedLyrics = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun startRecording(context: Context) {
        val audioFile = File(context.filesDir, "hymn_recording.m4a")
        recordedFilePath = audioFile.absolutePath

        mediaRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(recordedFilePath)
            prepare()
            start()
        }
        isRecording = true
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("HymnViewModel", "Stop recording error", e)
        }
        mediaRecorder = null
        isRecording = false
    }

    fun playAudio(context: Context) {
        if (recordedFilePath == null) return

        audioPlayer?.release()
        audioPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(recordedFilePath!!)
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        isPlaying = true

        // Start tracking playback position for lyric sync
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive && isPlaying) {
                currentPlaybackPosition = audioPlayer?.currentPosition ?: 0L
                if (audioPlayer?.isPlaying == false && currentPlaybackPosition > 0) {
                    stopAudio()
                }
                delay(100) // Update every 100ms
            }
        }
    }

    fun stopAudio() {
        playbackJob?.cancel()
        audioPlayer?.stop()
        audioPlayer?.release()
        audioPlayer = null
        isPlaying = false
        currentPlaybackPosition = 0L
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        audioPlayer?.release()
        playbackJob?.cancel()
    }
}