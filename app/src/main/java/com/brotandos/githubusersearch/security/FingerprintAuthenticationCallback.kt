package com.brotandos.githubusersearch.security

import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import io.reactivex.FlowableEmitter

class FingerprintAuthenticationCallback(
    private val emitter: FlowableEmitter<FingerprintStatus>,
    private val encryption: ByteArray
) : FingerprintManagerCompat.AuthenticationCallback() {

    override fun onAuthenticationHelp(helpMsgId: Int, helpMessage: CharSequence) {
        super.onAuthenticationHelp(helpMsgId, helpMessage)
        emitter.onNext(FingerprintStatus.Help(helpMessage.toString()))
    }

    override fun onAuthenticationError(errMsgId: Int, errorMessage: CharSequence) {
        super.onAuthenticationError(errMsgId, errorMessage)
        emitter.onNext(FingerprintStatus.Error(errorMessage.toString()))
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        emitter.onNext(FingerprintStatus.Failed)
    }

    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
        super.onAuthenticationSucceeded(result)
        result?.cryptoObject?.cipher?.let { cipher ->
            val password = String(cipher.doFinal(encryption))
            emitter.onNext(FingerprintStatus.Authenticated(password))
        }
    }
}