package com.example.lamelameo.picturepuzzle.data

import android.util.Log
import java.io.File

class PuzzleDataRepository constructor(private val bestDao: BestDataDao) {

    fun retrievePhotoPaths(picDirectory: File?): MutableList<String> {
        // TODO: cap number of photos possible? or load in batches if too many
        val paths = mutableListOf<String>()
        picDirectory?.listFiles()
            ?.forEach { file ->
                if (file.name.endsWith(".jpg") || file.name.endsWith(".png") && file.length() > 0)
                    paths.add(file.absolutePath) else file.delete()
            }
        return paths
    }

    suspend fun addBest(data: BestData) {
        bestDao.newEntry(data)
    }

    suspend fun addBests(list: List<BestData>) {
        list.forEach { bestData -> bestDao.newEntry(bestData) }
    }

    suspend fun getBest(puzzleName: String) = bestDao.findByPuzzle(puzzleName)

    suspend fun update(data: BestData) {
        bestDao.updateEntry(data)
    }

    suspend fun delete(data: BestData) {
        bestDao.delete(data)
    }

}