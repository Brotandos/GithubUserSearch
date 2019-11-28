package com.brotandos.githubusersearch.users.repository

import com.brotandos.githubusersearch.common.GithubClient
import com.brotandos.githubusersearch.users.entity.User
import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val baseUrl = "https://api.github.com"
private const val QUERY_LOGIN_KEYWORD = "in:login"
private const val READ_TIMEOUT_SECONDS = 15L

class UsersRepository {

    companion object {
        const val ELEMENTS_PER_PAGE = 30
    }

    private val client by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return@lazy Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()
            .create(GithubClient::class.java)
    }

    fun getUsers(lastLoadedUserId: Int): Single<List<User>> =
        client.getUsers(lastLoadedUserId)
            .map { it.body() ?: listOf() }

    fun searchUsers(word: String, page: Int): Single<List<User>> =
        client.getUsersByLogin(getQueryPattern(word), page,
            ELEMENTS_PER_PAGE
        )
            .map {
                it.body()?.items ?: listOf()
            }

    private fun getQueryPattern(word: String) = "$word $QUERY_LOGIN_KEYWORD"
}