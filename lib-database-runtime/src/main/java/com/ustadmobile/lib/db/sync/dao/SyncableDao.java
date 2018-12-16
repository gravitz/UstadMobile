package com.ustadmobile.lib.db.sync.dao;

import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmOnConflictStrategy;
import com.ustadmobile.lib.database.annotation.UmRestAccessible;
import com.ustadmobile.lib.database.annotation.UmRestAuthorizedUidParam;
import com.ustadmobile.lib.database.annotation.UmSyncFindAllChanges;
import com.ustadmobile.lib.database.annotation.UmSyncFindLocalChanges;
import com.ustadmobile.lib.database.annotation.UmSyncFindUpdateable;
import com.ustadmobile.lib.database.annotation.UmSyncIncoming;
import com.ustadmobile.lib.database.annotation.UmSyncOutgoing;
import com.ustadmobile.lib.db.sync.SyncResponse;
import com.ustadmobile.lib.db.sync.UmSyncExistingEntity;

import java.util.List;

/**
 * A base interface for DAOs which support synchronization.
 *
 * @param <T> The Entity Type
 * @param <D> The DAO Type (generally the DAO class that is implementing SyncableDao)
 */
public interface SyncableDao<T, D> extends BaseDao<T> {

    /**
     * Sync with the other DAO. THe other DAO should be the same class
     *
     * @param otherDao
     * @param accountPersonUid
     * @param sendLimit the maximum number of changes to upload to the server at a time
     * @param receiveLimit the maximum number of changes to receive from the server at a time
     */
    @UmSyncOutgoing
    void syncWith(D otherDao, long accountPersonUid, int sendLimit, int receiveLimit);

    @UmSyncIncoming
    @UmRestAccessible
    SyncResponse<T> handleIncomingSync(List<T> incomingChanges, long fromLocalChangeSeqNum,
                                       long fromMasterChangeSeqNum, @UmRestAuthorizedUidParam long userId,
                                       int limit);

    @UmInsert(onConflict = UmOnConflictStrategy.REPLACE)
    void replaceList(List<T> entities);

    /**
     * Find all local changes using the local change sequence number
     *
     * @param fromLocalChangeSeqNum The number to find all local changes from (inclusive)
     * @param accountPersonUid The account performing the sync
     * @return List of entities that have been locally modified since fromLocalChangeSeqNum
     */
    @UmSyncFindLocalChanges
    List<T> findLocalChanges(long fromLocalChangeSeqNum, long toLocalChangeSeqNum, long accountPersonUid, int limit);

    /**
     * Find all changes made
     *
     * @param fromLocalChangeSeqNum  (inclusive)
     * @param toLocalChangeSeqNum (inclusive)
     * @param fromMasterChangeSeqNum (inclusive)
     * @param toMasterChangeSeqNum (inclusive)
     * @param accountPersonUid id of the user account that is running the sync
     * @param limit maximum number of entries to retrieve
     * @return
     */
    @UmSyncFindAllChanges
    List<T> syncFindAllChanges(long fromLocalChangeSeqNum, long toLocalChangeSeqNum,
                               long fromMasterChangeSeqNum, long toMasterChangeSeqNum,
                               @UmRestAuthorizedUidParam long accountPersonUid,
                               int limit);
    @UmSyncFindUpdateable
    List<UmSyncExistingEntity> syncFindExistingEntities(List<Long> primaryKeys, long accountPersonUid);

}
