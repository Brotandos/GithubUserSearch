package com.brotandos.githubusersearch.users.ui

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brotandos.githubusersearch.users.repository.UsersRepository
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

private const val SEARCH_TIMEOUT = 600L
private const val SEARCH_STARTING_PAGE = 1

private const val ELEMENTS_PER_PAGE =
    UsersRepository.ELEMENTS_PER_PAGE

class UsersViewModel(
    private val usersView: UsersView,
    private val needToLoadMoreRelay: BehaviorRelay<Boolean>
) : ViewModel() {

    private val usersRepository =
        UsersRepository()

    private val compositeDisposable = CompositeDisposable()

    private val queryRelay = BehaviorRelay.createDefault("")

    private var itemsLength = 0

    private var lastLoadedUserId = 0

    private val nextSearchPageNumber
        get() = ceil(itemsLength / ELEMENTS_PER_PAGE.toDouble()).toInt() + SEARCH_STARTING_PAGE

    init {
        queryRelay
            .distinctUntilChanged()
            .debounce(SEARCH_TIMEOUT, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { usersView.onLoadingStarted() }
            .subscribe(
                {
                    clearUsers()
                    loadNextPage()
                },
                {
                    usersView.onError("Something went wrong", it)
                    usersView.onLoadingFinished()
                }
            )
            .untilViewModelCleared()

        needToLoadMoreRelay
            .throttleWithTimeout(SEARCH_TIMEOUT, TimeUnit.MILLISECONDS)
            .filter { it }
            .switchMapSingle {
                Handler(Looper.getMainLooper()).post { usersView.onLoadingStarted() }
                val query = queryRelay.value
                val single = if (query.isEmpty()) {
                    usersRepository.getUsers(lastLoadedUserId)
                } else {
                    println("next page: $nextSearchPageNumber")
                    usersRepository.searchUsers(query, nextSearchPageNumber)
                }
                single
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe(
                { users ->
                    if (users.isEmpty()) {
                        usersView.onEmptyUsersLoaded()
                    } else {
                        usersView.showUsers(users)
                        itemsLength += users.size
                        lastLoadedUserId = users.lastOrNull()?.id ?: lastLoadedUserId
                    }
                    usersView.onLoadingFinished()
                    needToLoadMoreRelay.accept(false)
                },
                {
                    usersView.onError("Something went wrong", it)
                    usersView.onLoadingFinished()
                }
            )
            .untilViewModelCleared()
    }

    fun onActivityCreated() = loadUsersFromStart()

    fun onQueryEdited(word: String) = queryRelay.accept(word)

    fun onRetryButtonClicked() = loadNextPage()

    fun onPulledDown() {
        if (needToLoadMoreRelay.value == true) return
        needToLoadMoreRelay.accept(true)
        usersView.onLoadingStarted()
    }

    fun onRefreshButtonClicked() = loadUsersFromStart()

    private fun loadUsersFromStart() {
        itemsLength = 0
        loadNextPage()
    }

    private fun loadNextPage() {
        usersView.onLoadingStarted()
        needToLoadMoreRelay.accept(true)
    }

    private fun clearUsers() {
        Completable.fromCallable {
            itemsLength = 0
            Any()
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { usersView.clearUsers() }
            .untilViewModelCleared()
    }

    private fun Disposable.untilViewModelCleared() = compositeDisposable.add(this)

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    fun checkIfLoggedInFacebook() {
        if (AccessToken.getCurrentAccessToken()?.isExpired == true) {
            LoginManager.getInstance().logOut()
            usersView.onFacebookTokenExpired()
        }
    }

    class Factory(
        private val usersView: UsersView,
        private val needToLoadMoreRelay: BehaviorRelay<Boolean>
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            UsersViewModel(
                usersView,
                needToLoadMoreRelay
            ) as T
    }
}