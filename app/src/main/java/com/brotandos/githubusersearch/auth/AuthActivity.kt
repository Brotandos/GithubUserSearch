package com.brotandos.githubusersearch.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.brotandos.githubusersearch.users.ui.UsersActivity
import com.brotandos.githubusersearch.R
import com.brotandos.githubusersearch.common.startActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.android.synthetic.main.activity_auth.googleSignInButton

private const val GOOGLE_PLUS_REQUEST_CODE = 1

class AuthActivity : AppCompatActivity(R.layout.activity_auth) {

    override fun onStart() {
        super.onStart()
        GoogleSignIn.getLastSignedInAccount(this) ?: return

        onSignedIn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, options)
        googleSignInButton.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent,
                GOOGLE_PLUS_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == GOOGLE_PLUS_REQUEST_CODE) {
            GoogleSignIn.getSignedInAccountFromIntent(data) ?: return

            onSignedIn()
        }
    }

    private fun onSignedIn() = startActivity<UsersActivity>(
        needToFinishCurrent = true,
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    )
}