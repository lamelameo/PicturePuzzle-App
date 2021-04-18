package com.example.lamelameo.picturepuzzle.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.example.lamelameo.picturepuzzle.R
import com.squareup.picasso.Picasso
import java.io.File

class ImageRecyclerAdapter2(private val mDrawableInts: List<Int>?,
                            private val mPhotoPaths: MutableList<String>?,
                            private val mRecyclerView: RecyclerView) :
    RecyclerView.Adapter<ImageRecyclerAdapter2.MyViewHolder>() {

    private val TAG = "ImageRecyclerAdapter2"
    //TODO: selected image is not saved on screen rotation
    private var selectedImage: Int = -1

    class MyViewHolder(val mPreviewLayout: ConstraintLayout, val mImageView: AppCompatImageView) :
        RecyclerView.ViewHolder(mPreviewLayout)

    // return the selection depending on which dataset is displayed in recycler view
    fun getSelection(): Int {
        return selectedImage
    }

    // clears any viewholders which may have been previously selected but were unselected while out of view of UI
    override fun onViewAttachedToWindow(holder: MyViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder.mPreviewLayout.tag != selectedImage) holder.mPreviewLayout.background = null
    }

    private val viewHolderListener = View.OnClickListener { v ->
        // TODO: if we dont use adapter to get selected image it always uses default instance variable
        // clear the previous selection graphic (if any) before checking for new selection
        val adapter = mRecyclerView.adapter as ImageRecyclerAdapter2
        mRecyclerView.children.forEach { it.background = null }
        // clicked selected
        if (v.tag == adapter.selectedImage) {
            adapter.selectedImage = -1
        } else {  // clicked other
            adapter.selectedImage = v.tag as Int
            v.setBackgroundColor(Color.CYAN)
        }
    }

    // creates holders for recycler
    override fun onCreateViewHolder(parent: ViewGroup, i: Int): MyViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.image_preview, parent, false) as ConstraintLayout
        layout.setOnClickListener(viewHolderListener)
        return MyViewHolder(layout, layout.findViewById(R.id.previewImage))
    }

    // set image view to corresponding drawable in dataset
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // set image and tag corresponding to the set image for the current holder
        mPhotoPaths?.let {
            val size = holder.mImageView.layoutParams.width
//            holder.mImageView.setImageBitmap(scalePhoto(holder.mImageView.layoutParams.width, it[position]))
            Picasso.get().load(File(it[position])).placeholder(android.R.drawable.ic_menu_gallery).resize(size, size)
                .into(holder.mImageView)
        }
            ?: mDrawableInts?.let {
//                holder.mImageView.setImageResource(it[position])
                Picasso.get().load(it[position]).into(holder.mImageView)
            }

        holder.mPreviewLayout.tag = position
        // check if the holders image is selected and set its background accordingly
        if (selectedImage == position) {
            holder.mPreviewLayout.setBackgroundColor(Color.CYAN)
        } else {  // non selected images
            holder.mPreviewLayout.background = null
        }
    }

    override fun getItemCount(): Int {
        return mPhotoPaths?.size ?: mDrawableInts!!.size
    }

    fun addPhotos(newPaths: List<String>) {
        val position: Int = itemCount
        newPaths.forEach { mPhotoPaths?.add(it) }
        notifyItemRangeInserted(position, newPaths.size)
    }

    /**
     * Scale an image from a file path to a specific views size, and return as a Bitmap
     * Intended to be used for photos taken with a camera intent so images are by default in landscape
     * Therefore images are also rotated 90 degrees
     *
     * @param viewSize  size of the (pixels) view that the image is intended to be placed into
     * @param photoPath file path of the image to be scaled
     * @return the scaled and rotated image as a Bitmap object
     */
    private fun scalePhoto(viewSize: Int, photoPath: String): Bitmap {
        // scale puzzle bitmap to fit the game grid view to save app memory/ prevent errors
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoPath, bmOptions)
        bmOptions.inJustDecodeBounds = false
        // This scale factor is rounded down to nearest power of 2
        bmOptions.inSampleSize = (bmOptions.outWidth / viewSize).coerceAtMost(bmOptions.outHeight / viewSize)
        return BitmapFactory.decodeFile(photoPath, bmOptions)
    }

}