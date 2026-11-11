package com.example.aihymnbook

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                HymnStudioScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HymnStudioScreen(viewModel: HymnViewModel) {
    val context = LocalContext.current
    val savedHymns by viewModel.savedHymns.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AI Hymn Studio & Library",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = viewModel.promptInput,
            onValueChange = { viewModel.promptInput = it },
            label = { Text("Enter theme (e.g., Grace, Comfort)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.generateHymn() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        ) {
            Text(if (viewModel.isLoading) "Composing..." else "Generate Hymn Lyrics")
        }

        // Recorder Controls & Save Button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (viewModel.isRecording) viewModel.stopRecording()
                    else viewModel.startRecording(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (viewModel.isRecording) "Stop Rec" else "Record")
            }

            Button(
                onClick = {
                    if (viewModel.isPlaying) viewModel.stopAudio()
                    else viewModel.playAudio(null, null, context)
                },
                enabled = viewModel.recordedFilePath != null && !viewModel.isRecording,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (viewModel.isPlaying) "Stop" else "Play Audio")
            }

            Button(
                onClick = { viewModel.saveHymn() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Hymn")
            }
        }

        // Live Synchronized Lyrics Card (Restored)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Live Synchronized Lyrics",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(viewModel.timedLyrics) { line ->
                        val isHighlighted = viewModel.isPlaying &&
                                viewModel.currentPlaybackPosition in line.startMs..line.endMs

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        Divider()

        Text(text = "Saved Hymns (${savedHymns.size})", style = MaterialTheme.typography.titleMedium)

        // List of Saved Hymns with Play, Stop, Edit, Delete buttons
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedHymns) { hymn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = hymn.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = hymn.lyrics, style = MaterialTheme.typography.bodyMedium, maxLines = 3)

                        // Action buttons: Play, Stop, Edit, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!hymn.audioPath.isNullOrBlank()) {
                                TextButton(onClick = { viewModel.playAudio(hymn.audioPath, hymn.lyrics, context) }) {
                                    Text("Play")
                                }
                            }

                            TextButton(onClick = { viewModel.stopAudio() }) {
                                Text("Stop")
                            }

                            TextButton(onClick = {
                                viewModel.promptInput = hymn.title
                                viewModel.generatedLyrics = hymn.lyrics
                                viewModel.recordedFilePath = hymn.audioPath
                            }) {
                                Text("Edit")
                            }

                            TextButton(onClick = { viewModel.deleteHymn(hymn) }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}