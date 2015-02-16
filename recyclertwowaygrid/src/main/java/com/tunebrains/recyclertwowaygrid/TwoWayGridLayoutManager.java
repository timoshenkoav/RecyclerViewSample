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
    private static final int DEFAULT_COLUMN_COUNT = 6;
    private int mTotalColumnCount = DEFAULT_COLUMN_COUNT;
    /* Fill Direction Constants */
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_START = 0;
    private static final int DIRECTION_END = 1;
    private static final int DIRECTION_UP = 2;
    private static final int DIRECTION_DOWN = 3;
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
                 /*
             * Adjust the visible position if out of bounds in the
             * new layout. This occurs when the new item count in an adapter
             * is much smaller than it was before, and you are scrolled to
             * a location where no items would exist.
             */
                int maxFirstRow = getTotalRowCount() - (mVisibleRowCount - 1);
                int maxFirstCol = getTotalColumnCount() - (mVisibleColumnCount - 1);
                boolean isOutOfRowBounds = getFirstVisibleRow() > maxFirstRow;
                boolean isOutOfColBounds = getFirstVisibleColumn() > maxFirstCol;
                if (isOutOfRowBounds || isOutOfColBounds) {
                    int firstRow;
                    if (isOutOfRowBounds) {
                        firstRow = maxFirstRow;
                    } else {
                        firstRow = getFirstVisibleRow();
                    }
                    int firstCol;
                    if (isOutOfColBounds) {
                        firstCol = maxFirstCol;
                    } else {
                        firstCol = getFirstVisibleColumn();
                    }
                    mFirstVisiblePosition = firstRow * getTotalColumnCount() + firstCol;

                    childLeft = getHorizontalSpace() - (mDecoratedChildWidth * mVisibleColumnCount);
                    childTop = getVerticalSpace() - (mDecoratedChildHeight * mVisibleRowCount);

                    //Correct cases where shifting to the bottom-right overscrolls the top-left
                    // This happens on data sets too small to scroll in a direction.
                    if (getFirstVisibleRow() == 0) {
                        childTop = Math.min(childTop, 0);
                    }
                    if (getFirstVisibleColumn() == 0) {
                        childLeft = Math.min(childLeft, 0);
                    }
                }
            }
        }


        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);
        //Fill the grid for the initial layout of views
        fillGrid(DIRECTION_NONE, childLeft, childTop, recycler);
    }

    private void fillGrid(int pDirection, int pChildLeft, int pChildTop, RecyclerView.Recycler pRecycler) {
        if (mFirstVisiblePosition < 0) mFirstVisiblePosition = 0;
        if (mFirstVisiblePosition >= getItemCount()) mFirstVisiblePosition = (getItemCount() - 1);
        SparseArray<View> viewCache = new SparseArray<View>(getChildCount());
        int startLeftOffset = getPaddingLeft();
        int startTopOffset = getPaddingTop();
        if (getChildCount() != 0) {
            final View topView = getChildAt(0);
            startLeftOffset = getDecoratedLeft(topView);
            startTopOffset = getDecoratedTop(topView);

            switch (pDirection) {
                case DIRECTION_START:
                    startLeftOffset -= mDecoratedChildWidth;
                    break;
                case DIRECTION_END:
                    startLeftOffset += mDecoratedChildWidth;
                    break;
                case DIRECTION_UP:
                    startTopOffset -= mDecoratedChildHeight;
                    break;
                case DIRECTION_DOWN:
                    startTopOffset += mDecoratedChildHeight;
                    break;
            }
            
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
        /*
         * Next, we advance the visible position based on the fill direction.
         * DIRECTION_NONE doesn't advance the position in any direction.
         */
        switch (pDirection) {
            case DIRECTION_START:
                mFirstVisiblePosition--;
                break;
            case DIRECTION_END:
                mFirstVisiblePosition++;
                break;
            case DIRECTION_UP:
                mFirstVisiblePosition -= getTotalColumnCount();
                break;
            case DIRECTION_DOWN:
                mFirstVisiblePosition += getTotalColumnCount();
                break;
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
            if (i % mVisibleColumnCount == (mVisibleColumnCount - 1)) {
                leftOffset = startLeftOffset;
                topOffset += mDecoratedChildHeight;

            } else {
                leftOffset += mDecoratedChildWidth;
            }


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

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        //Take top measurements from the top-left child
        final View topView = getChildAt(0);
        //Take bottom measurements from the bottom-right child.
        final View bottomView = getChildAt(getChildCount() - 1);

        //Optimize the case where the entire data set is too small to scroll
        int viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView);
        if (viewSpan < getVerticalSpace()) {
            //We cannot scroll in either direction
            return 0;
        }

        int delta;
        int maxRowCount = getTotalRowCount();
        boolean topBoundReached = getFirstVisibleRow() == 0;
        boolean bottomBoundReached = getLastVisibleRow() >= maxRowCount;
        if (dy > 0) { // Contents are scrolling up
            //Check against bottom bound
            if (bottomBoundReached) {
                //If we've reached the last row, enforce limits
                int bottomOffset;
                if (rowOfIndex(getChildCount() - 1) >= (maxRowCount - 1)) {
                    //We are truly at the bottom, determine how far
                    bottomOffset = getVerticalSpace() - getDecoratedBottom(bottomView)
                            + getPaddingBottom();
                } else {
                    /*
                     * Extra space added to account for allowing bottom space in the grid.
                     * This occurs when the overlap in the last row is not large enough to
                     * ensure that at least one element in that row isn't fully recycled.
                     */
                    bottomOffset = getVerticalSpace() - (getDecoratedBottom(bottomView)
                            + mDecoratedChildHeight) + getPaddingBottom();
                }

                delta = Math.max(-dy, bottomOffset);
            } else {
                //No limits while the last row isn't visible
                delta = -dy;
            }
        } else { // Contents are scrolling down
            //Check against top bound
            if (topBoundReached) {
                int topOffset = -getDecoratedTop(topView) + getPaddingTop();

                delta = Math.min(-dy, topOffset);
            } else {
                delta = -dy;
            }
        }

        offsetChildrenVertical(delta);

        if (dy > 0) {
            if (getDecoratedBottom(topView) < 0 && !bottomBoundReached) {
                fillGrid(DIRECTION_DOWN, recycler);
            } else if (!bottomBoundReached) {
                fillGrid(DIRECTION_NONE, recycler);
            }
        } else {
            if (getDecoratedTop(topView) > 0 && !topBoundReached) {
                fillGrid(DIRECTION_UP, recycler);
            } else if (!topBoundReached) {
                fillGrid(DIRECTION_NONE, recycler);
            }
        }

        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return -delta;
    }

    private int rowOfIndex(int childIndex) {
        int position = positionOfIndex(childIndex);

        return position / getTotalColumnCount();
    }

    private int getFirstVisibleRow() {
        return (mFirstVisiblePosition / getTotalColumnCount());
    }

    private int getLastVisibleRow() {
        return getFirstVisibleRow() + mVisibleRowCount;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        //Take leftmost measurements from the top-left child
        final View topView = getChildAt(0);
        //Take rightmost measurements from the top-right child
        final View bottomView = getChildAt(mVisibleColumnCount - 1);

        int viewSpan = getDecoratedRight(bottomView) - getDecoratedLeft(topView);
        if (viewSpan < getHorizontalSpace()) {
            //We cannot scroll in either direction
            return 0;
        }
        int delta;
        boolean leftBoundReached = getFirstVisibleColumn() == 0;
        boolean rightBoundReached = getLastVisibleColumn() >= getTotalColumnCount();
        if (dx > 0) { // Contents are scrolling left
            //Check right bound
            if (rightBoundReached) {
                //If we've reached the last column, enforce limits
                int rightOffset = getHorizontalSpace() - getDecoratedRight(bottomView) + getPaddingRight();
                delta = Math.max(-dx, rightOffset);
            } else {
                //No limits while the last column isn't visible
                delta = -dx;
            }
        } else { // Contents are scrolling right
            //Check left bound
            if (leftBoundReached) {
                int leftOffset = -getDecoratedLeft(topView) + getPaddingLeft();
                delta = Math.min(-dx, leftOffset);
            } else {
                delta = -dx;
            }
        }
        offsetChildrenHorizontal(delta);
        if (dx > 0) {
            if (getDecoratedRight(topView) < 0 && !rightBoundReached) {
                fillGrid(DIRECTION_END, recycler);
            } else if (!rightBoundReached) {
                fillGrid(DIRECTION_NONE, recycler);
            }
        } else {
            if (getDecoratedLeft(topView) > 0 && !leftBoundReached) {
                fillGrid(DIRECTION_START, recycler);
            } else if (!leftBoundReached) {
                fillGrid(DIRECTION_NONE, recycler);
            }
        }
        return -delta;
    }

    private void fillGrid(int direction, RecyclerView.Recycler recycler) {
        fillGrid(direction, 0, 0, recycler);
    }

    private int getLastVisibleColumn() {
        return getFirstVisibleColumn() + mVisibleColumnCount;
    }

    private int getFirstVisibleColumn() {
        return (mFirstVisiblePosition % getTotalColumnCount());
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
