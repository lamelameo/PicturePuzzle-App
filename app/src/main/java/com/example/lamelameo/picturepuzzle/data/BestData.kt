package com.example.lamelameo.picturepuzzle.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzle_best_data")
data class BestData(
    @PrimaryKey val puzzleName: String,
    @ColumnInfo(name = "moves") val moves: Int,
    @ColumnInfo(name = "time") val time: Int
    )
