package com.example.lamelameo.picturepuzzle.ui.main

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.lamelameo.picturepuzzle.R

class PuzzleActivity2Fragment : androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance() = PuzzleActivity2Fragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var mHandler: Handler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
        mHandler = Handler(Looper.getMainLooper())
    }

}