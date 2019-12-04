package com.brotandos.githubusersearch.users.ui

import android.content.Context
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
import com.brotandos.githubusersearch.auth.Profile
import com.brotandos.githubusersearch.common.setClearable
import com.brotandos.githubusersearch.common.startActivity
import com.brotandos.githubusersearch.security.StorePasswordActivity
import com.brotandos.githubusersearch.users.entity.User
import com.facebook.AccessToken
import com.facebook.login.LoginManager
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
import kotlinx.android.synthetic.main.layout_nav_header.securityAuthButton
import kotlinx.android.synthetic.main.layout_nav_header.view.emailTextView
import kotlinx.android.synthetic.main.layout_nav_header.view.logoutButton
import kotlinx.android.synthetic.main.layout_nav_header.view.profileImageView
import kotlinx.android.synthetic.main.layout_nav_header.view.profileNameTextView
import kotlinx.android.synthetic.main.layout_nav_header.view.securityAuthButton

private const val SCROLL_DIRECTION_DOWN = 1
private const val EXTRA_PROFILE_NAME = "EXTRA_PROFILE_NAME"
private const val EXTRA_PROFILE_LINK = "EXTRA_PROFILE_LINK"
private const val EXTRA_PHOTO_LINK = "EXTRA_PHOTO_LINK"

class UsersActivity : AppCompatActivity(R.layout.activity_users), UsersView {

    companion object {
        fun start(
            context: Context,
            profileName: String?,
            profileLink: String?,
            photoLink: String?
        ) {
            val intent = Intent(context, UsersActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(EXTRA_PROFILE_NAME, profileName)
            profileLink.let { intent.putExtra(EXTRA_PROFILE_LINK, it) }
            photoLink.let { intent.putExtra(EXTRA_PHOTO_LINK, it) }
            context.startActivity(intent)
        }
    }

    private val profileName: String? by lazy {
        intent.getStringExtra(EXTRA_PROFILE_NAME)
    }

    private val profileLink: String? by lazy {
        intent.getStringExtra(EXTRA_PROFILE_LINK)
    }

    private val photoLink: String? by lazy {
        intent.getStringExtra(EXTRA_PHOTO_LINK)
    }

    private val adapter = UsersAdapter()

    private val usersViewModel by lazy {
        ViewModelProviders.of(
            this,
            UsersViewModel.Factory(
                this,
                adapter.needToLoadMoreRelay
            )
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
        usersViewModel.checkIfLoggedInFacebook()
        setTitle(R.string.title_search_users)
        initNavView()
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

    override fun onFacebookTokenExpired() {
        onLoggedOut()
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

    private fun initNavView() {
        val headerView = navigationView.getHeaderView(0)
        val nameTextView = headerView.profileNameTextView
        val emailTextView = headerView.emailTextView
        val avatarImageView = headerView.profileImageView
        headerView.securityAuthButton.setOnClickListener { startActivity<StorePasswordActivity>() }
        headerView.logoutButton.setOnClickListener {
            GoogleSignIn.getLastSignedInAccount(this)?.let {
                GoogleSignIn
                    .getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .signOut()
                    .addOnCompleteListener { onLoggedOut() }
            }

            AccessToken.getCurrentAccessToken()?.let {
                LoginManager.getInstance().logOut()
            }
        }

        profileName?.let(nameTextView::setText)
        profileLink?.let(emailTextView::setText)
        photoLink?.let {
            Picasso.get().load(it).into(avatarImageView)
        }
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

    private fun onLoggedOut() {
        startActivity<AuthActivity>(
            needToFinishCurrent = true,
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
    }
}