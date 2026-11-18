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
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

enum class AppScreen {
    HOME,
    RECORD_AND_TRANSCRIBE,
    AI_ADDONS,
    FINALIZE_HYMN,
    HYMN_BOOK,
    HYMN_DETAIL
}

data class TimedLyric(val startMs: Long, val endMs: Long, val text: String)

class HymnViewModel(context: Context) : ViewModel() {

    private val dao = HymnDatabase.getDatabase(context).hymnDao()
    val savedHymns: StateFlow<List<HymnEntity>> = dao.getAllHymns()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Navigation State
    var currentScreen by mutableStateOf(AppScreen.HOME)

    // Song Creation States
    var generatedLyrics by mutableStateOf("")
    var songTitle by mutableStateOf("")
    var songDescription by mutableStateOf("")
    var aiArrangementNotes by mutableStateOf("Standard 4-part choir harmony with organ backing.")

    // ElevenLabs Style Music Parameters
    var selectedGenre by mutableStateOf("Traditional Gospel Hymn")
    var selectedMood by mutableStateOf("Reverent & Uplifting")
    var selectedInstrument by mutableStateOf("Acoustic Piano & Pipe Organ")
    var selectedChoir by mutableStateOf("4-Part SATB Choir Harmony")

    var isLoading by mutableStateOf(false)
    var isAiAdding by mutableStateOf(false)

    // Recording & Playback States
    var isRecording by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var recordedFilePath: String? = null
    var currentPlaybackPosition by mutableLongStateOf(0L)
    private var playbackJob: Job? = null

    var selectedHymn: HymnEntity? = null
    var timedLyrics by mutableStateOf<List<TimedLyric>>(emptyList())

    private var mediaRecorder: MediaRecorder? = null
    var audioPlayer: ExoPlayer? = null

    private val httpClient = OkHttpClient()

