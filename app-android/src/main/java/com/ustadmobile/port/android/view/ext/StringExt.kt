package com.ustadmobile.port.android.view.ext

import android.content.Context
import android.view.View
import com.toughra.ustadmobile.R

fun String?.visibleIfNotNullOrEmpty() = if(!this.isNullOrEmpty()) View.VISIBLE else View.GONE

fun String?.visibleIfNullOrEmpty() = if(!this.isNullOrEmpty()) View.GONE else View.INVISIBLE

fun optional(context: Context, field: String): String{
    return field + " (" + context.getString(R.string.optional)  + ")"
}
