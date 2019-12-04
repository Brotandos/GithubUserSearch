package com.brotandos.githubusersearch.security

sealed class FingerprintStatus {
    data class Authenticated(val password: String) : FingerprintStatus()
    data class Help(val message: String) : FingerprintStatus()
    data class Error(val message: String) : FingerprintStatus()
    object Failed : FingerprintStatus()
}