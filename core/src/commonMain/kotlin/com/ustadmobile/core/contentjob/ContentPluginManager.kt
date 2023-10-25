package com.ustadmobile.core.contentjob

import com.ustadmobile.door.DoorUri
import kotlinx.coroutines.CancellationException

class ContentPluginManager(val pluginList: List<ContentImportPlugin>) {

    val supportedMimeTypeList: List<String>

    init{
        val pluginDuplicates = pluginList.filter { plugin ->
            pluginList.count { it.pluginId == plugin.pluginId } > 1
        }
        if(pluginDuplicates.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate pluginIds in: ${pluginDuplicates.joinToString()}")
        }
        supportedMimeTypeList = pluginList.flatMap { it.supportedMimeTypes }.toSet().toList()
    }

    fun isMimeTypeSupported(mimeType: String): Boolean {
        return supportedMimeTypeList.contains(mimeType)
    }


    fun requirePluginById(id: Int) : ContentImportPlugin {
        return pluginList.find { it.pluginId == id } ?: throw FatalContentJobException("invalid pluginId")
    }

    fun getPluginById(id: Int): ContentImportPlugin? {
        return pluginList.firstOrNull { it.pluginId == id }
    }

    suspend fun extractMetadata(uri: DoorUri): MetadataResult {
        pluginList.forEach {
            try {
                return it.extractMetadata(uri) ?: return@forEach
            }catch (e: Exception){
                if(e is CancellationException){
                    throw e
                }
                e.printStackTrace()
            }
        }
        throw ContentTypeNotSupportedException("no contentType plugin support found")
    }

}