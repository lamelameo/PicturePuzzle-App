package com.example.lamelameo.picturepuzzle.ui.main

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.children
import com.example.lamelameo.picturepuzzle.PuzzleActivity
import com.example.lamelameo.picturepuzzle.databinding.ActivityPhotoCroppingBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class PhotoActivity : AppCompatActivity() {

    private val TAG = "PhotoCropping_debug"
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_SELECT = 0
    private val REQUEST_PHOTO_CROP = 2
    private lateinit var mBinding: ActivityPhotoCroppingBinding
    private var mCurrentPhotoPath: String? = null
    private lateinit var cropView: ImageView
    private var mUri: Uri? = null
    private var mImage: Bitmap? = null
    private var mSaved = false
    private var savedPhotos: ArrayList<String>? = ArrayList<String>()
    private var mGridRows = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPhotoCroppingBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
//        setSupportActionBar(findViewById(R.id.toolbar))
//        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }

        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getCharSequence("photoPath") as String
            savedPhotos = savedInstanceState.getStringArrayList("savedPhotos")
        }

        // Buttons
        cropView = mBinding.cropView
        mImage = mCurrentPhotoPath?.let { BitmapFactory.decodeFile(it) }.also { cropView.setImageBitmap(it) }
        mBinding.rotateRight.setOnClickListener {
            mImage = mImage?.let { rotatePhoto(90f, it) }.also { cropView.setImageBitmap(it) }
        }
        mBinding.rotateLeft.setOnClickListener {
            mImage = mImage?.let { rotatePhoto(270f, it) }.also { cropView.setImageBitmap(it) }
        }

        // start game button onclicklistener
        val gameIntent = Intent(this, PuzzleActivity2::class.java)
        mBinding.startGame.setOnClickListener {
            mImage?.let {
                if (!mSaved) { savePhoto(it) }
                gameIntent.putExtra("photoPath", mCurrentPhotoPath)
                gameIntent.putExtra("numColumns", mGridRows)
                startActivity(gameIntent)
            } ?: Toast.makeText(applicationContext, "Provide a photo from Camera or Gallery.", Toast.LENGTH_LONG).show()

        }

        // send intent to take photo using camera on button click
        mBinding.takePhoto.setOnClickListener { dispatchTakePictureIntent() }
        // open gallery picker on gallery button click
        mBinding.galleryButton.setOnClickListener {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI).let {
                startGalleryActivity.launch(it)
//                startActivityForResult(it, REQUEST_GALLERY_SELECT)
            }
        }

        mBinding.saveButton.setOnClickListener {
            mImage?.let {
                if (mSaved) {
                    Toast.makeText(applicationContext, "Image Saved Already.", Toast.LENGTH_SHORT).show()
                } else {
                    savePhoto(it)
                    Toast.makeText(applicationContext, "Image Saved.", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(applicationContext, "No image to save.", Toast.LENGTH_SHORT).show()
        }

        // set gridsize based on the checked radio button, this value will be used as an intent extra when starting the game
        mBinding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            group.children.forEachIndexed { i, child ->  if (child.id == checkedId) mGridRows = i + 3 }
        }

    }

    override fun onBackPressed() {
        // send list of paths of newly saved photos to main activity so the recycler can be updated.
        val intent = Intent()
        intent.putStringArrayListExtra("savedPhotos", savedPhotos)
        if (savedPhotos!!.size != 0) {
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    /**
     * Save the full sized image taken from camera to an app private directory that is deleted if app is removed
     *
     * @return a File to store a photo taken with the camera intent
     * throws IOException error when making File
     */
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = timeStamp + "_"
        val image = File.createTempFile(imageFileName, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    /**
     * Rotates the given photo by @direction degrees of clockwise rotation and saves the new image over the old one
     *
     * @param degrees intended value of either 90 or 270, determines if the photo rotates right or left, respectively
     */
    private fun rotatePhoto(degrees: Float, img: Bitmap): Bitmap? {
        // rotate image to correct orientation
        mSaved = false
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    /**
     * Given a Bitmap, this function will save the image into an app specific image folder for future use
     *
     * @param image An input Bitmap image
     */
    private fun savePhoto(image: Bitmap) {
        //TODO: check device storage before saving
        (mCurrentPhotoPath?.let { File(it) } ?: createImageFile())?.also {
            try {
                FileOutputStream(it).use { fileOutputStream ->
                    image.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            // Variable will update Recycler View in MainActivity with new image if user navigates back there
            mCurrentPhotoPath?.let { path -> savedPhotos?.add(path) }
            mSaved = true
        }
    }

    private val startCameraActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "got camera photo back")
                    // got a photo from camera intent
                    retrievePhoto()?.let { rotatePhoto(90f, it) }?.also { savePhoto(it) }
                    mUri?.let { dispatchCropIntent(it, it) }
                }
                Activity.RESULT_CANCELED -> {
                    deleteEmptyFile()
                }
            }
        }

    private val startGalleryActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "got camera photo back")
                    // get the URI for the photo selected from gallery and send it to the android photo editor to crop
                    mSaved = false
                    mUri = result.data?.data?.also { dispatchCropIntent(it, null) }
                }
                Activity.RESULT_CANCELED -> {
                    deleteEmptyFile()
                }
            }
        }

    private val startCropActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "got camera photo back")
                    // output URI is saved in the intent data, see link below
                    // https://android.googlesource.com/platform/packages/apps/Gallery2/+/c9f743a/src/com/android/gallery3d/app/CropImage.java
                    mUri = result.data?.data?.also {
                        try {
                            mImage = BitmapFactory.decodeStream(contentResolver.openInputStream(it))?.also { bmp ->
                                cropView.setImageBitmap(bmp)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                Activity.RESULT_CANCELED -> {
                    deleteEmptyFile()
                    // retrieve any image from camera or gallery if crop fails
                    mImage = retrievePhoto()
                    cropView.setImageBitmap(mImage)
                }
            }
        }

    private fun deleteEmptyFile() {
        mCurrentPhotoPath?.let {
            val photo = File(mCurrentPhotoPath)
            if (photo.length() == 0L) {
                val deleted = photo.delete()
                Log.i(TAG, "File Deleted: $deleted")
            }
        }
    }

    /**
     * Retrieve camera or gallery image data from mImage after a failed crop intent and auto crop to size.
     */
    private fun retrievePhoto(): Bitmap? {
        var bitmap: Bitmap? = null
        mUri?. let {
            try { bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it)) }
            catch (e: IOException) { e.printStackTrace() }
        }

        return bitmap?.let {
            val bmpH = it.height
            val bmpW = it.width
            val matrix = Matrix()
            val scale = 1000f / max(bmpW, bmpH)
            matrix.postScale(scale, scale)
            Bitmap.createBitmap(it, 0, 0, bmpW, bmpH, matrix, true)
        }
    }

    /**
     * Creates and invokes an Intent to take a photo using the camera
     */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).let { intent ->
            if (intent.resolveActivity(packageManager) != null) {
                try {
                    createImageFile()?.let {
                        mUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", it)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
                        startCameraActivity.launch(intent)
//                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                    }
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()
                }

            }
        }

    }

    /**
     * Try to send a crop intent using the given input image Uri and output Uri and display a toast if it fails.
     * @param input Uri of image to be cropped
     * @param output Uri for cropped image to be saved into
     */
    private fun dispatchCropIntent(input: Uri, output: Uri?) {
//        Log.i(TAG, "dispatchCropIntent: ");
        val intent = Intent("com.android.camera.action.CROP")
        val outUri: Uri? = output ?: run {
            try {
                createImageFile()?.let { Uri.fromFile(it) }
            } catch (e: IOException) {
                e.printStackTrace(); null
            }
        }

        try {
            intent.setDataAndType(input, "image/*")
            // must flag both read and write permissions or will get security error
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri) // output file uri
            // output smaller photo
            intent.putExtra("outputX", 1000)
            intent.putExtra("outputY", 1000)
            // set aspect ratio to 1:1 for a square image
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("scale", true)
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
            intent.putExtra("noFaceDetection", true)
            startCropActivity.launch(intent)
//            startActivityForResult(intent, REQUEST_PHOTO_CROP)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(applicationContext, "Device unable to run crop intent", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

}