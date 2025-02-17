package com.derosa.progettolam.adapters

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with(outRect) {
            top = spacing
            left = spacing
            right = spacing

            if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
                bottom = spacing
            }
        }
    }
}