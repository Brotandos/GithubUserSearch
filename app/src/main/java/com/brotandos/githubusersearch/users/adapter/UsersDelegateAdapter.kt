package com.brotandos.githubusersearch.users.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.brotandos.githubusersearch.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_user.view.*

class UsersDelegateAdapter :
    ViewTypeDelegateAdapter {

    companion object {
        @LayoutRes const val VIEW_TYPE_ID =
            R.layout.item_user
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        UserViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType) {
        (holder as? UserViewHolder)?.bind(item as UserViewType)
    }

    class UserViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
    ) {
        private val nameTextView = itemView.nameTextView
        private val avatarImageView = itemView.avatarImageView

        fun bind(item: UserViewType) {
            val user = item.user
            nameTextView.text = user.login
            Picasso
                .get()
                .load(user.avatarUrl)
                .into(avatarImageView)
        }
    }
}