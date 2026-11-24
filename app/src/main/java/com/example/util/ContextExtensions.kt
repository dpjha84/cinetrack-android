package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Safely unwraps any ContextWrapper (e.g. Compose ContextThemeWrapper) to find the host Activity.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
