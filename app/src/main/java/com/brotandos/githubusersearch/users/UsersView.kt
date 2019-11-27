package com.brotandos.githubusersearch.users

import com.brotandos.githubusersearch.users.entity.User

interface UsersView {

    fun clearUsers()

    fun showUsers(users: List<User>)

    fun onError(reason: String, throwable: Throwable)
}