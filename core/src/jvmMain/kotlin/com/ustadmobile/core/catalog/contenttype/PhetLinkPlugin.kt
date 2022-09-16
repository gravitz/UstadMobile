package com.ustadmobile.core.catalog.contenttype

import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.contentjob.*
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.io.ext.isRemote
import com.ustadmobile.core.util.DiTag
import com.ustadmobile.core.util.ext.updateTotalFromLocalUriIfNeeded
import com.ustadmobile.core.view.PhetLinkContentView
import com.ustadmobile.door.DoorUri
import com.ustadmobile.door.ext.DoorTag
import com.ustadmobile.door.ext.toFile
import com.ustadmobile.lib.db.entities.*
import io.ktor.client.*
import io.ktor.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.on
import java.io.File
import java.util.*

class PhetLinkPlugin(
    private var context: Any,
    private val endpoint: Endpoint,
    override val di: DI,
    private val uploader: ContentPluginUploader = DefaultContentPluginUploader(di)
): ContentPlugin {

    val viewName: String
        get() = PhetLinkContentView.VIEW_NAME

    override val pluginId: Int
        get() = PLUGIN_ID

    override val supportedMimeTypes: List<String>
        get() = SupportedContent.PHET_LINK

    override val supportedFileExtensions: List<String>
        get() = TODO("not implemented yet")

    private val httpClient: HttpClient = di.direct.instance()

    private val repo: UmAppDatabase by di.on(endpoint).instance(tag = DoorTag.TAG_REPO)

    private val db: UmAppDatabase by di.on(endpoint).instance(tag = DoorTag.TAG_DB)

    private val defaultContainerDir: File by di.on(endpoint).instance(tag = DiTag.TAG_DEFAULT_CONTAINER_DIR)

    override suspend fun extractMetadata(
        uri: DoorUri,
        process: ContentJobProcessContext
    ): MetadataResult? {
        if(!uri.toString().startsWith("https://phet.colorado.edu/"))
            return null

        val localUri = process.getLocalOrCachedUri()
        val downloadedFile: File = localUri.toFile()
        val fileContent = downloadedFile.readText()
        val htmlContent = Jsoup.parse(fileContent)

        val headTags: Elements = htmlContent.getElementsByTag("head")
        val img: String?  =
            headTags.select("meta[property=og:image]").first()?.attr("content");

        return MetadataResult(ContentEntryWithLanguage().apply {
            author = "PhET"
            publisher = "PhET"

            val title: String?  =
                headTags.select("meta[property=og:title]").first()?.attr("content");
            this.title = title

            val footerTags: Elements = htmlContent.getElementsByTag("footer")
            val languageCode: String?  =
                footerTags.select("option[selected]").first()?.attr("value");
            if (!languageCode.equals(null)) {
                this.language = Language().apply {
                    iso_639_1_standard = languageCode
                }
            }

            val description: String? = headTags.select("meta[name=description]").first()?.attr("content")
            this.description = description

        }, pluginId, img)
    }

    override suspend fun processJob(
        jobItem: ContentJobItemAndContentJob,
        process: ContentJobProcessContext,
        progress: ContentJobProgressListener): ProcessResult {
        val contentJobItem = jobItem.contentJobItem ?: throw IllegalArgumentException("mising job item")
        val uri = contentJobItem.sourceUri ?: return ProcessResult(JobStatus.FAILED)
        return withContext(Dispatchers.Default) {

            try {

                val doorUri = DoorUri.parse(uri)
                val localUri = process.getLocalOrCachedUri()
                val contentNeedUpload = !doorUri.isRemote()
                contentJobItem.updateTotalFromLocalUriIfNeeded(localUri, contentNeedUpload,
                    progress, context, di)

                if(!contentJobItem.cjiContainerProcessed) {

//                    val container = db.containerDao.findByUid(contentJobItem.cjiContainerUid)
//                        ?: Container().apply {
//                            containerContentEntryUid = contentJobItem.cjiContentEntryUid
//                            cntLastModified = System.currentTimeMillis()
//                            mimeType = supportedMimeTypes.first()
//                            containerUid = repo.containerDao.insertAsync(this)
//                        }
//
//                    val containerFolder = jobItem.contentJob?.toUri
//                        ?: defaultContainerDir.toURI().toString()
//                    val containerFolderUri = DoorUri.parse(containerFolder)
//
//                    contentJobItem.cjiContainerUid = container.containerUid
//                    db.contentJobItemDao.updateContentJobItemContainer(contentJobItem.cjiUid,
//                        container.containerUid)
//
//                    repo.addEntriesToContainerFromZip(container.containerUid,
//                        localUri,
//                        ContainerAddOptions(storageDirUri = containerFolderUri), context)
//
//                    contentJobItem.updateTotalFromContainerSize(contentNeedUpload, db,
//                        progress)
//
//                    db.contentJobItemDao.updateContainerProcessed(contentJobItem.cjiUid, true)
//
//                    contentJobItem.cjiConnectivityNeeded = true
//                    db.contentJobItemDao.updateConnectivityNeeded(contentJobItem.cjiUid, true)
//
//                    val haveConnectivityToContinueJob = db.contentJobDao.isConnectivityAcceptableForJob(jobItem.contentJob?.cjUid
//                        ?: 0)
//                    if (!haveConnectivityToContinueJob) {
//                        return@withContext ProcessResult(JobStatus.WAITING_FOR_CONNECTION)
//                    }
                }

//                if(contentNeedUpload) {
//                    val progressListenerAdapter = NetworkProgressListenerAdapter(progress,
//                        contentJobItem)
//                    return@withContext ProcessResult(uploader.upload(
//                        contentJobItem, progressListenerAdapter, httpClient, endpoint, process
//                    ))
//                }

                return@withContext ProcessResult(JobStatus.COMPLETE)

            }catch (c: CancellationException){
                throw c
            }
        }
    }

    companion object {
        const val PLUGIN_ID = 102

    }

}