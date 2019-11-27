package com.brotandos.githubusersearch.users.entity

import com.google.gson.annotations.SerializedName

data class SearchEntity(
    @SerializedName("total_count")
    val totalCount: Int,
    val items: List<User>
)