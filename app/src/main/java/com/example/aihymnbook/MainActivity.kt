package com.example.aihymnbook

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    private val viewModel: HymnViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HymnViewModel(applicationContext) as T
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            AIHymnBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigator(viewModel: HymnViewModel) {
    when (viewModel.currentScreen) {
        AppScreen.HOME -> HomeScreen(viewModel)
        AppScreen.RECORD_AND_TRANSCRIBE -> RecordAndTranscribeScreen(viewModel)
        AppScreen.AI_ADDONS -> AiAddonsScreen(viewModel)
        AppScreen.FINALIZE_HYMN -> FinalizeHymnScreen(viewModel)
        AppScreen.HYMN_BOOK -> HymnBookScreen(viewModel)
        AppScreen.HYMN_DETAIL -> HymnDetailScreen(viewModel)
    }
}

// Reusable Step Progress Header for Option 1 Wizard
@Composable
fun CreationStepHeader(currentStep: Int, title: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 3-Step Visual Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val steps = listOf("1. Sing & Lyrics", "2. Arrangement", "3. Save Hymn")
            steps.forEachIndexed { index, label ->
                val stepNum = index + 1
                val isCompletedOrActive = stepNum <= currentStep
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isCompletedOrActive) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (stepNum == currentStep) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCompletedOrActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 1. Upgraded Sanctuary Home Screen with Blurred Logo Wallpaper & Sharp Center Emblem
@Composable
fun HomeScreen(viewModel: HymnViewModel) {
    val savedHymns by viewModel.savedHymns.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Full-screen blurred version of the logo
        Image(
            painter = painterResource(id = R.drawable.hymnbook_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        )

        // Layer 2: Translucent scrim overlay so text & cards stay crisp and legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f))
        )

        // Layer 3: Foreground UI Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Custom Logo Emblem & App Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                    shadowElevation = 8.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hymnbook_logo),
                        contentDescription = "Sanctuary Hymn Studio Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Sanctuary Hymn Studio",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Compose sacred songs from your voice and preserve them in your personal hymn book.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Two Main Feature Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeFeatureCard(
                    title = "Create a New Hymn",
                    subtitle = "Record your melody, transcribe lyrics with AI, and design choir & organ arrangements.",
                    badgeText = "STUDIO",
                    icon = Icons.Default.Mic,
                    isPrimary = true,
                    onClick = { viewModel.currentScreen = AppScreen.RECORD_AND_TRANSCRIBE }
                )

                HomeFeatureCard(
                    title = "Open the Hymn Book",
                    subtitle = "Browse your saved hymns, listen to recordings, and sing along with karaoke lyrics.",
                    badgeText = "${savedHymns.size} SAVED",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    isPrimary = false,
                    onClick = { viewModel.currentScreen = AppScreen.HYMN_BOOK }
                )
            }

            Text(
                text = "“Sing to the Lord a new song”",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeFeatureCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = if (!isPrimary) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (isPrimary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPrimary) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPrimary) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPrimary) Color.White else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrimary) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Step 1: Record Voice -> AI Transcribe -> Editable Parchment Lyrics
