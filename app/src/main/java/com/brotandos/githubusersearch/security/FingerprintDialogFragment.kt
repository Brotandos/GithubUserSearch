package com.brotandos.githubusersearch.security

import android.content.Context
import android.os.Bundle
import android.security.KeyPairGeneratorSpec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.brotandos.githubusersearch.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.cancelButton

class FingerprintDialogFragment : BottomSheetDialogFragment() {

    private lateinit var disposable: Disposable

    private val authSecurityRepository: AuthSecurityRepository by lazy {
        val context = requireContext()
        AuthSecurityRepository(
            FingerprintManagerCompat.from(context),
            KeyPairGeneratorSpec.Builder(context),
            context.getSharedPreferences(
                AuthSecurityRepository.CRYPTO_PREFERENCE_FILENAME,
                Context.MODE_PRIVATE
            ),
            context.packageName
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_fingerprint_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancelButton.setOnClickListener { dismiss() }
        disposable = authSecurityRepository.listenToFingerprint()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    when (it) {
                        is FingerprintStatus.Authenticated -> showPassword(it.password)
                        is FingerprintStatus.Help -> toast(it.message)
                        is FingerprintStatus.Error -> toast(it.message)
                        is FingerprintStatus.Failed -> toast("Failed to read fingerprint")
                    }
                },
                {}
            )
    }

    private fun toast(message: String) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    private fun showPassword(password: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_password)
            .setMessage(password)
            .show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.dispose()
    }
}