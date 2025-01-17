package com.ustadmobile.core.util.ext

/**
 * Convenience extension function. Where the receiver is being used to hold
 * bitmask values, check if the given flag is set
 *
 * @param a bitmask flag
 * @receiver Long being used to store bitmask values
 *
 * @return true if the flag is set, false otherwise
 */

fun Long.hasFlag(flag: Long): Boolean {
    return (this and flag) == flag
}

