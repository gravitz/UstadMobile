package com.ustadmobile.lib.contentscrapers.khanacademy

import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil
import com.ustadmobile.lib.contentscrapers.ScraperConstants
import com.ustadmobile.lib.contentscrapers.ScraperConstants.EMPTY_STRING
import com.ustadmobile.lib.contentscrapers.abztract.Indexer
import com.ustadmobile.lib.contentscrapers.khanacademy.KhanConstants.khanFullMap
import com.ustadmobile.lib.contentscrapers.khanacademy.KhanConstants.khanLiteMap
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ScrapeQueueItem
import org.kodein.di.DI

@ExperimentalStdlibApi
class KhanFrontPageIndexer(parentContentEntry: Long, runUid: Int, sqiUid: Int, contentEntryUid: Long, endpoint: Endpoint, di: DI) : Indexer(parentContentEntry, runUid, sqiUid, contentEntryUid, endpoint, di) {

    private lateinit var parentEntry: ContentEntry

    override fun indexUrl(sourceUrl: String) {

        parentEntry = ContentScraperUtil.createOrUpdateContentEntry("https://www.khanacademy.org/", "Khan Academy",
                sourceUrl, ScraperConstants.KHAN, ContentEntry.LICENSE_TYPE_CC_BY_NC, englishLang.langUid, null,
                "You can learn anything.\n" + "For free. For everyone. Forever.", false, EMPTY_STRING,
                "https://cdn.kastatic.org/images/khan-logo-dark-background.new.png",
                EMPTY_STRING, EMPTY_STRING, 0, contentEntryDao)

        ContentScraperUtil.insertOrUpdateParentChildJoin(contentEntryParentChildJoinDao, masterRootParent, parentEntry, 12)

        khanLiteMap.values.forEach{
            createQueueItem(it.url, parentEntry, ScraperTypes.KHAN_LITE_INDEXER,  ScrapeQueueItem.ITEM_TYPE_INDEX, masterRootParent.contentEntryUid)
        }

        khanFullMap.values.forEach{
            createQueueItem(it.url, parentEntry, ScraperTypes.KHAN_FULL_INDEXER,  ScrapeQueueItem.ITEM_TYPE_INDEX, masterRootParent.contentEntryUid)
        }

        setIndexerDone(true, 0)


    }

    override fun close() {

    }

}