package com.brotandos.githubusersearch.users

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.brotandos.githubusersearch.R
import com.brotandos.githubusersearch.auth.AuthActivity
import com.brotandos.githubusersearch.common.setClearable
import com.brotandos.githubusersearch.common.startActivity
import com.brotandos.githubusersearch.users.adapter.UsersAdapter
import com.brotandos.githubusersearch.users.entity.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_users.drawerLayout
import kotlinx.android.synthetic.main.activity_users.navigationView
import kotlinx.android.synthetic.main.activity_users.progressBar
import kotlinx.android.synthetic.main.activity_users.searchQueryEditText
import kotlinx.android.synthetic.main.activity_users.usersRecyclerView
import kotlinx.android.synthetic.main.layout_nav_header.view.emailTextView
import kotlinx.android.synthetic.main.layout_nav_header.view.logoutButton
import kotlinx.android.synthetic.main.layout_nav_header.view.profileImageView

private const val SCROLL_DIRECTION_DOWN = 1

class UsersActivity : AppCompatActivity(R.layout.activity_users), UsersView {

    private val adapter = UsersAdapter()

    private val usersViewModel by lazy {
        ViewModelProviders.of(
            this,
            UsersViewModel.Factory(this, adapter.needToLoadMoreRelay)
        )[UsersViewModel::class.java]
    }

    private val drawerToggle: ActionBarDrawerToggle by lazy {
        ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.empty,
            R.string.empty
        )
    }

    private val pullToLoadListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (usersRecyclerView.canScrollVertically(SCROLL_DIRECTION_DOWN)) return
            usersViewModel.onPulledDown()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = GoogleSignIn.getLastSignedInAccount(this) ?: return onLoggedOut()
        initNavView(account)
        initDrawer()
        initSearchView()
        usersRecyclerView.adapter = adapter
        usersRecyclerView.addOnScrollListener(pullToLoadListener)
        usersViewModel.onActivityCreated()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_refresh, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                clearUsers()
                usersViewModel.onRefreshButtonClicked()
                return true
            }
        }

        return drawerToggle.onOptionsItemSelected(item).takeIf { it }
            ?: super.onOptionsItemSelected(item)
    }


    private fun initNavView(account: GoogleSignInAccount) {
        val headerView = navigationView.getHeaderView(0)
        val emailTextView = headerView.emailTextView
        val avatarImageView = headerView.profileImageView
        headerView.logoutButton.setOnClickListener {
            GoogleSignIn
                .getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                .signOut()
                .addOnCompleteListener { onLoggedOut() }
        }

        account.email?.let(emailTextView::setText)
        val photoUrl = account.photoUrl?.toString() ?: return
        Picasso
            .get()
            .load(photoUrl)
            .into(avatarImageView)
    }

    private fun initDrawer() {
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initSearchView() {
        searchQueryEditText.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                searchQueryEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                searchQueryEditText.setOnTouchListener { _, _ -> false }
            } else {
                searchQueryEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear, 0)
                searchQueryEditText.setClearable()
            }
            usersViewModel.onQueryEdited(it.toString())
        }
    }

    override fun onLoadingStarted() = progressBar.setVisibility(View.VISIBLE)

    override fun onLoadingFinished() = progressBar.setVisibility(View.GONE)

    override fun clearUsers() {
        adapter.clearUsers()
    }

    override fun showUsers(users: List<User>) {
        adapter.addUsers(users)
    }

    override fun onEmptyUsersLoaded() {
        Snackbar
            .make(drawerLayout, R.string.empty_users_loaded, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_refresh) {
                usersViewModel.onRefreshButtonClicked()
            }
            .show()
    }

    override fun onError(reason: String, throwable: Throwable) {
        Snackbar
            .make(drawerLayout, "$reason: ${throwable.message}", Snackbar.LENGTH_LONG)
            .setAction(R.string.action_reload_page) {
                usersViewModel.onRetryButtonClicked()
            }
    }

    private fun onLoggedOut() {
        startActivity<AuthActivity>(
            needToFinishCurrent = true,
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
    }
}