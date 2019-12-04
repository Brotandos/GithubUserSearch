package com.brotandos.githubusersearch.security

import android.content.Context
import android.os.Bundle
import android.security.KeyPairGeneratorSpec
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.widget.addTextChangedListener
import com.brotandos.githubusersearch.R
import com.brotandos.githubusersearch.common.startActivity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_store_password.passwordEditText
import kotlinx.android.synthetic.main.activity_store_password.pinEditText
import kotlinx.android.synthetic.main.activity_store_password.savePasswordButton

class StorePasswordActivity : AppCompatActivity(R.layout.activity_store_password) {

    private val authSecurityRepository: AuthSecurityRepository by lazy {
        AuthSecurityRepository(
            FingerprintManagerCompat.from(this),
            KeyPairGeneratorSpec.Builder(this),
            getSharedPreferences(AuthSecurityRepository.CRYPTO_PREFERENCE_FILENAME, Context.MODE_PRIVATE),
            packageName
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSupportActionBar()
        if (authSecurityRepository.isPasswordStored()) {
            startActivity<ShowPasswordActivity>()
            finish()
        }
        pinEditText.addTextChangedListener {
            savePasswordButton.visibility = if (it.toString().length == AuthSecurityRepository.PIN_LENGTH) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        savePasswordButton.setOnClickListener {
            Completable.fromCallable {
                authSecurityRepository.savePassword(
                    pinEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    startActivity<ShowPasswordActivity>()
                    finish()
                }
        }
    }

    private fun initSupportActionBar() {
        setTitle(R.string.title_store_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}