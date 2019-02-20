package com.example.lamelameo.picturepuzzle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.*;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.myViewHolder> {
    private ArrayList<Drawable> mDataset;
    private Intent mIntent;
    private int[] mDrawableInts;
    private Context mContext;
    private int mGridRows;
    private int mSelectedImage = -1;
    private int mSelectedHolder;
    private ArrayList<myViewHolder> viewHolders;

    // constructor takes array of drawables resource integers
    ImageRecyclerAdapter(int[] drawableInts, Intent intent, Context context) {
//        mDataset = imageDataset;
        mIntent = intent;
        mDrawableInts = drawableInts;
        mContext = context;
        mGridRows = 4;
        viewHolders = new ArrayList<>();
    }

    // constructor takes arraylist of drawables
    ImageRecyclerAdapter(ArrayList<Drawable> images, Intent intent, Context context) {
        mIntent = intent;
        mDataset = images;
        mContext = context;
        mGridRows = 4;
        viewHolders = new ArrayList<>();
    }

    void setmGridRows(int mGridRows) {
        this.mGridRows = mGridRows;
    }

    int getmSelectedImage() {
        return mSelectedImage;
    }

    void resetSelection() {
        mSelectedHolder = -1;
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

    // creates holders for recycler
    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        // inflate the image previews in the recycler
        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.image_preview, parent, false);
        AppCompatImageView imageView = layout.findViewById(R.id.previewImage);
        // create view holder instances using the preview Views
        final myViewHolder vh = new myViewHolder(layout, imageView);
        viewHolders.add(vh);
        // set on click listener for imageviews to make them selectable
        vh.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = vh.getAdapterPosition();
                // clicked an unselected image - select it and remove previous selection if any
                if (vh.mPreviewLayout.getBackground() == null) {
                    vh.mPreviewLayout.setBackgroundColor(Color.CYAN);
                    // update the image selection to the position of this image
                    mSelectedImage = position;
                    int holderIndex = viewHolders.indexOf(vh);
                    // if we are changing from one selection to another - selected holder will be diff to clicked
                    if (mSelectedHolder != holderIndex && mSelectedHolder != -1) {
                        // remove background from the previously selected holder
                        viewHolders.get(mSelectedHolder).mPreviewLayout.setBackground(null);
                    }
                    // update the selected holder
                    mSelectedHolder = holderIndex;

                } else {  // clicked a selected an image - unselect it
                    vh.mPreviewLayout.setBackground(null);
                    mSelectedImage = -1;
                    mSelectedHolder = -1;
                }
            }
        });
        return vh;
    }

    // set image view to corresponding drawable in dataset
    @Override
    public void onBindViewHolder(@NonNull final myViewHolder holder, int position) {
        //TODO: a switch from one adapter for the recyclerview removes the selection,
        // if the game is started then go back to main then that puzzle becomes default

        if (mDataset != null) {
            holder.mImageView.setImageDrawable(mDataset.get(position));
        } else {
            // set the image for the item based on the position in adapter...as holders are recycled it will be set each bind
            holder.mImageView.setImageResource(mDrawableInts[position]);
        }

        // check if the holder being bound is the selected image and keep make it blue else set no background
        // this is needed when scrolling occurs and holders get shuffled
        if (mSelectedImage == position) {
            holder.mPreviewLayout.setBackgroundColor(Color.CYAN);
        } else {
            holder.mPreviewLayout.setBackground(null);
        }
    }

    @Override
    public int getItemCount() {
        if (mDataset != null) {
            return mDataset.size();
        } else {
            return mDrawableInts.length;
        }
    }
}
