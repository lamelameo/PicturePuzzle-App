package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.myViewHolder> {

    private static final String TAG = "ImageRecyclerAdapter";
    private ArrayList<Drawable> mDrawablePhotos;
    private int[] mDrawableInts;
    private Context mContext;
    private int mGridRows;
    private static int mSelectedImage = -1;
    private static int mSelectedPhoto = -1;
    private static boolean isDefaultImages = true;
    private static ArrayList<myViewHolder> recyclerViews = new ArrayList<>();

    // constructor takes array of drawables resource integers (default images)
    ImageRecyclerAdapter(int[] drawableInts, Context context) {
        mDrawableInts = drawableInts;
        mContext = context;
        mGridRows = 4;
        Log.i(TAG, "created default adapter");
    }

    // constructor takes arraylist of drawables (photos)
    ImageRecyclerAdapter(ArrayList<Drawable> images, Context context) {
        mDrawablePhotos = images;
        mContext = context;
        mGridRows = 4;
        Log.i(TAG, "created photo adapter");
    }

    void setmGridRows(int mGridRows) {
        this.mGridRows = mGridRows;
    }

    // return the selection depending on which dataset is displayed in recycler view
    int getSelection() {
        if (isDefaultImages) {
            return mSelectedImage;
        } else {
            return mSelectedPhoto;
        }
    }

    void setIsDefaultImages() {
        isDefaultImages = !isDefaultImages;
        Log.i(TAG, "setIsDefaultImages: "+isDefaultImages);
    }

    void resetSelection() {
        mSelectedImage = -1;
    }

    static class myViewHolder extends RecyclerView.ViewHolder {
        private AppCompatImageView mImageView;
        private ConstraintLayout mPreviewLayout;
        private myViewHolder(ConstraintLayout constraintLayout, AppCompatImageView imageView) {
            super(constraintLayout);
            mImageView = imageView;
            mPreviewLayout = constraintLayout;
        }
    }

    //TODO: MORE SELECTION BUGS...problem is switching adapters
    // scroll through all defaults selecting each till last...switch to photos and do same, at some point the adapter will
    // create a new viewholder and two selections will happen as the previous selection is not cleared.
    // FIX: list of viewholders was not same for each clicked view, made the list static, so same list is present no matter
    // what and both adapters should use same holders and fill up same list when new one is created...

    private View.OnClickListener recyclerViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // clear the previous selection graphic (if any) before checking for new selection
            for (myViewHolder view: recyclerViews) {
                int[] vhTag = (int[])view.mImageView.getTag();
                // clear only the selected view for the active dataset (defaults or photos)
                if ((isDefaultImages && vhTag[0] == mSelectedImage) || (!isDefaultImages && vhTag[1] == mSelectedPhoto)) {
                    view.mPreviewLayout.setBackground(null);
                }
            }
            int[] viewTag = (int[])v.getTag();  // keeps track of the displayed image/photo
            int viewImageIndex;
            ConstraintLayout parentView = (ConstraintLayout)v.getParent();  // the view to be coloured on selection
            // process clicks for only the displayed dataset, while keeping track of selections for both,
            // boolean isDefaultImages keeps track of displayed dataset, updated from main activity
            if (isDefaultImages) {  // process clicks for default images
                viewImageIndex = viewTag[0];
                if (mSelectedImage != viewImageIndex) {  // clicked a new selection
                    parentView.setBackgroundColor(Color.CYAN);
                    mSelectedImage = viewImageIndex;  // update selection tracker variable for next click
                } else {  // clicked selected image, already updated graphic
                    mSelectedImage = -1;  // update tracker
                }
            } else {  // process clicks for photos, same as for images but with separate variable
                viewImageIndex = viewTag[1];
                if (mSelectedPhoto != viewImageIndex) {
                    parentView.setBackgroundColor(Color.CYAN);
                    mSelectedPhoto = viewImageIndex;
                } else {
                    mSelectedPhoto = -1;
                }
            }
        }
    };

    // creates holders for recycler
    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        Log.i(TAG, "onCreateViewHolder: ");
        // inflate the image previews in the recycler
        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(
                                   R.layout.image_preview, parent, false);
        final AppCompatImageView imageView = layout.findViewById(R.id.previewImage);
        // set click listeners for the images only, and initialise tags to track the image present in each holder upon
        // clicking the ImageView, in order to handle image selection (graphic and data)
        imageView.setOnClickListener(recyclerViewListener);
        if (imageView.getTag() == null) {
            int[] tag = {-1,-1};
            imageView.setTag(tag);
        }
        // create view holder instances using the preview Views
        final myViewHolder vh = new myViewHolder(layout, imageView);
        recyclerViews.add(vh);
        return vh;
    }

    // set image view to corresponding drawable in dataset
    @Override
    public void onBindViewHolder(@NonNull final myViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder: ");
        // set graphic and tag corresponding to the set image for the current holder
        int[] currentTag = (int[])holder.mImageView.getTag();
        // set the image for the item based on the position in adapter...as holders are recycled it will be set each bind
        if (mDrawablePhotos != null) {  // instance displaying app photos will have an object for mDrawablePhotos
            holder.mImageView.setImageDrawable(mDrawablePhotos.get(position));  // set the correct image for that position
            currentTag[1] = position;
            holder.mImageView.setTag(currentTag);  // set a tag for tracking the current image set, as holders are recycled
        } else {  // instance using default images, mDrawablePhoto will be null
            holder.mImageView.setImageResource(mDrawableInts[position]);
            currentTag[0] = position;
            holder.mImageView.setTag(currentTag);
        }

        // check if the holder being bound is selected and give cyan background else set no background
        // this is needed as when scrolling occurs, holders get shuffled
        if (isDefaultImages) {  // displaying default images
            if (mSelectedImage == position) {  // selected image
                holder.mPreviewLayout.setBackgroundColor(Color.CYAN);
            } else {  // non selected images
                holder.mPreviewLayout.setBackground(null);
            }
        } else {  // displaying app photos
            if (mSelectedPhoto == position) {
                holder.mPreviewLayout.setBackgroundColor(Color.CYAN);
            } else {
                holder.mPreviewLayout.setBackground(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mDrawablePhotos != null) {
            return mDrawablePhotos.size();
        } else {
            return mDrawableInts.length;
        }
    }
}
