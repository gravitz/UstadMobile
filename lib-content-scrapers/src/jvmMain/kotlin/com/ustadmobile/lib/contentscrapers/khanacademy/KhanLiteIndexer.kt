package com.ustadmobile.lib.contentscrapers.khanacademy

import ScraperTypes.KHAN_LITE_VIDEO_SCRAPER
import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil
import com.ustadmobile.lib.contentscrapers.ScraperConstants
import com.ustadmobile.lib.contentscrapers.UMLogUtil
import com.ustadmobile.lib.contentscrapers.abztract.ScraperException
import com.ustadmobile.lib.contentscrapers.abztract.SeleniumIndexer
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ScrapeQueueItem
import org.jsoup.nodes.Document
import org.kodein.di.DI
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import java.net.URL


class KhanLiteIndexer(parentContentEntry: Long, runUid: Int, sqiUid: Int, contentEntryUid: Long, endpoint: Endpoint, di: DI) : SeleniumIndexer(parentContentEntry, runUid, sqiUid, contentEntryUid, endpoint, di) {

    override fun indexUrl(sourceUrl: String) {

        val khanEntry = getKhanEntry(englishLang, repo.contentEntryDao)

        ContentScraperUtil.insertOrUpdateParentChildJoin(repo.contentEntryParentChildJoinDao, parentContentEntry, khanEntry, 12)

        val lang = sourceUrl.substringBefore(".khan").substringAfter("://")

        val khanLang = KhanConstants.khanLiteMap[lang]
                ?: throw ScraperException(0, "Do not have support for lite language: $lang")

        val parentEntry = createKangLangEntry(if (lang == "www") "en" else lang, khanLang.title, khanLang.url, db)

        ContentScraperUtil.insertOrUpdateParentChildJoin(repo.contentEntryParentChildJoinDao, khanEntry, parentEntry, 0)

        val document: Document
        try {
            document = startSeleniumIndexer(sourceUrl) {
                it.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#library-content-main")))
            }
        }catch (e: Exception){
            throw e
        }

        val fullList = document.select("div#library-content-main div[data-role=page]")

        fullList.forEachIndexed { count, element ->

            val header = element.selectFirst("div.library-content-header h2")?.text() ?: ""

            if (header.isNullOrEmpty()) {
                UMLogUtil.logError("page had a missing header text for count $count for url $sourceUrl")
                return@forEachIndexed
            }

            val description = element.select("div.library-content-list p.topic-desc")?.text() ?: ""

            val headerEntry = ContentScraperUtil.createOrUpdateContentEntry(header, header, header,
                    ScraperConstants.KHAN, ContentEntry.LICENSE_TYPE_CC_BY_NC,
                    parentEntry.primaryLanguageUid, parentEntry.languageVariantUid,
                    description, false, "", "",
                    "", "",
                    0, repo.contentEntryDao)

            ContentScraperUtil.insertOrUpdateParentChildJoin(repo.contentEntryParentChildJoinDao, parentEntry, headerEntry, count)

            val contentList = element.select("div.library-content-list li.subjects-row-first td a.subject-link")

            contentList.forEachIndexed { contentCount, contentElement ->

                val href = contentElement.attr("href")
                val title = contentElement.text()

                val contentUrl = URL(URL(sourceUrl), href)

                val contentId = contentUrl.toString().substringAfter("v=")

                if (contentId.isNullOrEmpty()) {
                    UMLogUtil.logError("no Content Id found for element $title  with href $href in heading $header on url $sourceUrl")
                    return@forEachIndexed
                }

                val entry = ContentScraperUtil.createOrUpdateContentEntry(contentId, title,
                        ScraperConstants.KHAN_PREFIX + contentId, ScraperConstants.KHAN,
                        ContentEntry.LICENSE_TYPE_CC_BY_NC, parentEntry.primaryLanguageUid,
                        parentEntry.languageVariantUid, "", true,
                        "", "",
                        "", "",
                        ContentEntry.TYPE_VIDEO, repo.contentEntryDao)

                ContentScraperUtil.insertOrUpdateParentChildJoin(repo.contentEntryParentChildJoinDao, headerEntry, entry, contentCount)

                createQueueItem(contentUrl.toString(), entry, KHAN_LITE_VIDEO_SCRAPER, ScrapeQueueItem.ITEM_TYPE_SCRAPE, headerEntry.contentEntryUid)

            }

        }

        setIndexerDone(true, 0)

    }


    override fun close() {

    }
}