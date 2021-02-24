package com.example.lamelameo.picturepuzzle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.lamelameo.picturepuzzle.ui.main.PuzzleActivity2Fragment

class PuzzleActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.puzzle_activity2_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, PuzzleActivity2Fragment.newInstance())
                    .commitNow()
        }
    }
}