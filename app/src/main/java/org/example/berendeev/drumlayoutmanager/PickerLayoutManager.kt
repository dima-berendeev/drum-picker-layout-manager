package org.example.berendeev.drumlayoutmanager

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.View
import com.example.dmitry_macpro.drumlayoutmanager.R


/**
 * First and last element limit by center of screen.
 *
 * Notifies items about distance to center.
 *
 * Item view have to have tag with key "VIEW_TAG_DISTANCE_TO_FOCUS_LISTENER" and
 * implementation of DistanceToFocusListener in tag value
 */
class PickerLayoutManager(val context: Context) : RecyclerView.LayoutManager() {
    companion object {
        val NO_POSITION = -1
        val VIEW_TAG_DISTANCE_TO_FOCUS_LISTENER = R.id.VIEW_TAG_DISTANCE_TO_FOCUS_LISTENER
    }

    var stickyY: Int = 0
        get() = height / 2

    var onFocusChangeListener: (adapterPosition: Int) -> Unit = {}

    var focusedItemAdapterPosition: Int = NO_POSITION
        get() = savedState.focusPosition

    private var savedState: SavedState = SavedState()


    override fun onSaveInstanceState(): Parcelable {
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            savedState = state
            if (savedState.pendingScrollPosition == NO_POSITION) {
                // restore last position
                savedState.pendingScrollPosition = savedState.focusPosition
            }
            requestLayout()
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount >= 0) {
            fill(recycler)
        } else {
            detachAndScrapAttachedViews(recycler)
            recycleUnused(recycler)
            savedState.focusPosition = NO_POSITION
            savedState.pendingScrollPosition = NO_POSITION
        }
    }

    private fun fill(recycler: RecyclerView.Recycler) {
        val anchorView = getAnchor(recycler)
        val anchorTop = getDecoratedTop(anchorView)
        val anchorPosition = getPosition(anchorView)
        detachAndScrapAttachedViews(recycler)
        fillDown(anchorTop, anchorPosition, recycler)
        fillUp(anchorTop, anchorPosition, recycler)
        saveFocusPosition()
        notifyHoldersAboutDistance(recycler)
        recycleUnused(recycler)
    }

    private fun saveFocusPosition() {
        val viewInFocus = findViewInFocus()
        if (viewInFocus == null) {
            return
        }
        val focusPosition = getPosition(viewInFocus)
        if (savedState.focusPosition != focusPosition) {
            savedState.focusPosition = focusPosition
            onFocusChangeListener(focusPosition)
        }
    }

    private fun notifyHoldersAboutDistance(recycler: RecyclerView.Recycler) {
        recycler.scrapList.forEach { holder ->
            if (holder is DistanceToFocusListener) {
                val distanceToFocus = calcViewStickyY(holder.itemView) - stickyY
                holder.onDistanceToFocusChanged(distanceToFocus)
            }
        }
    }


    private fun getAnchor(recycler: RecyclerView.Recycler): View {
        if (savedState.pendingScrollPosition != NO_POSITION) {
            val anchor = createAnchor(savedState.pendingScrollPosition, recycler)
            savedState.pendingScrollPosition = NO_POSITION
            return anchor
        } else if (childCount == 0) {
            return createAnchor(0, recycler)
        } else {
            return getChildAt(0)
        }
    }

    fun findViewInFocus(): View? {
        return (0 until childCount)
                .asSequence()
                .map { getChildAt(it) }
                .firstOrNull { getDecoratedTop(it) <= stickyY && stickyY <= getDecoratedBottom(it) }
    }

    private fun createAnchor(anchorPosition: Int, recycler: RecyclerView.Recycler): View {
        val anchorView: View = recycler.getViewForPosition(anchorPosition)
        addView(anchorView)
        measureChildWithMargins(anchorView, 0, 0)
        val anchorHeight = getDecoratedMeasuredHeight(anchorView)
        val anchorWidth = getDecoratedMeasuredWidth(anchorView)
        val anchorTop = stickyY - anchorHeight / 2
        val anchorBottom = anchorTop + anchorHeight
        layoutDecorated(anchorView, 0, anchorTop, anchorWidth, anchorBottom)
        return anchorView
    }

    private fun fillDown(anchorTop: Int, anchorPosition: Int, recycler: RecyclerView.Recycler) {
        var top = anchorTop
        var position = anchorPosition
        while (top < height && position < itemCount) {
            val view = recycler.getViewForPosition(position)
            addView(view)
            measureChildWithMargins(view, 0, 0)
            val viewWidth = getDecoratedMeasuredWidth(view)
            val viewHeight = getDecoratedMeasuredHeight(view)
            layoutDecorated(view, 0, top, viewWidth, top + viewHeight)
            top += viewHeight
            position++
            notifyViewAboutDistance(view)
        }
    }

    private fun fillUp(anchorTop: Int, anchorPosition: Int, recycler: RecyclerView.Recycler) {
        var position = anchorPosition
        if (position < 0) {
            return
        }

        var viewTop = anchorTop
        while (true) {
            position--
            if (position < 0) {
                break
            }
            val view = recycler.getViewForPosition(position)
            measureChildWithMargins(view, 0, 0)
            val viewWidth = getDecoratedMeasuredWidth(view)
            val viewHeight = getDecoratedMeasuredHeight(view)
            viewTop -= viewHeight

            if (viewTop + viewHeight < 0) {
                break
            }

            addView(view)
            layoutDecorated(view, 0, viewTop, viewWidth, viewTop + viewHeight)
            notifyViewAboutDistance(view)
        }
    }

    private fun notifyViewAboutDistance(view: View) {
        val tag = view.getTag(VIEW_TAG_DISTANCE_TO_FOCUS_LISTENER)
        if (tag is DistanceToFocusListener) {
            tag.onDistanceToFocusChanged(calcViewStickyY(view) - stickyY)
        }
    }

    private fun recycleUnused(recycler: RecyclerView.Recycler) {
        val scrapList = mutableListOf<RecyclerView.ViewHolder>()
        scrapList.addAll(recycler.scrapList)

        (0 until scrapList.size).forEach { index: Int ->
            recycler.recycleView(scrapList[index].itemView)
        }
    }


    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val delta = scrollVerticallyInternal(dy)
        offsetChildrenVertical(-delta)
        fill(recycler)
        return delta
    }

    private fun scrollVerticallyInternal(dy: Int): Int {
        var topPosition = Int.MAX_VALUE
        var topView: View? = null
        var bottomPosition = Int.MIN_VALUE
        var bottomView: View? = null

        for (index in 0 until childCount) {
            val view = getChildAt(index)
            val position = getPosition(view)
            if (position < topPosition) {
                topPosition = position
                topView = view
            }
            if (position > bottomPosition) {
                bottomPosition = position
                bottomView = view
            }
        }

        if (bottomView == null || topView == null) {
            return 0
        }

        if (dy < 0) {
            if (topPosition == 0) {
                val possibleDelta = calcViewStickyY(topView) - stickyY
                return if (possibleDelta > dy) {
                    possibleDelta
                } else {
                    dy
                }
            }
        } else {
            if (bottomPosition == itemCount - 1) {
                val possibleDelta = calcViewStickyY(bottomView) - stickyY
                return if (possibleDelta < dy) {
                    possibleDelta
                } else {
                    dy
                }
            }
        }
        return dy
    }

    fun calcViewStickyY(view: View) =
            getDecoratedTop(view) + getDecoratedMeasuredHeight(view) / 2

    override fun scrollToPosition(position: Int) {
        savedState.pendingScrollPosition = position
        requestLayout()
    }

    override fun canScrollVertically() = true

    class SavedState() : Parcelable {
        var pendingScrollPosition: Int = NO_POSITION
        var focusPosition: Int = 0

        constructor(parcel: Parcel) : this() {
            focusPosition = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(focusPosition)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    interface DistanceToFocusListener {
        fun onDistanceToFocusChanged(distance: Int)
    }
}