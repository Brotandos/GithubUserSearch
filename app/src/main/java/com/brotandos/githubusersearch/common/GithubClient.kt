package com.brotandos.githubusersearch.common

import com.brotandos.githubusersearch.users.entity.SearchEntity
import com.brotandos.githubusersearch.users.entity.User
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

private const val QUERY_PARAM_LAST_LOADED_USER_ID = "since"
private const val QUERY_PARAM = "q"
private const val QUERY_PARAM_PAGE = "page"
private const val QUERY_PARAM_ELEMENTS_PER_PAGE = "per_page"

interface GithubClient {

    @GET("/users")
    fun getUsers(
        @Query(QUERY_PARAM_LAST_LOADED_USER_ID) lastLoadedUserId: Int
    ): Single<Response<List<User>>>

    @GET("/search/users")
    fun getUsersByLogin(
        @Query(QUERY_PARAM) queryPattern: String,
        @Query(QUERY_PARAM_PAGE) page: Int,
        @Query(QUERY_PARAM_ELEMENTS_PER_PAGE) elementsPerPage: Int
    ): Single<Response<SearchEntity>>
}