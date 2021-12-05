package com.ustadmobile.lib.rest

import com.turn.ttorrent.client.FileMetadataProvider
import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.contentjob.ContentJobManager
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.contentjob.ContentPluginIds.CONTAINER_TORRENT_DOWNLOAD_PLUGIN
import com.ustadmobile.core.util.DiTag
import com.ustadmobile.lib.db.entities.ContentJob
import com.ustadmobile.lib.db.entities.ContentJobItem
import com.ustadmobile.lib.rest.ext.dbModeToEndpoint
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import org.kodein.di.on
import java.io.File
import java.lang.Exception

fun Route.TorrentFileRoute(){

    route("containers") {

        get("{containerUid}"){
            val torrentDir: File by closestDI().on(call).instance(tag = DiTag.TAG_TORRENT_DIR)
            val containerUid = call.parameters["containerUid"]
            val torrentFile = File(torrentDir, "$containerUid.torrent")
            call.respondFile(torrentFile)
        }

        post("{containerUid}/{contentEntryUid}/upload"){

            val torrentDir: File by closestDI().on(call).instance(tag = DiTag.TAG_TORRENT_DIR)
            val contentJobManager: ContentJobManager by closestDI().instance()
            val containerFolder: File by closestDI().on(call).instance(tag = DiTag.TAG_DEFAULT_CONTAINER_DIR)
            val db: UmAppDatabase by closestDI().on(call).instance(tag = UmAppDatabase.TAG_DB)
            val endpoint = call.application.environment.config.dbModeToEndpoint(call = call)

            val containerUid = call.parameters["containerUid"]?.toLong() ?: 0L
            val contentEntryUid = call.parameters["contentEntryUid"]?.toLong() ?: 0L

            val torrentFile = File(torrentDir, "$containerUid.torrent")

            try {
                withContext(Dispatchers.IO) {
                    call.receiveMultipart().forEachPart {
                        when(it){
                            is PartData.FileItem ->{
                                it.streamProvider.invoke().use { stream ->
                                    torrentFile.writeBytes(stream.readBytes())
                                }
                                it.dispose
                            }
                        }
                    }
                }
            }catch(e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Upload error: $e")
            }

            val metadataProvider = FileMetadataProvider(torrentFile.absolutePath)
            val fileSize = metadataProvider.torrentMetadata.files.sumOf { it.size }

            val job = ContentJob().apply {
                toUri = containerFolder.toURI().toString()
                cjIsMeteredAllowed = true
                cjUid = db.contentJobDao.insertAsync(this)
            }

            ContentJobItem().apply {
                cjiJobUid = job.cjUid
                cjiContainerUid = containerUid
                sourceUri = "content://${contentEntryUid}"
                cjiContentEntryUid = contentEntryUid
                cjiPluginId = CONTAINER_TORRENT_DOWNLOAD_PLUGIN
                cjiItemTotal = fileSize
                cjiRecursiveTotal = fileSize
                cjiIsLeaf = true
                cjiConnectivityNeeded = false
                cjiUid = db.contentJobItemDao.insertJobItem(this)
            }

            contentJobManager.enqueueContentJob(endpoint, job.cjUid)
            call.respond(job.cjUid)

        }

        get("{jobUid}/status"){

            val db: UmAppDatabase by closestDI().on(call).instance(tag = UmAppDatabase.TAG_DB)
            val jobUid: Long = call.parameters["jobUid"]?.toLongOrNull() ?: 0L
            val jobItem: ContentJobItem? = db.contentJobItemDao.findByJobId(jobUid)

            with(call) {
                if (jobItem != null) {
                    respond(jobItem)
                }else{
                    respond(HttpStatusCode.NotFound)
                }
            }

        }

        delete("{jobId}/cancel"){

            val jobUid: Long = call.parameters["jobId"]?.toLongOrNull() ?: 0L
            val endpoint = call.application.environment.config.dbModeToEndpoint(call = call)
            val contentJobManager: ContentJobManager by closestDI().instance()
            contentJobManager.cancelContentJob(endpoint, jobUid)
            call.respond(HttpStatusCode.OK)
        }



    }
}