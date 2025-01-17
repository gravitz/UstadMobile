package com.ustadmobile.lib.contentscrapers.googledrive

import kotlinx.serialization.Serializable

@Serializable
class GoogleFile {

    var kind: String? = null

    var id: String? = null

    var name: String? = null

    var mimeType: String? = null

    var modifiedTime: String? = null

    var thumbnailLink: String? = null

    var description: String? = null

}