package org.example.berendeev.drumlayoutmanager

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SnapHelper
import android.view.View


class PickerSnapHelper : SnapHelper() {
    override fun calculateDistanceToFinalSnap(layoutManager: RecyclerView.LayoutManager, targetView: View): IntArray {
        val lm = layoutManager as PickerLayoutManager
        val viewStickyY = lm.calcViewStickyY(targetView)
        val stickyY = layoutManager.stickyY
        return intArrayOf(0, viewStickyY - stickyY)
    }

    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {
        return RecyclerView.NO_POSITION
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        return (layoutManager as PickerLayoutManager).findViewInFocus()
    }
}