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
    private ArrayList<Drawable> mDrawablePhotos;
    private Intent mIntent;
    private int[] mDrawableInts;
    private Context mContext;
    private int mGridRows;
    private static int mSelectedImage = -1;
    private static int mSelectedPhoto = -1;
    private static boolean isDefaultImages = true;
    private String TAG = "ImageRecyclerAdapter";
    private ArrayList<ConstraintLayout> recyclerViews = new ArrayList<>();

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

    private View.OnClickListener recyclerViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "mSelectedImage: "+mSelectedImage);
            // clear all views to remove potential previous selection before checking for new selection
            for (ConstraintLayout view: recyclerViews) {
                //TODO: just uncolour selected holder?
                view.setBackground(null);
            }

            int viewImageIndex = (int)v.getTag();  // keeps track of the displayed image/photo
            ConstraintLayout parentView = (ConstraintLayout)v.getParent();  // the view to be coloured on selection
            // process clicks for only the displayed dataset, while keeping track of selections for both,
            // boolean isDefaultImages keeps track of displayed dataset, updated from main activity
            if (isDefaultImages) {  // process clicks for default images
                if (mSelectedImage != viewImageIndex) {  // clicked a new selection
                    parentView.setBackgroundColor(Color.CYAN);
                    mSelectedImage = viewImageIndex;  // update selection tracker variable for next click
                } else {  // clicked selected image, already updated graphic
                    mSelectedImage = -1;  // update tracker
                }
            } else {  // process clicks for photos, same as for images but with separate variable
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
        imageView.setOnClickListener(recyclerViewListener);
        recyclerViews.add(layout);
        // create view holder instances using the preview Views
        final myViewHolder vh = new myViewHolder(layout, imageView);
        return vh;
    }

    // set image view to corresponding drawable in dataset
    @Override
    public void onBindViewHolder(@NonNull final myViewHolder holder, int position) {
        // set the image for the item based on the position in adapter...as holders are recycled it will be set each bind
        if (mDrawablePhotos != null) {  // instance displaying app photos will have an object for mDrawablePhotos
            Log.i(TAG, "onBindViewHolder: photos");
            holder.mImageView.setImageDrawable(mDrawablePhotos.get(position));  // set the correct image for that position
            holder.mImageView.setTag(position);  // set a tag for tracking the current image set, as holders are recycled
        } else {  // instance using default images, mDrawablePhoto will be null
            Log.i(TAG, "onBindViewHolder: defaults");
            holder.mImageView.setImageResource(mDrawableInts[position]);
            holder.mImageView.setTag(position);
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
