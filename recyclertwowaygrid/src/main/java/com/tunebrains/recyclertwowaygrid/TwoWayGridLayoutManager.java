package com.tunebrains.recyclertwowaygrid;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by alex on 2/16/15.
 */
public class TwoWayGridLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = TwoWayGridLayoutManager.class.getName();
    private static final int DEFAULT_COLUMN_COUNT = 1;
    private int mTotalColumnCount = DEFAULT_COLUMN_COUNT;
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;
    private int mFirstVisiblePosition;
    private int mVisibleColumnCount;
    private int mVisibleRowCount;

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }


        View scrap = recycler.getViewForPosition(0);
        addView(scrap);
        measureChildWithMargins(scrap, 0, 0);
        mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
        mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
        updateWindowSizing();


        int childLeft;
        int childTop;
        mFirstVisiblePosition = 0;
        childLeft = childTop = 0;

        if (getChildCount() == 0) {
            mFirstVisiblePosition = 0;
            childLeft = childTop = 0;
        } else {

            if (getVisibleChildCount() >= getItemCount()) {
                mFirstVisiblePosition = 0;
                childLeft = childTop = 0;
            } else {
                final View topChild = getChildAt(0);
                childLeft = getDecoratedLeft(topChild);
                childTop = getDecoratedTop(topChild);

            }
        }


        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);
        //Fill the grid for the initial layout of views
        fillGrid(childLeft, childTop, recycler);
    }

    private void fillGrid(int pChildLeft, int pChildTop, RecyclerView.Recycler pRecycler) {
        if (mFirstVisiblePosition < 0) mFirstVisiblePosition = 0;
        if (mFirstVisiblePosition >= getItemCount()) mFirstVisiblePosition = (getItemCount() - 1);
        SparseArray<View> viewCache = new SparseArray<View>(getChildCount());
        int startLeftOffset = getPaddingLeft();
        int startTopOffset = getPaddingTop();
        if (getChildCount() != 0) {
            final View topView = getChildAt(0);
            startLeftOffset = getDecoratedLeft(topView);
            startTopOffset = getDecoratedTop(topView);

            //Cache all views by their existing position, before updating counts
            for (int i = 0; i < getChildCount(); i++) {
                int position = positionOfIndex(i);
                final View child = getChildAt(i);
                viewCache.put(position, child);
            }
            //Temporarily detach all views.
            // Views we still need will be added back at the proper index.
            for (int i = 0; i < viewCache.size(); i++) {
                detachView(viewCache.valueAt(i));
            }
        }
        int leftOffset = startLeftOffset;
        int topOffset = startTopOffset;
        for (int i = 0; i < getVisibleChildCount(); i++) {
            int nextPosition = positionOfIndex(i);
            int offsetPositionDelta = 0;
            if (nextPosition < 0 || nextPosition >= getItemCount()) {
                //Item space beyond the data set, don't attempt to add a view
                continue;
            }
            //Layout this position
            View view = viewCache.get(nextPosition);
            if (view == null) {
                view = pRecycler.getViewForPosition(nextPosition);
                addView(view);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                lp.row = getGlobalRowOfPosition(nextPosition);
                lp.column = getGlobalColumnOfPosition(nextPosition);
                measureChildWithMargins(view, 0, 0);
                layoutDecorated(view, leftOffset, topOffset,
                        leftOffset + mDecoratedChildWidth,
                        topOffset + mDecoratedChildHeight);
            } else {
                attachView(view);
                viewCache.remove(nextPosition);
            }
            leftOffset += mDecoratedChildWidth;

        }
        for (int i = 0; i < viewCache.size(); i++) {
            final View removingView = viewCache.valueAt(i);
            pRecycler.recycleView(removingView);
        }


    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        if (position >= getItemCount()) {
            Log.e(TAG, "Cannot scroll to " + position + ", item count is " + getItemCount());
            return;
        }

        //Set requested position as first visible
        mFirstVisiblePosition = position;
        //Trigger a new view layout
        requestLayout();
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    private int getGlobalColumnOfPosition(int pNextPosition) {
        return pNextPosition % mTotalColumnCount;
    }

    private int getGlobalRowOfPosition(int pNextPosition) {
        return pNextPosition / mTotalColumnCount;
    }

    private int getVisibleChildCount() {
        return mVisibleColumnCount * mVisibleRowCount;
    }

    private int positionOfIndex(int pIndex) {
        int row = pIndex / mVisibleColumnCount;
        int column = pIndex % mVisibleColumnCount;

        return mFirstVisiblePosition + (row * getTotalColumnCount()) + column;
    }

    private int getTotalColumnCount() {
        if (getItemCount() < mTotalColumnCount) {
            return getItemCount();
        }

        return mTotalColumnCount;
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private void updateWindowSizing() {
        mVisibleColumnCount = (getHorizontalSpace() / mDecoratedChildWidth) + 1;
        if (getHorizontalSpace() % mDecoratedChildWidth > 0) {
            mVisibleColumnCount++;
        }

        //Allow minimum value for small data sets
        if (mVisibleColumnCount > getTotalColumnCount()) {
            mVisibleColumnCount = getTotalColumnCount();
        }


        mVisibleRowCount = (getVerticalSpace() / mDecoratedChildHeight) + 1;
        if (getVerticalSpace() % mDecoratedChildHeight > 0) {
            mVisibleRowCount++;
        }

        if (mVisibleRowCount > getTotalRowCount()) {
            mVisibleRowCount = getTotalRowCount();
        }
    }

    private int getTotalRowCount() {
        if (getItemCount() == 0 || mTotalColumnCount == 0) {
            return 0;
        }
        int maxRow = getItemCount() / mTotalColumnCount;
        //Bump the row count if it's not exactly even
        if (getItemCount() % mTotalColumnCount != 0) {
            maxRow++;
        }

        return maxRow;
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        //Current row in the grid
        public int row;
        //Current column in the grid
        public int column;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }
}
