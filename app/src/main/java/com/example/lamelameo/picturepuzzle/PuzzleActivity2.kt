package com.example.lamelameo.picturepuzzle

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.lamelameo.picturepuzzle.databinding.Puzzle2ActivityBinding
import com.example.lamelameo.picturepuzzle.ui.main.MainViewModel
import java.util.ArrayList

class PuzzleActivity2 : AppCompatActivity() {

    private lateinit var mViewModel: MainViewModel
    private lateinit var mHandler: Handler
    private lateinit var mBinding: Puzzle2ActivityBinding
    private lateinit var mCells: ArrayList<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
        }
        mBinding = Puzzle2ActivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // first get relevant chosen settings from main activity
        val numRows = intent.getIntExtra("numColumns", 4)
        val puzzleNum = intent.getIntExtra("puzzleNum", 0)
        val photoPath = intent.getStringExtra("photoPath")
        mCells = ArrayList(numRows*numRows)
        mBinding.gridLayout.columnCount = numRows
        mBinding.gridLayout.rowCount = numRows

        mViewModel = MainViewModel(photoPath, numRows, this.lifecycle)
        // TODO: factory pattern VM provider
//        mViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        mViewModel.getPuzzleData().observe(this, Observer {
            // TODO: update UI when data changes... How to distinguish what changes?
            handleDataChange()
        })

        mHandler = Handler(Looper.getMainLooper())

    }

    private fun handleDataChange() {

    }

    private fun swapCellImages(index1: Int, index2: Int) {
        val image: Drawable = mCells[index1].drawable
        mCells[index1].setImageDrawable(mCells[index2].drawable)
        mCells[index2].setImageDrawable(image)
    }

}