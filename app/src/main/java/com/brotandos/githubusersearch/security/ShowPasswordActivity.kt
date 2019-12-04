package com.brotandos.githubusersearch.security

import android.content.Context
import android.os.Bundle
import android.security.KeyPairGeneratorSpec
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.widget.addTextChangedListener
import com.brotandos.githubusersearch.R
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_show_password.dropPasswordButton
import kotlinx.android.synthetic.main.activity_show_password.openFingerprintDialog
import kotlinx.android.synthetic.main.activity_show_password.pinEditText

private const val INCORRECT_PIN_MESSAGE = "PIN incorrect"

class ShowPasswordActivity : AppCompatActivity(R.layout.activity_show_password) {

    private var fingerprintDialog: FingerprintDialogFragment? = null

    private val authSecurityRepository: AuthSecurityRepository by lazy {
        AuthSecurityRepository(
            FingerprintManagerCompat.from(this),
            KeyPairGeneratorSpec.Builder(this),
            getSharedPreferences(
                AuthSecurityRepository.CRYPTO_PREFERENCE_FILENAME,
                Context.MODE_PRIVATE
            ),
            packageName
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSupportActionBar()
        if (authSecurityRepository.isFingerprintConfigured()) {
            FingerprintDialogFragment().let {
                fingerprintDialog = it
                it.show(supportFragmentManager, null)
            }
        }
        pinEditText.addTextChangedListener {
            if (it.toString().length == AuthSecurityRepository.PIN_LENGTH) {
                Single.fromCallable {
                    authSecurityRepository.getPasswordByPin(it.toString()) ?: INCORRECT_PIN_MESSAGE
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(::showDialog, {})
            }
        }
        openFingerprintDialog.setOnClickListener {
            fingerprintDialog?.show(supportFragmentManager, null)
        }
        dropPasswordButton.setOnClickListener {
            Completable.fromCallable {
                authSecurityRepository.dropPassword()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { finish() }
        }
    }

    private fun initSupportActionBar() {
        setTitle(R.string.title_show_password)
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

    private fun showDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .show()
    }
}