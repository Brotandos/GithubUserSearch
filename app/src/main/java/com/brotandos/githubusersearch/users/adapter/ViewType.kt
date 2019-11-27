package com.brotandos.githubusersearch.users.adapter

import androidx.annotation.LayoutRes
import com.brotandos.githubusersearch.users.entity.User

sealed class ViewType(
    @LayoutRes val viewTypeId: Int
)

object LoadingViewType : ViewType(LoadingDelegateAdapter.VIEW_TYPE_ID)

class UserViewType(
    val user: User
) : ViewType(UsersDelegateAdapter.VIEW_TYPE_ID)