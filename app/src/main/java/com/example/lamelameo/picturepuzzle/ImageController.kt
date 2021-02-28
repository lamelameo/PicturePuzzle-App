package com.example.lamelameo.picturepuzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory

class ImageController(imagePath: String) {


    /**
     * Scale an image from a file path to a specific views size, and return as a Bitmap
     * Intended to be used for photos taken with a camera intent so images are by default in landscape
     * Therefore images are also rotated 90 degrees
     *
     * @param viewSize  size of the (pixels) view that the image is intended to be placed into
     * @param photoPath file path of the image to be scaled
     * @return the scaled and rotated image as a Bitmap object
     */
    fun scalePhoto(viewSize: Int, photoPath: String): Bitmap? {
        // scale puzzle bitmap to fit the game grid view to save app memory/ prevent errors
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoPath, bmOptions)
        bmOptions.inJustDecodeBounds = false
        // This scale factor is rounded down to nearest power of 2
        bmOptions.inSampleSize = (bmOptions.outWidth / viewSize).coerceAtMost(bmOptions.outHeight / viewSize)
        return BitmapFactory.decodeFile(photoPath, bmOptions)
    }

    /**
     * create the grid of smaller cell bitmaps using the chosen image and grid size and add them to the bitmaps list
     *
     * @param bmp     bitmap image to be used to create grid of images for the puzzle
     * @param rows    number of rows to split the grid into
     * @param columns number of columns to split the grid into
     */
    fun createCellBitmaps(bmp: Bitmap, rows: Int, columns: Int) {
        // determine cell size in pixels from the image size and set amount of rows/cols
        val cellSize = bmp.width / rows
        // for each row loop 4 times creating a new cropped image from original bitmap and add to adapter dataset
        for (x in 0 until columns) {
            // for each row, increment y value to start the bitmap at
            val ypos = (x * cellSize).toFloat()
            for (y in 0 until rows) {
                // loop through 4 positions in row incrementing the x value to start bitmap at
                val xpos = (y * cellSize).toFloat()
                val gridImage = Bitmap.createBitmap(bmp, xpos.toInt(), ypos.toInt(), cellSize, cellSize)
                // converted to drawable for use of setImageDrawable to easily swap cell images
//                bitmaps.add(BitmapDrawable(getResources(), gridImage))
            }
        }
    }
}