package com.brotandos.githubusersearch.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brotandos.githubusersearch.users.entity.User
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

private const val SEARCH_TIMEOUT = 600L
private const val SEARCH_STARTING_PAGE = 1

class UsersViewModel(private val usersView: UsersView) : ViewModel() {

    private val usersRepository = UsersRepository()

    private val compositeDisposable = CompositeDisposable()

    private val queryRelay = BehaviorRelay.createDefault("")

    private val isScrolledToBottomRelay = BehaviorRelay.createDefault(false)

    private var itemsLength = 0

    init {
        queryRelay
            .distinctUntilChanged()
            .debounce(SEARCH_TIMEOUT, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { clearUsers() },
                { usersView.onError("Something went wrong", it) }
            )
            .also { compositeDisposable.add(it) }

        isScrolledToBottomRelay
            .throttleWithTimeout(SEARCH_TIMEOUT, TimeUnit.MILLISECONDS)
            .filter { true }
            .distinctUntilChanged()
            .switchMapSingle {
                if (!it) return@switchMapSingle Single
                    .just(listOf<User>())
                    .observeOn(AndroidSchedulers.mainThread())

                val query = queryRelay.value
                val single = if (query.isEmpty()) {
                    usersRepository.getUsers(itemsLength)
                } else {
                    val page = itemsLength / UsersRepository.ELEMENTS_PER_PAGE + SEARCH_STARTING_PAGE
                    println(page.toString().repeat(10))
                    usersRepository.searchUsers(query, page)
                }
                single
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe({
                usersView.showUsers(it)
                itemsLength += it.size
                isScrolledToBottomRelay.accept(false)
            }, { usersView.onError("Something went wrong", it) })
            .also { compositeDisposable.add(it) }

    }

    fun onQueryEdited(word: String) = queryRelay.accept(word)

    fun loadPage() {
        isScrolledToBottomRelay.accept(true)
    }

    private fun clearUsers() {
        Completable.fromCallable {
            itemsLength = 0
            Any()
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { usersView.clearUsers() }
            .also { compositeDisposable.add(it) }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}

class UsersViewModelFactory(private val usersView: UsersView) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        UsersViewModel(usersView) as T
}