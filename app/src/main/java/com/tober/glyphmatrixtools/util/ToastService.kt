package com.tober.glyphmatrixtools.util

import android.content.Context
import android.widget.Toast

class ToastService(
    context: Context
) {
    private val context = context.applicationContext

    fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
