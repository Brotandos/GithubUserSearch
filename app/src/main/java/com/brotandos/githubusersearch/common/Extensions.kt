package com.brotandos.githubusersearch.common

import android.app.Activity
import android.content.Intent

const val NO_FLAGS = -1

inline fun <reified T : Activity> Activity.startActivity(
    needToFinishCurrent: Boolean = false,
    flags: Int = NO_FLAGS
) {
    val intent = Intent(this, T::class.java)
    if (flags != NO_FLAGS) intent.addFlags(flags)
    startActivity(intent)
    if (needToFinishCurrent) finish()
}