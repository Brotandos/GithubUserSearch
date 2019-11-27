package com.brotandos.githubusersearch.users.adapter

import android.view.ViewGroup
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.RecyclerView
import com.brotandos.githubusersearch.users.entity.User

class UsersAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ViewType>()

    private val delegateAdapters = SparseArrayCompat<ViewTypeDelegateAdapter>()

    init {
        delegateAdapters.put(
            LoadingDelegateAdapter.VIEW_TYPE_ID,
            LoadingDelegateAdapter()
        )
        delegateAdapters.put(
            UsersDelegateAdapter.VIEW_TYPE_ID,
            UsersDelegateAdapter()
        )
        items += LoadingViewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegateAdapters[viewType]?.onCreateViewHolder(parent)
            ?: throw IllegalStateException("viewType with value $viewType is not registered in delegateAdapters")

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        delegateAdapters[viewType]?.onBindViewHolder(holder, items[position])
            ?: throw IllegalStateException("viewType with value $viewType is not registered in delegateAdapters")
    }

    override fun getItemViewType(position: Int): Int =
        items[position].viewTypeId

    override fun getItemCount(): Int = items.size

    fun addUsers(loadedUsers: List<User>) {
        if (loadedUsers.isEmpty()) {
            items.remove(LoadingViewType)
            return notifyItemRemoved(items.size)
        }

        val insertStartPosition = getInsertPosition()
        val usersWasEmptyBefore = insertStartPosition == 0
        val freshLoadedUsers = loadedUsers.map { UserViewType(it) }
        items.addAll(insertStartPosition, freshLoadedUsers)
        when ((freshLoadedUsers.size == 1) to usersWasEmptyBefore) {
            true to true -> { // Only 1 element added
                items.remove(LoadingViewType)
                notifyDataSetChanged()
            }
            false to true -> // keep scroll in top if first time in current page loaded
                notifyDataSetChanged()

            true to false,
            false to false -> // keep scroll in current position
                notifyItemRangeInserted(insertStartPosition, loadedUsers.size) // keep scroll in current position
        }
    }

    private fun getInsertPosition() = items.indexOf(LoadingViewType)
        .takeIf { it >= 0 }
        ?: items.size

    fun clearUsers() {
        items.clear()
        items += LoadingViewType
        notifyDataSetChanged()
    }
}