package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.*

@UmEntity
@Entity
class FeedEntry()  {

    @UmPrimaryKey
    @PrimaryKey
    var feedEntryUid: Long = 0

    var feedEntryPersonUid: Long = 0

    var title: String? = null

    var description: String? = null

    var link: String? = null

    var feedEntryClazzName: String? = null

    /**
     * Get the deadline (in utime) by which the entry should be actioned (or 0 for no deadline)
     *
     * @return the deadline (in utime) by which the entry should be actioned (or 0 for no deadline)
     */
    /**
     * Set the deadline (in utime) by which the entry should be actioned (or 0 for no deadline)
     *
     * @param deadline the deadline (in utime) by which the entry should be actioned (or 0 for no deadline)
     */
    var deadline: Long = 0

    /**
     * Get the feed entry hash. Feed entries may be generated simultaneously on multiple devices.
     * The same feed entry must have the same feedEntryHash - e.g. to take attendance for a given class
     * for a given day.
     *
     * @return the feed entry hash as above
     */
    /**
     * Set the feed entry hash
     *
     * @see .getFeedEntryHash
     * @param feedEntryHash the feed entry hash
     */
    var feedEntryHash: Long = 0

    var feedEntryDone: Boolean = false

    var feedEntryClazzLogUid: Long = 0

    var dateCreated: Long = 0

    /**
     * As per ScheduledCheck.TYPE_ constants
     */
    var feedEntryCheckType: Int = 0

    @UmSyncLocalChangeSeqNum
    var feedEntryLocalChangeSeqNum: Long = 0

    @UmSyncMasterChangeSeqNum
    var feedEntryMasterChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var feedEntryLastChangedBy: Int = 0


    constructor(feedEntryUid: Long, title: String, description: String, link: String,
                feedEntryClazzName: String, personUid: Long) : this() {
        this.feedEntryUid = feedEntryUid
        this.title = title
        this.description = description
        this.link = link
        this.feedEntryClazzName = feedEntryClazzName
        this.feedEntryPersonUid = personUid
        this.dateCreated = 0
        //TODO: date created bit
    }

}
