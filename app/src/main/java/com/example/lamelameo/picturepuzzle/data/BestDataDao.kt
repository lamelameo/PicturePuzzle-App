package com.example.lamelameo.picturepuzzle.data

import androidx.room.*

@Dao
interface BestDataDao {
    @Query("SELECT * FROM puzzle_best_data")
    suspend fun getAll(): List<BestData>?

    @Query("SELECT * FROM puzzle_best_data WHERE puzzleName IN (:names)")
    suspend fun loadAllBy(names: Array<String>): List<BestData>?

    @Query("SELECT * FROM puzzle_best_data WHERE puzzleName is :puzzleName")
    suspend fun findByPuzzle(puzzleName: String): BestData?

    @Insert
    suspend fun newEntry(entry: BestData)

    @Update
    suspend fun updateEntry(entry: BestData)

    @Delete
    suspend fun delete(entry: BestData)

}