    private val modelPriorityList = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-flash-latest"
    )

    private suspend fun generateWithFallback(promptContent: Content): String {
        var lastException: Exception? = null

        for (modelName in modelPriorityList) {
            try {
                val model = GenerativeModel(modelName = modelName, apiKey = BuildConfig.GEMINI_API_KEY)
                val response = model.generateContent(promptContent)
                val text = response.text
                if (!text.isNullOrBlank()) return text
            } catch (e: Exception) {
                Log.w("HymnViewModel", "Model $modelName busy or failed: ${e.message}")
                lastException = e
                delay(1000)
            }
        }
        throw lastException ?: Exception("All models are currently busy. Please try again.")
    }

    fun generateLyricsFromRecording() {
        val path = recordedFilePath ?: return
        if (isLoading) return

        isLoading = true
        generatedLyrics = "Listening to your recording and transcribing lyrics..."

        viewModelScope.launch {
            try {
                val audioBytes = withContext(Dispatchers.IO) { File(path).readBytes() }
                val multimodalPrompt = content {
                    blob("audio/mp4", audioBytes)
                    text(
                        "Listen carefully to this vocal recording of a hymn or worship song. " +
                                "Transcribe the exact lyrics being sung. Format the output neatly into verses and a chorus " +
                                "so it can be read as a church hymn. If parts are hummed or unclear, fill in reverent, " +
                                "fitting Christian hymn lyrics that match the rhythm and theme."
                    )
                }
                val resultText = generateWithFallback(multimodalPrompt)
                generatedLyrics = resultText
                prepareTimedLyrics(resultText)
            } catch (e: Exception) {
                Log.e("HymnViewModel", "Audio transcription error", e)
                generatedLyrics = "Error reading audio: ${e.localizedMessage}\n\nYou can still type your lyrics manually below!"
            } finally {
                isLoading = false
            }
        }
    }

    // ElevenLabs Custom Styled Song Generation
    fun generateCustomElevenLabsSong(context: Context) {
        if (generatedLyrics.isBlank() || isAiAdding) return
        isAiAdding = true
        aiArrangementNotes = "Composing custom track ($selectedGenre, $selectedMood, $selectedInstrument, $selectedChoir) via ElevenLabs..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Combine user's custom stylistic choices into a prompt for ElevenLabs TTS / Music styling
                val styledText = "Genre: $selectedGenre. Mood: $selectedMood. Instrumentation: $selectedInstrument. Vocals: $selectedChoir.\n\nLyrics:\n$generatedLyrics"

                val jsonBody = JSONObject().apply {
                    put("text", styledText)
                    put("model_id", "eleven_flash_v2_5")
                    put("voice_settings", JSONObject().apply {
                        put("stability", 0.60)
                        put("similarity_boost", 0.75)
                    })
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://api.elevenlabs.io/v1/text-to-speech/JBFqnCBsd6RMkjVDRZzb")
                    .addHeader("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
                    .addHeader("Accept", "audio/mpeg")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("ElevenLabs API Error: ${response.code} - ${response.body?.string()}")
                    }

                    val audioBytes = response.body?.bytes() ?: throw Exception("Empty audio response")
                    val outputFile = File(context.filesDir, "elevenlabs_custom_${System.currentTimeMillis()}.mp3")
                    outputFile.writeBytes(audioBytes)

                    withContext(Dispatchers.Main) {
                        recordedFilePath = outputFile.absolutePath
                        aiArrangementNotes = "Success! Generated custom $selectedGenre song with $selectedInstrument and $selectedChoir. Tap Preview to listen."
                    }
                }
            } catch (e: Exception) {
                Log.e("HymnViewModel", "ElevenLabs Error", e)
                withContext(Dispatchers.Main) {
                    aiArrangementNotes = "Failed to generate custom song: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isAiAdding = false
                }
            }
        }
    }

    fun generateAiAddons(addonType: String) {
        if (isAiAdding) return
        isAiAdding = true
        viewModelScope.launch {
            try {
                val prompt = content {
                    text(
                        "Based on these hymn lyrics:\n$generatedLyrics\n\n" +
                                "Create a musical arrangement guide focusing on '$addonType' " +
                                "(e.g., orchestral chord progressions or 4-part soprano-alto-tenor-bass background choir harmonies). " +
                                "Keep it concise (3-4 sentences)."
                    )
                }
                aiArrangementNotes = generateWithFallback(prompt)
            } catch (e: Exception) {
                aiArrangementNotes = "Failed to load AI add-on: ${e.message}"
            } finally {
                isAiAdding = false
            }
        }
    }

    fun prepareTimedLyrics(lyricsText: String) {
        val lines = lyricsText.lines().filter { it.isNotBlank() }
        val durationPerLine = 4000L
        var start = 0L
        timedLyrics = lines.map { line ->
            val item = TimedLyric(start, start + durationPerLine, line)
            start += durationPerLine
            item
        }
    }

    fun saveCompletedHymn() {
        if (songTitle.isBlank()) return
        prepareTimedLyrics(generatedLyrics)

        viewModelScope.launch {
            val hymn = HymnEntity(
                title = songTitle,
                description = songDescription,
                lyrics = generatedLyrics,
                audioPath = recordedFilePath,
                aiArrangement = aiArrangementNotes
            )
            dao.insertHymn(hymn)

            generatedLyrics = ""
            songTitle = ""
            songDescription = ""
            recordedFilePath = null
            aiArrangementNotes = "Standard 4-part choir harmony with organ backing."
            currentScreen = AppScreen.HYMN_BOOK
        }
    }

    fun deleteHymn(hymn: HymnEntity) {
        viewModelScope.launch { dao.deleteHymn(hymn) }
    }

    fun startRecording(context: Context) {
        stopAudio()
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
            mediaRecorder?.apply { stop(); release() }
        } catch (e: Exception) {
            Log.e("HymnViewModel", "Stop recording error", e)
        }
        mediaRecorder = null
        isRecording = false
    }

    fun playAudio(path: String?, context: Context) {
        val targetPath = path ?: recordedFilePath ?: return

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