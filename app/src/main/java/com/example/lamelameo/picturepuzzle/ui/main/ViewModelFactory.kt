package com.example.lamelameo.picturepuzzle.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class ViewModelFactory (private val imagePath: String?,
                        private val imageDrawable: Bitmap?,
                        private val numRows: Int,
                        private val gridViewSize: Int) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(imagePath, imageDrawable, numRows, gridViewSize) as T
        }
        throw IllegalArgumentException("ViewModel class not found.")
    }
}