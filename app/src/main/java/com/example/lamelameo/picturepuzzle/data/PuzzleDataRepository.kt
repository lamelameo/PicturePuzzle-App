package com.example.lamelameo.picturepuzzle.data


class PuzzleDataRepository constructor(private val bestDao: BestDataDao) {

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