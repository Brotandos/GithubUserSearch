package com.brotandos.githubusersearch.users.entity

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val login: String,
    @SerializedName("avatar_url") val avatarUrl: String
)