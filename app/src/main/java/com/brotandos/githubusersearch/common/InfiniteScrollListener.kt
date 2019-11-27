package com.brotandos.githubusersearch.common

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val VISIBILITY_THRESHOLD = 2

class InfiniteScrollListener(
    private val layoutManager: LinearLayoutManager,
    private val onEndReached: () -> Unit
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

        if (dy < 0) return

        val visibleItemCount = recyclerView.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

        if (totalItemCount - visibleItemCount <= firstVisibleItem + VISIBILITY_THRESHOLD) {
            onEndReached()
        }
    }
}