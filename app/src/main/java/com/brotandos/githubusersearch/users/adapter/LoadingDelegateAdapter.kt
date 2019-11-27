package com.brotandos.githubusersearch.users.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.brotandos.githubusersearch.R

class LoadingDelegateAdapter :
    ViewTypeDelegateAdapter {

    companion object {
        @LayoutRes const val VIEW_TYPE_ID =
            R.layout.item_progress
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(LoadingViewType.viewTypeId, parent, false)
        ) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType) = Unit
}