@Composable
fun RecordAndTranscribeScreen(viewModel: HymnViewModel) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CreationStepHeader(
            currentStep = 1,
            title = "Sing & Transcribe",
            onBack = {
                viewModel.stopAudio()
                viewModel.currentScreen = AppScreen.HOME
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (viewModel.isRecording) viewModel.stopRecording()
                            else viewModel.startRecording(context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = if (viewModel.isRecording) Modifier.scale(pulseScale) else Modifier
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (viewModel.isRecording) "Stop Rec" else "Record Voice")
                    }

                    OutlinedButton(
                        onClick = {
                            if (viewModel.isPlaying) viewModel.stopAudio()
                            else viewModel.playAudio(null, context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = viewModel.recordedFilePath != null && !viewModel.isRecording,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (viewModel.isPlaying) "Stop" else "Playback")
                    }
                }

                Button(
                    onClick = { viewModel.generateLyricsFromRecording() },
                    enabled = viewModel.recordedFilePath != null && !viewModel.isRecording && !viewModel.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI is Transcribing Your Voice...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transcribe Voice to Lyrics")
                    }
                }
            }
        }

        OutlinedTextField(
            value = viewModel.generatedLyrics,
            onValueChange = { viewModel.generatedLyrics = it },
            label = { Text("Hymn Lyrics (Tap anywhere to edit)") },
            placeholder = { Text("Record your voice above and tap 'Transcribe', or type your lyrics directly here...") },
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Button(
            onClick = {
                viewModel.stopAudio()
                viewModel.currentScreen = AppScreen.AI_ADDONS
            },
            enabled = viewModel.generatedLyrics.isNotBlank() && !viewModel.isLoading,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continue to Arrangement Studio →", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Step 2: Musical Arrangement Studio
@Composable
fun AiAddonsScreen(viewModel: HymnViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val genres = listOf("Traditional Gospel Hymn", "Contemporary Worship", "Cinematic Orchestral", "Acoustic Praise")
    val moods = listOf("Reverent & Uplifting", "Triumphant & Majestic", "Peaceful & Meditative", "Joyful & Celebratory")
    val instruments = listOf("Acoustic Piano & Pipe Organ", "Orchestral Strings & Brass", "Soft Acoustic Guitar", "Warm Choir Pads")
    val choirs = listOf("4-Part SATB Choir Harmony", "Ethereal Soprano Harmonies", "Deep Baritone & Tenor Backing", "Soloist with Cathedral Reverb")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CreationStepHeader(
            currentStep = 2,
            title = "Arrangement Studio",
            onBack = {
                viewModel.stopAudio()
                viewModel.currentScreen = AppScreen.RECORD_AND_TRANSCRIBE
            }
        )

        Text(
            "Select the musical style, instruments, and choir harmonies for this hymn.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StyleChipGroup("Genre", genres, viewModel.selectedGenre) { viewModel.selectedGenre = it }
        StyleChipGroup("Mood & Atmosphere", moods, viewModel.selectedMood) { viewModel.selectedMood = it }
        StyleChipGroup("Instrumentation", instruments, viewModel.selectedInstrument) { viewModel.selectedInstrument = it }
        StyleChipGroup("Choir & Vocal Backing", choirs, viewModel.selectedChoir) { viewModel.selectedChoir = it }

        Button(
            onClick = {
                viewModel.generateAiAddons(
                    "${viewModel.selectedGenre} (${viewModel.selectedMood}) featuring ${viewModel.selectedInstrument} and ${viewModel.selectedChoir}"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !viewModel.isAiAdding,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            if (viewModel.isAiAdding) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Composing Choir & Organ Guide...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Choir & Organ Arrangement")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Conductor & Choir Notes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.aiArrangementNotes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (viewModel.recordedFilePath != null) {
            OutlinedButton(
                onClick = {
                    if (viewModel.isPlaying) viewModel.stopAudio()
                    else viewModel.playAudio(null, context)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (viewModel.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (viewModel.isPlaying) "Stop Vocal Preview" else "Preview Your Recorded Melody")
            }
        }

        Button(
            onClick = {
                viewModel.stopAudio()
                viewModel.currentScreen = AppScreen.FINALIZE_HYMN
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Next: Title & Save Hymn →", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun StyleChipGroup(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { item ->
                FilterChip(
                    selected = selectedOption == item,
                    onClick = { onSelect(item) },
                    label = { Text(item) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

// Step 3: Finalize Name & Description
@Composable
fun FinalizeHymnScreen(viewModel: HymnViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CreationStepHeader(
            currentStep = 3,
            title = "Add to Hymn Book",
            onBack = { viewModel.currentScreen = AppScreen.AI_ADDONS }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = viewModel.songTitle,
                    onValueChange = { viewModel.songTitle = it },
                    label = { Text("Hymn Title") },
                    placeholder = { Text("e.g., Grace in the Morning") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.songDescription,
                    onValueChange = { viewModel.songDescription = it },
                    label = { Text("Occasion / Description") },
                    placeholder = { Text("e.g., Sunday Morning Opening Hymn, Key of G") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hymn Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("Style: ${viewModel.selectedGenre} • ${viewModel.selectedMood}", style = MaterialTheme.typography.bodySmall)
                Text("Choir: ${viewModel.selectedChoir}", style = MaterialTheme.typography.bodySmall)
                Text("Audio Attached: ${if (viewModel.recordedFilePath != null) "Yes (Vocal Recording)" else "Lyrics Only"}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.saveCompletedHymn() },
            enabled = viewModel.songTitle.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.BookmarkAdded, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to Hymn Book", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Option 2: Upgraded Hymn Book Library Screen
@Composable
fun HymnBookScreen(viewModel: HymnViewModel) {
    val savedHymns by viewModel.savedHymns.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.currentScreen = AppScreen.HOME }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                }
                Text("The Hymn Book", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${savedHymns.size} Hymns",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (savedHymns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text("Your Hymn Book is Empty", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Create your first song in the Studio to see it listed here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { viewModel.currentScreen = AppScreen.RECORD_AND_TRANSCRIBE }) {
                        Text("Create First Hymn")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(savedHymns) { index, hymn ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectedHymn = hymn
                                viewModel.prepareTimedLyrics(hymn.lyrics)
                                viewModel.currentScreen = AppScreen.HYMN_DETAIL
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${savedHymns.size - index}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = hymn.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (hymn.description.isNotBlank()) {
                                    Text(
                                        text = hymn.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = hymn.lyrics.replace("\n", " "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { viewModel.deleteHymn(hymn) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Hymn",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Hymn Detail & True Line-by-Line Synchronized Karaoke Reader
@Composable
fun HymnDetailScreen(viewModel: HymnViewModel) {
    val context = LocalContext.current
    val hymn = viewModel.selectedHymn

    val lyricLines = remember(hymn?.lyrics) {
        hymn?.lyrics?.lines()?.filter { it.isNotBlank() } ?: emptyList()
    }

    val activeLineIndex = remember(viewModel.currentPlaybackPosition, viewModel.isPlaying, lyricLines) {
        if (!viewModel.isPlaying || lyricLines.isEmpty()) -1
        else {
            val totalDuration = viewModel.audioPlayer?.duration?.takeIf { it > 0 } ?: (lyricLines.size * 4000L)
            val msPerLine = (totalDuration / lyricLines.size).coerceAtLeast(1000L)
            (viewModel.currentPlaybackPosition / msPerLine).toInt().coerceIn(0, lyricLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.stopAudio()
                viewModel.currentScreen = AppScreen.HYMN_BOOK
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Book")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = hymn?.title ?: "Hymn", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                if (!hymn?.description.isNullOrBlank()) {
                    Text(text = hymn?.description ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        if (!hymn?.aiArrangement.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Choir & Organ Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = hymn?.aiArrangement ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(lyricLines) { index, line ->
                    val isCurrentLine = index == activeLineIndex
                    val bgColor by animateColorAsState(
                        targetValue = if (isCurrentLine) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        label = "lineHighlight"
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = line,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (!hymn?.audioPath.isNullOrBlank()) {
            Button(
                onClick = {
                    if (viewModel.isPlaying) viewModel.stopAudio()
                    else viewModel.playAudio(hymn?.audioPath, context)
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(if (viewModel.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewModel.isPlaying) "Stop Playback" else "Play Recording with Karaoke Sync",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}