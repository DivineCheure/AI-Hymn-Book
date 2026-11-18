package com.example.aihymnbook

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hymns")
data class HymnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val lyrics: String,
    val audioPath: String?,
    val aiArrangement: String? = null // Stores AI tunes / choir harmony styling
)