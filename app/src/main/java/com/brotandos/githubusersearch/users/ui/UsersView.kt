package com.brotandos.githubusersearch.users.ui

import com.brotandos.githubusersearch.users.entity.User

interface UsersView {

    fun onLoadingStarted()

    fun onLoadingFinished()

    fun showUsers(users: List<User>)

    fun clearUsers()

    fun onEmptyUsersLoaded()

    fun onError(reason: String, throwable: Throwable)
}