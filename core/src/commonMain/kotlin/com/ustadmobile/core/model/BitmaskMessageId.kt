package com.ustadmobile.core.model

import com.ustadmobile.core.util.ext.hasFlag

data class BitmaskMessageId(val flagVal: Long, val messageId: Int) {
    fun toBitmaskFlag(value: Long) = BitmaskFlag(flagVal, messageId, value.hasFlag(flagVal))
}
