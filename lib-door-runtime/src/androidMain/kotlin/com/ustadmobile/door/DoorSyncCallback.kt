package com.ustadmobile.door

import androidx.sqlite.db.SupportSQLiteDatabase

interface DoorSyncCallback {

    /**
     *
     */
    fun initSyncablePrimaryKeys(supportDb: SupportSQLiteDatabase)

}