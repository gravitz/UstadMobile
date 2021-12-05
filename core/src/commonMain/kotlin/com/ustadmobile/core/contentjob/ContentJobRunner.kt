package com.ustadmobile.core.contentjob

import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.io.ext.emptyRecursively
import com.ustadmobile.core.networkmanager.ConnectivityLiveData
import com.ustadmobile.core.util.EventCollator
import com.ustadmobile.core.util.createTemporaryDir
import com.ustadmobile.core.util.ext.deleteFilesForContentEntry
import com.ustadmobile.door.DoorObserver
import com.ustadmobile.door.DoorUri
import com.ustadmobile.door.doorMainDispatcher
import com.ustadmobile.door.ext.DoorTag
import com.ustadmobile.door.ext.concurrentSafeListOf
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.util.getSystemTimeInMillis
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.on
import kotlin.jvm.Volatile
import kotlin.math.min

/**
 * Runs a given ContentJob.
 */
class ContentJobRunner(
    val jobId: Long,
    val endpoint: Endpoint,
    override val di: DI,
    val numProcessors: Int = DEFAULT_NUM_PROCESSORS,
    val maxItemAttempts: Int = DEFAULT_NUM_RETRIES
) : DIAware, ContentJobProgressListener, DoorObserver<ConnectivityStatus?>{

    data class ContentJobResult(val status: Int)

    /**
     * Sending anything on this channel will result in one queue check. If there is an available
     * processor, one new item will be started.
     */
    private val checkQueueSignalChannel = Channel<Boolean>(Channel.UNLIMITED)

    private val activeJobItemIds = concurrentSafeListOf<Long>()

    private val db: UmAppDatabase by on(endpoint).instance(tag = DoorTag.TAG_DB)

    private val repo: UmAppDatabase by on(endpoint).instance(tag = DoorTag.TAG_REPO)

    private val contentPluginManager: ContentPluginManager by on(endpoint).instance()

    private val eventCollator = EventCollator(500, this::commitProgressUpdates)

    private val connectivityLiveData: ConnectivityLiveData by on(endpoint).instance()



    @ExperimentalCoroutinesApi
    private fun CoroutineScope.produceJobs() = produce<ContentJobItemAndContentJob> {
        var done : Boolean
        try {
            Napier.d("connectivity observer forever")
            withContext(doorMainDispatcher()) {
                connectivityLiveData.liveData.observeForever(this@ContentJobRunner)
            }

            do {
                Napier.d("waiting for signal to check queue")
                checkQueueSignalChannel.receive()
                val numProcessorsAvailable = numProcessors - activeJobItemIds.size
                Napier.d("num process available :$numProcessorsAvailable")
                if (numProcessorsAvailable > 0) {
                    //Check queue and filter out any duplicates that are being actively processed
                    val queueItems = db.contentJobItemDao.findNextItemsInQueue(jobId, numProcessors * 2).filter {
                        (it.contentJobItem?.cjiUid ?: 0) !in activeJobItemIds
                    }

                    val numJobsToAdd = min(numProcessorsAvailable, queueItems.size)
                    Napier.d("num of Jobs to add :$numJobsToAdd")

                    for (i in 0 until numJobsToAdd) {
                        val contentJobItemUid = queueItems[i].contentJobItem?.cjiUid ?: 0L
                        activeJobItemIds += contentJobItemUid
                        db.contentJobItemDao.updateItemStatus(contentJobItemUid, JobStatus.RUNNING)
                        send(queueItems[i])
                    }
                }


                done = db.contentJobItemDao.isJobDone(jobId)
                Napier.d("is job Done :$done")
            } while (!done)
        }catch(e: Exception) {
            Napier.d(e.stackTraceToString(), e)
        }finally {
            withContext(NonCancellable + doorMainDispatcher()) {
                connectivityLiveData.liveData.removeObserver(this@ContentJobRunner)
            }
            Napier.d("close produce job")
            close()
        }
    }

    private fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<ContentJobItemAndContentJob>) = launch {
        val tmpDir = createTemporaryDir("job-$id")
        Napier.d("created tempDir job-$id")

        for(item in channel) {
            val itemUri = item.contentJobItem?.sourceUri?.let { DoorUri.parse(it) } ?: continue

            val processContext = ContentJobProcessContext(itemUri, tmpDir, mutableMapOf(), di)
            println("Proessor #$id processing job #${item.contentJobItem?.cjiUid} attempt #${item.contentJobItem?.cjiAttemptCount}")

            var processResult: ProcessResult? = null
            var processException: Throwable? = null
            var mediatorObserver: DoorObserver<Pair<Int, Boolean>>?= null
            val mediatorLiveData = JobConnectivityLiveData(connectivityLiveData,
                    db.contentJobDao.findMeteredAllowedLiveData(
                            item.contentJob?.cjUid ?: 0))

            try {

                item.contentJobItem?.cjiUid?.let { db.contentJobItemDao.updateStartTimeForJob(it, getSystemTimeInMillis()) }

                val sourceUri = item.contentJobItem?.sourceUri?.let { DoorUri.parse(it) }
                    ?: throw IllegalArgumentException("ContentJobItem #${item.contentJobItem?.cjiUid} has no source uri!")

                var metadataResult: MetadataResult? = null
                if(item.contentJobItem?.cjiContentEntryUid == 0L) {
                    metadataResult = contentPluginManager.extractMetadata(sourceUri, processContext)
                        ?: throw FatalContentJobException("ContentJobItem #${item.contentJobItem?.cjiUid}: cannot extract metadata")
                    val contentEntryUid = repo.contentEntryDao.insertAsync(metadataResult.entry)
                    item.contentJobItem?.cjiContentEntryUid = contentEntryUid
                    db.contentJobItemDao.updateContentEntryUid(item.contentJobItem?.cjiUid ?: 0,
                        contentEntryUid)

                    if(item.contentJobItem?.cjiParentContentEntryUid != 0L){
                        ContentEntryParentChildJoin().apply {
                            cepcjParentContentEntryUid = item.contentJobItem?.cjiParentContentEntryUid ?: 0L
                            cepcjChildContentEntryUid = item.contentJobItem?.cjiContentEntryUid ?: 0L
                            cepcjUid = repo.contentEntryParentChildJoinDao.insert(this)
                        }
                    }
                }

                val pluginId = if(item.contentJobItem?.cjiPluginId == 0) {
                    metadataResult?.pluginId ?: contentPluginManager.extractMetadata(sourceUri,
                        processContext)?.pluginId ?:
                        throw FatalContentJobException("ContentJobItem #${item.contentJobItem?.cjiUid}: cannot determine pluginId")
                }else {
                    item.contentJobItem?.cjiPluginId ?: 0
                }

                val plugin = contentPluginManager.getPluginById(pluginId)

                val jobResult = async {
                     plugin.processJob(item, processContext, this@ContentJobRunner)
                }

                mediatorObserver = DoorObserver {
                    val state = it.first
                    val isMeteredAllowed = it.second

                    if(item.contentJobItem?.cjiConnectivityNeeded == true
                            && (state == ConnectivityStatus.STATE_DISCONNECTED ||
                                    !isMeteredAllowed && state == ConnectivityStatus.STATE_METERED)){
                        jobResult.cancel(ConnectivityCancellationException("connectivity not acceptable"))
                    }

                }

                withContext(doorMainDispatcher()){
                    mediatorLiveData.observeForever(mediatorObserver)
                }

                processResult = jobResult.await()

                db.contentJobItemDao.updateItemStatus(item.contentJobItem?.cjiUid ?: 0, processResult.status)
                db.contentJobItemDao.updateFinishTimeForJob(item.contentJobItem?.cjiUid ?: 0, systemTimeInMillis())
                println("Processor #$id completed job #${item.contentJobItem?.cjiUid}")
                delay(1000)
            }catch(e: Exception) {
                //something went wrong
                processException = e

                e.printStackTrace()
            }finally {
                withContext(NonCancellable) {
                    val finalStatus: Int = when {
                        processResult != null -> processResult.status
                        processException is FatalContentJobException -> JobStatus.FAILED
                        processException is ConnectivityCancellationException -> JobStatus.QUEUED
                        processException is CancellationException -> {
                            deleteFilesForContentEntry(
                                    item.contentJobItem?.cjiContentEntryUid ?: 0,
                                            di, endpoint)
                            JobStatus.CANCELED
                        }
                        (item.contentJobItem?.cjiAttemptCount ?: maxItemAttempts) >= maxItemAttempts -> JobStatus.FAILED
                        else -> JobStatus.QUEUED
                    }

                    db.contentJobItemDao.updateJobItemAttemptCountAndStatus(
                        item.contentJobItem?.cjiUid ?: 0,
                        (item.contentJobItem?.cjiAttemptCount ?: 0) + 1, finalStatus)
                    db.contentJobItemDao.updateFinishTimeForJob(item.contentJobItem?.cjiUid ?: 0, systemTimeInMillis())

                    activeJobItemIds -= (item.contentJobItem?.cjiUid ?: 0)
                    try{
                        tmpDir.emptyRecursively()
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                    println("Processor #$id sending check queue signal after finishing with #${item.contentJobItem?.cjiUid}")
                }

                withContext(NonCancellable + doorMainDispatcher()) {
                    mediatorObserver?.let { mediatorLiveData.removeObserver(it) }
                }


                if(processException is CancellationException)
                    throw processException

                checkQueueSignalChannel.send(true)
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Volatile
    private var jobItemProducer : ReceiveChannel<ContentJobItemAndContentJob>? = null

    override fun onProgress(contentJobItem: ContentJobItem) {
        GlobalScope.launch {
            eventCollator.send(contentJobItem.toProgressUpdate())
        }
    }

    private suspend fun commitProgressUpdates(updates: List<ContentJobItemProgressUpdate>) {
        db.contentJobItemDao.commitProgressUpdates(updates)
    }


    //This can be called using Worker (Android) or Quartz (JVM)
    suspend fun runJob(): ContentJobResult {
        withContext(Dispatchers.Default) {
            val producerVal = produceJobs().also {
                jobItemProducer = it
            }

            repeat(numProcessors) {
                Napier.d("launch processor $it")
                launchProcessor(it, producerVal)
            }

            Napier.d("run Job, send queue signal")
            checkQueueSignalChannel.send(true)
        }

        return ContentJobResult(JobStatus.COMPLETE)
    }

    override fun onChanged(t: ConnectivityStatus?) {
        GlobalScope.launch {
            if(t != null){
                checkQueueSignalChannel.send(true)
            }
        }
    }

    companion object {

        const val DEFAULT_NUM_PROCESSORS = 10

        const val DEFAULT_NUM_RETRIES = 5

    }
}