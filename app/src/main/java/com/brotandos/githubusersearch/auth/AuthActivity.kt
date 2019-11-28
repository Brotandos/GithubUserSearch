package com.brotandos.githubusersearch.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.brotandos.githubusersearch.users.ui.UsersActivity
import com.brotandos.githubusersearch.R
import com.brotandos.githubusersearch.common.startActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.Profile as FacebookProfile
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_auth.facebookSignInButton
import kotlinx.android.synthetic.main.activity_auth.googleSignInButton

private const val GOOGLE_PLUS_REQUEST_CODE = 1

class AuthActivity : AppCompatActivity(R.layout.activity_auth) {

    private val facebookCallbackManager = CallbackManager.Factory.create()

    override fun onStart() {
        super.onStart()
        GoogleSignIn.getLastSignedInAccount(this)
            ?: AccessToken.getCurrentAccessToken()
            ?: return

        onSignedIn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGoogleSignIn()
        initFacebookSignIn()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == GOOGLE_PLUS_REQUEST_CODE) {
            val profile = GoogleSignIn.getSignedInAccountFromIntent(data)?.result ?: return
            val name = profile.displayName
                ?: return Toast.makeText(
                    this,
                    R.string.profile_name_not_found,
                    Toast.LENGTH_SHORT).show()

            UsersActivity.start(this, name, profile.email, profile.photoUrl?.toString())
        }
    }

    private fun initGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, options)
        googleSignInButton.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, GOOGLE_PLUS_REQUEST_CODE)
        }
    }

    private fun initFacebookSignIn() {
        facebookSignInButton.setReadPermissions("email")
        facebookSignInButton.registerCallback(facebookCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                val facebookProfile = FacebookProfile.getCurrentProfile()
                UsersActivity.start(this@AuthActivity,
                    facebookProfile?.name,
                    facebookProfile?.linkUri.toString(),
                    facebookProfile?.getProfilePictureUri(200, 200).toString()
                )
                finish()
            }

            override fun onCancel() =
                Toast.makeText(
                    this@AuthActivity,
                    R.string.facebook_sign_in_cancelled,
                    Toast.LENGTH_SHORT
                ).show()

            override fun onError(error: FacebookException?) =
                Snackbar.make(
                    facebookSignInButton,
                    error?.message ?: getString(R.string.login_error),
                    Snackbar.LENGTH_LONG
                ).show()

        })
    }

    private fun onSignedIn() = startActivity<UsersActivity>(
        needToFinishCurrent = true,
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    )
}