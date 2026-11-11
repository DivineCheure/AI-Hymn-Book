package com.example.aihymnbook

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HymnDao {
    @Query("SELECT * FROM hymns ORDER BY id DESC")
    fun getAllHymns(): Flow<List<HymnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHymn(hymn: HymnEntity)

    @Delete
    suspend fun deleteHymn(hymn: HymnEntity)
}