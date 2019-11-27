package com.brotandos.githubusersearch.users

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.brotandos.githubusersearch.R
import com.brotandos.githubusersearch.auth.AuthActivity
import com.brotandos.githubusersearch.common.InfiniteScrollListener
import com.brotandos.githubusersearch.common.startActivity
import com.brotandos.githubusersearch.users.adapter.UsersAdapter
import com.brotandos.githubusersearch.users.entity.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_users.drawerLayout
import kotlinx.android.synthetic.main.activity_users.navigationView
import kotlinx.android.synthetic.main.activity_users.searchQueryEditText
import kotlinx.android.synthetic.main.activity_users.usersRecyclerView
import kotlinx.android.synthetic.main.layout_nav_header.view.emailTextView
import kotlinx.android.synthetic.main.layout_nav_header.view.logoutButton
import kotlinx.android.synthetic.main.layout_nav_header.view.profileImageView

private const val VISIBILITY_THRESHOLD = 5

class UsersActivity : AppCompatActivity(R.layout.activity_users), UsersView {

    private val usersViewModel by lazy {
        ViewModelProviders.of(
            this,
            UsersViewModelFactory(this)
        )[UsersViewModel::class.java]
    }

    private val layoutManager: LinearLayoutManager by lazy {
        usersRecyclerView.layoutManager as LinearLayoutManager
    }

    private val scrollListener: InfiniteScrollListener by lazy {
        InfiniteScrollListener(layoutManager) {
            usersViewModel.loadPage()
        }
    }

    private val adapter = UsersAdapter()

    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = GoogleSignIn.getLastSignedInAccount(this) ?: return onLoggedOut()
        initNavView(account)
        initDrawer()
        initSearchView()
        initRecyclerView()
        usersViewModel.loadPage()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        drawerToggle.onOptionsItemSelected(item)
            .takeIf { it }
            ?: super.onOptionsItemSelected(item)


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
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.empty,
            R.string.empty
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initSearchView() {
        searchQueryEditText.addTextChangedListener {
            usersViewModel.onQueryEdited(it.toString())
        }
    }

    private fun initRecyclerView() {
        usersRecyclerView.adapter = adapter
        usersRecyclerView.addOnScrollListener(scrollListener)
    }

    override fun clearUsers() {
        adapter.clearUsers()
    }

    override fun showUsers(users: List<User>) = adapter.addUsers(users)

    override fun onError(reason: String, throwable: Throwable) =
        Toast.makeText(this, "$reason: $throwable", Toast.LENGTH_LONG).show()

    private fun onLoggedOut() {
        startActivity<AuthActivity>(
            needToFinishCurrent = true,
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
    }
}