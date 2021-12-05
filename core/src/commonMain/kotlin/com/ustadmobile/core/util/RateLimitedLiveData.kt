package com.ustadmobile.core.util

import com.ustadmobile.core.util.ext.addInvalidationListener
import com.ustadmobile.core.util.ext.removeInvalidationListener
import com.ustadmobile.door.ChangeListenerRequest
import com.ustadmobile.door.DoorDatabase
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.door.util.systemTimeInMillis
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.Volatile

/**
 * RateLimitedLiveData can listen for changes to a list of tables and restrict refreshes to a
 * given interval. This is designed for situations where we want to use LiveData to display
 * updates on the UI, but don't want to waste resources by re-running a query too frequently. This
 * could be an issue if a table is frequently updated by multiple threads/coroutines.
 *
 * RateLimitedLiveData will only listen for table changes when the LiveData itself has active
 * observers.
 */
class RateLimitedLiveData<T>(
    private val db: DoorDatabase,
    tableNames: List<String>,
    private val interval: Long = 1000,
    private val getter: suspend () -> T
): DoorMutableLiveData<T>() {

    private val changeListenerRequest = ChangeListenerRequest(tableNames) { invalidate() }

    @Volatile
    private var lastCheckTime: Long = 0

    private val refreshCheckJob = atomic<Job?>(null)

    private fun invalidate() {
        val timeNow = systemTimeInMillis()
        if((timeNow - lastCheckTime) >= interval) {
            GlobalScope.launch {
                refresh()
            }
        }else if(refreshCheckJob.value == null){
            refreshCheckJob.value = GlobalScope.launch {
                delay((lastCheckTime + interval) - timeNow)
                refresh()
            }
        }
    }

    private suspend fun refresh() {
        refreshCheckJob.value = null
        lastCheckTime = systemTimeInMillis()
        sendValue(getter())
    }

    override fun onActive2() {
        super.onActive2()
        db.addInvalidationListener(changeListenerRequest)
        invalidate()
    }

    override fun onInactive2() {
        super.onInactive2()
        db.removeInvalidationListener(changeListenerRequest)
    }
}