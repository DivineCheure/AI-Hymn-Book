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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class TimedLyric(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

class HymnViewModel(context: Context) : ViewModel() {

    private val dao = HymnDatabase.getDatabase(context).hymnDao()

    val savedHymns: StateFlow<List<HymnEntity>> = dao.getAllHymns()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var promptInput by mutableStateOf("")
    var generatedLyrics by mutableStateOf("Enter a theme above and tap Generate...")
    var isLoading by mutableStateOf(false)

    var isRecording by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var recordedFilePath: String? = null

    var currentPlaybackPosition by mutableLongStateOf(0L)
    private var playbackJob: Job? = null

    var timedLyrics by mutableStateOf(
        listOf(
            TimedLyric(0L, 5000L, "Generated lyrics will appear here...")
        )
    )

    private var mediaRecorder: MediaRecorder? = null
    var audioPlayer: ExoPlayer? = null

    // Using the stable flash-latest endpoint
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
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
                val textResult = response.text ?: "Could not generate lyrics."
                generatedLyrics = textResult
                parseLyricsIntoTimedLines(textResult, 12000L)
            } catch (e: Exception) {
                Log.e("HymnViewModel", "API Error", e)
                generatedLyrics = "Error: ${e.localizedMessage ?: e.toString()}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun parseLyricsIntoTimedLines(text: String, totalDurationMs: Long) {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val durationPerLine = totalDurationMs / lines.size
        timedLyrics = lines.mapIndexed { index, lineText ->
            TimedLyric(
                startMs = index * durationPerLine,
                endMs = (index + 1) * durationPerLine,
                text = lineText
            )
        }
    }

    fun saveHymn() {
        val titleToSave = if (promptInput.isBlank()) "Worship Hymn" else promptInput

        viewModelScope.launch {
            val hymn = HymnEntity(
                title = titleToSave,
                lyrics = generatedLyrics,
                audioPath = recordedFilePath
            )
            dao.insertHymn(hymn)
        }
    }

    fun deleteHymn(hymn: HymnEntity) {
        viewModelScope.launch {
            dao.deleteHymn(hymn)
        }
    }

    fun startRecording(context: Context) {
        val audioFile = File(context.filesDir, "hymn_${System.currentTimeMillis()}.m4a")
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

    fun playAudio(path: String?, lyricsToParse: String?, context: Context) {
        val targetPath = path ?: recordedFilePath ?: return
        val textToUse = lyricsToParse ?: generatedLyrics

        audioPlayer?.release()
        audioPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(targetPath)
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        isPlaying = true

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            delay(300)
            val trueDuration = audioPlayer?.duration ?: 0L
            if (trueDuration > 0 && trueDuration != C.TIME_UNSET) {
                parseLyricsIntoTimedLines(textToUse, trueDuration)
            }

            while (isActive && isPlaying) {
                currentPlaybackPosition = audioPlayer?.currentPosition ?: 0L
                if (audioPlayer?.isPlaying == false && currentPlaybackPosition > 0) {
                    stopAudio()
                }
                delay(100)
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