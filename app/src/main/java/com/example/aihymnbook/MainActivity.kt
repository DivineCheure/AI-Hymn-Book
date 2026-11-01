package com.example.aihymnbook

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel: HymnViewModel by viewModels()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI Hymn Studio & Karaoke",
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

        // Recorder & Player Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Audio Controls", style = MaterialTheme.typography.titleMedium)

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
                        Text(if (viewModel.isRecording) "Stop Recording" else "Record")
                    }

                    Button(
                        onClick = {
                            if (viewModel.isPlaying) viewModel.stopAudio()
                            else viewModel.playAudio(context)
                        },
                        enabled = viewModel.recordedFilePath != null && !viewModel.isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (viewModel.isPlaying) "Stop Playing" else "Play & Sync")
                    }
                }
            }
        }

        // Synchronized Karaoke Lyrics View
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Synchronized Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.timedLyrics) { line ->
                        // Check if current playback position matches this line's timestamp window
                        val isHighlighted = viewModel.isPlaying &&
                                viewModel.currentPlaybackPosition in line.startMs..line.endMs

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}