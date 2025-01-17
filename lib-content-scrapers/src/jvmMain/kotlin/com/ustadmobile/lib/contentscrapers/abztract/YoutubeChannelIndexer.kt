package com.ustadmobile.lib.contentscrapers.abztract

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.io.ext.readString
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil
import com.ustadmobile.lib.contentscrapers.UMLogUtil
import com.ustadmobile.lib.contentscrapers.util.YoutubeData
import com.ustadmobile.lib.db.entities.ScrapeQueueItem
import org.kodein.di.DI
import java.io.File
import java.io.IOException
import java.lang.Exception
import kotlin.concurrent.withLock
import kotlin.math.pow
import kotlin.random.Random
import kotlin.system.exitProcess


class YoutubeChannelIndexer(parentContentEntryUid: Long, runUid: Int, sqiUid: Int, contentEntryUid: Long, endpoint: Endpoint, di: DI) : Indexer(parentContentEntryUid, runUid, sqiUid, contentEntryUid, endpoint, di ) {


    private val ytPath: String
    private val gson: Gson


    init {
        ContentScraperUtil.checkIfPathsToDriversExist()
        ytPath = System.getProperty(ContentScraperUtil.YOUTUBE_DL_PATH_KEY)
        gson = GsonBuilder().disableHtmlEscaping().create()
    }

    override fun indexUrl(sourceUrl: String) {

        val ytExeFile = File(ytPath)
        if (!ytExeFile.exists()) {
            close()
            setIndexerDone(false, Scraper.ERROR_TYPE_MISSING_EXECUTABLE)
            throw IOException("Webp executable does not exist: $ytPath")
        }

        val builder = ProcessBuilder(ytPath, "--retries", "1",
                "-J", "-i", "--flat-playlist", sourceUrl)

        var data: String? = null
        YoutubeScraper.youtubeLocker.withLock {
            UMLogUtil.logTrace("starting youtube lock channel")
            var retryFlag = true
            var numberOfFailures = 1
            while (retryFlag) {

                var process: Process? = null
                try {
                    Thread.sleep(Random.nextLong(10000, 30000))
                    process = builder.start()
                    process!!.waitFor()
                    data = process.inputStream.readString()
                    val exitValue = process.exitValue()
                    if (exitValue != 0) {
                        val error = process.errorStream.readString()
                        UMLogUtil.logError("Error Stream for src $sourceUrl with error code  $error")
                        if (!error.contains("429")) {
                            throw ScraperException(Scraper.ERROR_TYPE_UNKNOWN_YOUTUBE, "unknown error: $error")
                        }
                        throw IOException("Failed $numberOfFailures for  $sourceUrl")
                    }
                    retryFlag = false
                } catch (s: ScraperException) {
                    setIndexerDone(false, Scraper.ERROR_TYPE_UNKNOWN_YOUTUBE)
                    close()
                    throw s
                } catch (e: Exception) {
                    if (numberOfFailures > 5) {
                        setIndexerDone(false, Scraper.ERROR_TYPE_YOUTUBE_ERROR)
                        exitProcess(1)
                    }

                    YoutubeScraper.lockedUntil = YoutubeScraper.baseRetry.pow(numberOfFailures) * 1000
                    UMLogUtil.logError("caught youtube exception with lockedUntil value of ${YoutubeScraper.lockedUntil.toLong()}")
                    Thread.sleep(YoutubeScraper.lockedUntil.toLong())

                    numberOfFailures++
                } finally {
                    process?.destroy()
                }

            }

        }
        UMLogUtil.logTrace("ending youtube lock channel")

        if(data == null){
            setIndexerDone(false, 0)
            close()
            throw ScraperException(0, "No Data Found after running youtube-dl")
        }

        val youtubeData = gson.fromJson(data, YoutubeData::class.java)
        youtubeData.entries?.forEachIndexed{ counter, entry ->

            if (entry.url == null) {
                return@forEachIndexed
            }

            val playlistEntry = ContentScraperUtil.insertTempYoutubeContentEntry(
                    repo.contentEntryDao, entry.url!!,
                    parentContentEntry?.primaryLanguageUid?: 0, "",
                    parentContentEntry?.publisher?: "", parentContentEntry?.licenseType?: 0,
                    parentContentEntry?.languageVariantUid?: 0)

            ContentScraperUtil.insertOrUpdateParentChildJoin(repo.contentEntryParentChildJoinDao, parentContentEntry!!, playlistEntry, counter)

            createQueueItem(entry.url!!, playlistEntry, ScraperTypes.YOUTUBE_PLAYLIST_INDEXER, ScrapeQueueItem.ITEM_TYPE_INDEX, parentContentEntryUid)
        }

        setIndexerDone(true, 0)

    }

    override fun close() {

    }


}