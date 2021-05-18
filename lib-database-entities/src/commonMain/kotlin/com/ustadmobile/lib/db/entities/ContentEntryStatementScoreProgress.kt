package com.ustadmobile.lib.db.entities

import kotlinx.serialization.Serializable

@Serializable
class ContentEntryStatementScoreProgress {

    var resultScore: Int = 0

    var resultMax: Int = 0

    var contentComplete: Boolean = false

    var progress: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ContentEntryStatementScoreProgress

        if (resultScore != other.resultScore) return false
        if (resultMax != other.resultMax) return false
        if (contentComplete != other.contentComplete) return false
        if (progress != other.progress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resultScore
        result = 31 * result + resultMax
        result = 31 * result + contentComplete.hashCode()
        result = 31 * result + progress
        return result
    }


}