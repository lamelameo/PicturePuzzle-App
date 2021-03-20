package com.example.lamelameo.picturepuzzle.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lamelameo.picturepuzzle.PhotoCropping
import com.example.lamelameo.picturepuzzle.R
import com.example.lamelameo.picturepuzzle.data.BestsDatabase
import com.example.lamelameo.picturepuzzle.data.ImageRecyclerAdapter2
import com.example.lamelameo.picturepuzzle.data.PuzzleDataRepository
import com.example.lamelameo.picturepuzzle.databinding.ActivityMainBinding
import java.io.File

class MainActivity2: AppCompatActivity() {

    private val TAG = "MainActivity2"
    private lateinit var mRecyclerView: RecyclerView
    private var mGridRows = 4
    private var defaultAdapterActive = true
    private lateinit var mPhotoAdapter: ImageRecyclerAdapter2
    private val REQUEST_PHOTO_CROPPING = 1
    private lateinit var mBinding: ActivityMainBinding
    private val drawableInts = listOf(
        R.drawable.dfdfdefaultgrid, R.drawable.dfdfcarpet, R.drawable.dfdfcat, R.drawable.dfdfclock,
        R.drawable.dfdfdarklights, R.drawable.dfdfnendou, R.drawable.dfdfrazer, R.drawable.dfdfsaiki
    )
    private val defaultPuzzles = intArrayOf(R.drawable.grid9, R.drawable.grid15, R.drawable.grid25, R.drawable.grid36)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // set stuff
        mRecyclerView = mBinding.pictureRecyclerView
        val defaultAdapter = ImageRecyclerAdapter2(drawableInts, null, mRecyclerView)
        val orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
        mRecyclerView.apply { this.setHasFixedSize(true); this.adapter = defaultAdapter
            this.layoutManager = LinearLayoutManager(context, orientation, false)
        }
        // toggle recycler view between default images and photos taken and saved using this app
        val photos = PuzzleDataRepository(BestsDatabase.getInstance(applicationContext).bestDao).retrievePhotoPaths(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pictures")
        )
        mPhotoAdapter = ImageRecyclerAdapter2(null, photos, mRecyclerView)
        mBinding.adapterButton.setOnCheckedChangeListener { _, isChecked ->
            Log.i(TAG, "Selections: photo - ${mPhotoAdapter.getSelection()}, default - ${defaultAdapter.getSelection()}")
            defaultAdapterActive = !defaultAdapterActive
            if (isChecked) {
                mRecyclerView.swapAdapter(mPhotoAdapter, true)
            } else {
                mRecyclerView.swapAdapter(defaultAdapter, true)
            }
        }

        // move to cropper activity to crop gallery images or take photo with camera
        mBinding.photoCropButton.setOnClickListener {
            startActivityForResult(Intent(this, PhotoCropping::class.java), REQUEST_PHOTO_CROPPING)
        }

        // set gridsize based on the checked radio button, this value will be used as an intent extra when starting the game
        mBinding.setGrid.setOnCheckedChangeListener { group, checkedId ->
            group.children.forEachIndexed { i, button -> if ((button as RadioButton).id == checkedId) {
                    mGridRows = i + 3
                    return@forEachIndexed
                }
            }
        }

        // on click listener for the load button creates an intent to start the game activity and sets extras to give
        // that activity the information of grid size and image to use
        mBinding.loadButton.setOnClickListener {
            val gameIntent = Intent(this, PuzzleActivity2::class.java)
            // remove any previous extra so the game activity does not use it instead of the intended image/photo
            gameIntent.extras?.clear()
            gameIntent.putExtra("numColumns", mGridRows)
            // get drawable id or photo path for selected image from recycler view adapter
            val selectedImage = if (defaultAdapterActive) defaultAdapter.getSelection() else mPhotoAdapter.getSelection()
            // if no selection, check for selected grid size to send the appropriate default image
            if (selectedImage == -1) {
                gameIntent.putExtra("drawableId", defaultPuzzles[mGridRows - 3])
            } else {  // there is a selected item from whichever dataset is displayed
                if (defaultAdapterActive) {
                    gameIntent.putExtra("drawableId", drawableInts[selectedImage])
                } else {
                    gameIntent.putExtra("photoPath", photos[selectedImage])
                }
            }
            startActivity(gameIntent)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //TODO: start photocropping with code then when press back set photo data as list of paths then update here
        //  also consider case where cropper goes to game, then back to here directly from solved UI
        if (requestCode == REQUEST_PHOTO_CROPPING && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra("savedPhotos")?.forEach { path -> mPhotoAdapter.addPhoto(path) }
        }
    }
}