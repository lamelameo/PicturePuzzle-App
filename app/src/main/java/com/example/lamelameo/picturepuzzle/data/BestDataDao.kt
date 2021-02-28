package com.example.lamelameo.picturepuzzle.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BestDataDao {
    // TODO: data for each image
    private val bestsList = mutableListOf<BestData>()
    private val bests = MutableLiveData<List<BestData>>()

    init {
        bests.value = bestsList
    }

    fun addBest(best: BestData) {
        bestsList.add(best)
        bests.value = bestsList
    }

    fun getBest() = bests as LiveData<List<BestData>>

}