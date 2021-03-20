package com.example.lamelameo.picturepuzzle.unused;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.view.GestureDetectorCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Random;


public class ImageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Drawable> mBitmaps;
    private int mGridWidth;
    private boolean isShown;
    private String TAG = "ImageAdapter";
    private ArrayList<ArrayList<ImageView>> cellRows, cellCols;
    private ArrayList<ImageView> gridCells;
    private int gridRows;
    private ArrayList<Integer> positionPool;
    private Random rng;
    private int bounds;
    private View.OnTouchListener swipeListener;

    public ArrayList<ImageView> getRow(int index) {
        return cellRows.get(index);
    }

    public ArrayList<ImageView> getCol(int index) {
        return cellCols.get(index);
    }

    public ImageAdapter(Context c, ArrayList<Drawable> bitmaps, int gridWidth, View.OnTouchListener onTouchListener) {
        mContext = c;
        mBitmaps = bitmaps;
        mGridWidth = gridWidth;
        swipeListener = onTouchListener;
        gridRows = (int)Math.sqrt(bitmaps.size());
        // initialise objects for randomisation of images
        rng = new Random();
        bounds = gridRows*gridRows-1;
        positionPool = new ArrayList<>();
        for (int x=0; x<gridRows*gridRows; x++) {
            positionPool.add(x);
        }

        //TODO: if allow for m x n grid size then have to change stuff here

        // initialise lists to hold grid objects
        gridCells = new ArrayList<ImageView>();
        cellRows = new ArrayList<>();
        cellCols =  new ArrayList<>();
        for (int x=0; x<gridRows; x++) {
            cellRows.add(new ArrayList<ImageView>());
            cellCols.add(new ArrayList<ImageView>());
        }

    }

    public int getGridRows() {
        return gridRows;
    }

    public int getCount() {
        return mBitmaps.size();
    }

    public Object getItem(int position) {
        return gridCells.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public int getItemViewType(int position) {
        return 0;
    }

    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        Log.i(TAG, "getViewTest: ");
        final ImageView imageView;
//        int rngBitmapIndex;
//        final LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();

        if(convertView == null) {
            Log.i(TAG, "convertView=null: "+position);
//            View item = inflater.inflate(R.layout.puzzle_view, parent, false);
            imageView = new ImageView(mContext);
            convertView = imageView;

            // set cell size using params
            //TODO: divide based on what grid size is selected
            int size = mGridWidth/gridRows;  // cell size in px based on grid size which may be scaled down
//            Log.i("cellsize", "getView: "+size);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));

            //TODO: an extra call to getview where convertview=null for position = 0 which causes a bug where an empty
            // cell to appears in pos 7 (8thcell) after moving empty from it to one above (4thcell)
            // probably due to the extra empty imageview added to row/col lists which had no tag so set neighbour cell empty

            // used to stop extra call to getView where convertView=null to add an extra imageview to row/col and
            // prevent error with setting imageviews drawable (bounds=0 gives an error)
            if (position == 0 && bounds == 0) {
                Log.i(TAG, "pos0 bound0 ");
                return imageView;
            }

            // add imageviews to row and column lists for access
            //TODO: divide/multiply by amount of cols/rows
            int cellRow = (int)Math.floor(position/(float)gridRows);
            int cellCol = position - cellRow*gridRows;

            gridCells.add(imageView);
            cellRows.get(cellRow).add(imageView);
            cellCols.get(cellCol).add(imageView);

            // setting images and tags for cells
            if (position == mBitmaps.size() - 1) {  // leave last cell with no image
                imageView.setTag(position);

            // set all other cells with a randomised image excluding the last cells image as it must be empty
            } else {  //TODO: HAVE TO MAKE A NEW RANDOMISE FUNCTION AS THIS PRODUCES UNSOLVABLE PUZZLES
                // get random number from a pool of ints: zero - number of cells-1, and set cell image as the bitmap at this index
                int randIndex = rng.nextInt(bounds);  // gets a randomised number within the pools bounds
//                Log.i(TAG, "randomNum " + randIndex);
                int rngBitmapIndex = positionPool.get(randIndex); // get the bitmap index from the pool using the randomised number
//                Log.i(TAG, "randomIndex " + rngBitmapIndex);
                positionPool.remove((Integer) rngBitmapIndex);  // remove used number from the pool - use Integer else it takes as Arrayindex
//                Log.i(TAG, "randomPool " + positionPool);
                bounds -= 1;  // lower the bounds by 1 to match the new pool size so the next cycle can function properly
//                Log.i(TAG, "randomBounds " + bounds);

                // set the cells starting image
                imageView.setImageDrawable(mBitmaps.get(rngBitmapIndex));
                //set cell tags corresponding to the set image for tracking/identification purposes
                imageView.setTag(rngBitmapIndex);

            }

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick:");

                }
            });
            imageView.setOnTouchListener(swipeListener);
            //TODO: changed to onitemclicklistener implemented in the activity instead due to problems with 1st cell clicks

        } else {
            Log.i(TAG, "convertView=NOTNULL");
            imageView = (ImageView) convertView;
        }

        return imageView;
    }

    private Integer[] mImageIds = {
            // get image from default pics or take a photo and create grid of smaller images
            //TODO: set onclicklistener for each item which changes the set image
    };

}
