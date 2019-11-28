package com.brotandos.githubusersearch.common

import android.app.Activity
import android.content.Intent
import android.view.MotionEvent
import android.widget.EditText

const val NO_FLAGS = -1
const val DRAWABLE_RIGHT_INDEX = 2

inline fun <reified T : Activity> Activity.startActivity(
    needToFinishCurrent: Boolean = false,
    flags: Int = NO_FLAGS
) {
    val intent = Intent(this, T::class.java)
    if (flags != NO_FLAGS) intent.addFlags(flags)
    startActivity(intent)
    if (needToFinishCurrent) finish()
}

fun EditText.setClearable() {
    setOnTouchListener { v, event ->
        if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false
        val rightBoundsWidth = compoundDrawables[DRAWABLE_RIGHT_INDEX]
            ?.bounds
            ?.width()
            ?: return@setOnTouchListener false
        if (event.rawX < right - rightBoundsWidth) return@setOnTouchListener false
        setText("")
        true
    }
}