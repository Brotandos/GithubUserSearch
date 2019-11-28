package com.brotandos.githubusersearch.users.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brotandos.githubusersearch.R
import com.brotandos.githubusersearch.users.entity.User
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_user.view.avatarImageView
import kotlinx.android.synthetic.main.item_user.view.nameTextView

private const val MAX_VISIBLE_ITEMS_COUNT = 6
private const val START_LOADING_THRESHOLD = 5

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private val users = mutableListOf<User>()

    val needToLoadMoreRelay: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        UserViewHolder(
            parent
        )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
        val itemsSize = users.size
        if (itemsSize > MAX_VISIBLE_ITEMS_COUNT && position >= itemsSize - START_LOADING_THRESHOLD) {
            needToLoadMoreRelay.accept(true)
        }
    }

    override fun getItemCount(): Int = users.size

    fun addUsers(loadedUsers: List<User>) {
        val insertPosition = users.size
        users.addAll(loadedUsers)
        notifyItemRangeInserted(insertPosition, loadedUsers.size)
    }

    fun clearUsers() {
        users.clear()
        notifyDataSetChanged()
    }

    class UserViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
    ) {
        private val nameTextView = itemView.nameTextView
        private val avatarImageView = itemView.avatarImageView

        fun bind(user: User) {
            nameTextView.text = user.login
            Picasso
                .get()
                .load(user.avatarUrl)
                .into(avatarImageView)
        }
    }
}