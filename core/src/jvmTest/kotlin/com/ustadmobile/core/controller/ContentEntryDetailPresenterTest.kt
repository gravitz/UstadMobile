package com.ustadmobile.core.controller

import com.nhaarman.mockitokotlin2.*
import com.ustadmobile.core.controller.ContentEntryListFragmentPresenter.Companion.ARG_CONTENT_ENTRY_UID
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.networkmanager.DownloadJobItemStatusProvider
import com.ustadmobile.core.networkmanager.downloadmanager.ContainerDownloadManager
import com.ustadmobile.core.util.GoToEntryFn
import com.ustadmobile.core.view.ContentEntryDetailView
import com.ustadmobile.core.view.ContentEntryListFragmentView
import com.ustadmobile.core.view.HomeView
import com.ustadmobile.door.DoorLifecycleObserver
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.DownloadJob
import com.ustadmobile.lib.db.entities.DownloadJobItem
import com.ustadmobile.util.test.checkJndiSetup
import io.ktor.util.Hash
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap

class ContentEntryDetailPresenterTest {

    private lateinit var mockView: ContentEntryDetailView
    private lateinit var statusProvider: DownloadJobItemStatusProvider

    private lateinit var umAppDatabase: UmAppDatabase

    private lateinit var umAppRepository: UmAppDatabase

    private lateinit var contentEntry: ContentEntry

    private lateinit var containerDownloadManager: ContainerDownloadManager

    private lateinit var systemImpl: UstadMobileSystemImpl

    private var args = HashMap<String, String>()

    private lateinit var downloadJobItemLiveData: DoorMutableLiveData<DownloadJobItem?>

    private lateinit var goToEntryFnCountDownLatch: CountDownLatch


    val counter: GoToEntryFn = { contentEntryUid: Long,
                                 umAppDatabase: UmAppDatabase,
                                 context: Any,
                                 systemImpl: UstadMobileSystemImpl,
                                 downloadRequired: Boolean,
                                 goToContentEntryDetailViewIfNotDownloaded: Boolean,
                                 noIframe: Boolean ->
        goToEntryFnCountDownLatch.countDown()
    }


    private val context = mock<DoorLifecycleOwner>() {
        on { currentState }.thenReturn(DoorLifecycleObserver.STARTED)
    } as Any

    @Before
    fun setUp() {
        checkJndiSetup()
        mockView = mock {
            on { runOnUiThread(ArgumentMatchers.any()) }.doAnswer { invocation ->
                Thread(invocation.getArgument<Any>(0) as Runnable).start()
                Unit
            }
        }
        statusProvider = mock()
        systemImpl = mock {
            on {
                getAppConfigString(AppConfig.KEY_LOGIN_REQUIRED_FOR_CONTENT_OPEN,
                        "false", context)
            }.thenReturn("false")
        }
        containerDownloadManager = mock()
        umAppDatabase = UmAppDatabase.getInstance(context)
        umAppRepository = umAppDatabase //for this test there is no difference

        contentEntry = ContentEntry()
        contentEntry.contentEntryUid = umAppDatabase.contentEntryDao.insert(contentEntry)

        var container = Container()
        container.containerContentEntryUid = contentEntry.contentEntryUid
        container.fileSize = 10
        container.containerUid = umAppDatabase.containerDao.insert(container)

        var dj = DownloadJobItem()
        dj.djiContainerUid = container.containerUid
        dj.djiContentEntryUid = contentEntry.contentEntryUid
        dj.djiStatus = JobStatus.COMPLETE
        umAppDatabase.downloadJobItemDao.insert(dj)

        downloadJobItemLiveData = DoorMutableLiveData(dj as DownloadJobItem?)

        runBlocking {
            whenever(containerDownloadManager.getDownloadJobItemByContentEntryUid(contentEntry.contentEntryUid)).thenReturn(downloadJobItemLiveData)
        }


        args[ContentEntryDetailPresenter.ARG_CONTENT_ENTRY_UID] = contentEntry.contentEntryUid.toString()
    }


    @Test
    fun givenEntryExists_whenOnCreateCalled_thenShouldSetContentEntryObserveDownloadJobAndSetTranslations() {


        val contentEntryLiveData = spy(DoorMutableLiveData(contentEntry as ContentEntry?))

        val repoContentEntryDaoSpy = spy(umAppRepository.contentEntryDao) {
            on { findLiveContentEntry(contentEntry.contentEntryUid) }.thenReturn(contentEntryLiveData)
        }

        val repoSpy = spy(umAppRepository) {
            on { contentEntryDao }.thenReturn(repoContentEntryDaoSpy)
        }

        var presenter = ContentEntryDetailPresenter(context, args, mockView,
                true, repoSpy, umAppDatabase,
                mock(), containerDownloadManager, null, systemImpl)
        presenter.onCreate(null)

        verify(mockView, timeout(5000)).setMainButtonEnabled(eq(true))
        verify(mockView, timeout(5000)).setDownloadSize(eq(10))
        verify(mockView, timeout(5000)).setAvailableTranslations(eq(listOf()))
        verify(contentEntryLiveData).observe(any(), any())

    }

    @Test
    fun givenContentEntryNotDownloaded_whenMainButtonClicked_thenShouldShowDownloadDialog() {

        runBlocking {
            var presenter = ContentEntryDetailPresenter(context, args, mockView,
                    true, umAppRepository, umAppDatabase,
                    mock(), containerDownloadManager, null, systemImpl, counter)
            presenter.onCreate(null)

            presenter.handleDownloadButtonClick()
            verify(mockView, timeout(5000)).showDownloadOptionsDialog(any())
        }


    }

    @Test
    fun givenContentEntryDownloaded_whenMainButtonClicked_thenShouldInvokeGoToContentEntryFunc() {

        goToEntryFnCountDownLatch = CountDownLatch(1)

        var presenter = ContentEntryDetailPresenter(context, args, mockView,
                true, umAppRepository, umAppDatabase,
                mock(), containerDownloadManager, null, systemImpl, counter)
        presenter.onCreate(null)

        verify(mockView, timeout(5000)).setDownloadJobItemStatus(any())
        presenter.handleDownloadButtonClick()
        goToEntryFnCountDownLatch.await()
        Assert.assertEquals("Go to ContentEntryFunc was called", 0, goToEntryFnCountDownLatch.count)

    }

    @Test
    fun givenTranslatedEntryExists_whenTranslationClicked_thenShouldGoToTranslatedContentEntryDetail() {

        var presenter = ContentEntryDetailPresenter(context, args, mockView,
                true, umAppRepository, umAppDatabase,
                mock(), containerDownloadManager, null, systemImpl, counter)
        presenter.onCreate(null)

        var args = mapOf(ContentEntryDetailPresenter.ARG_CONTENT_ENTRY_UID to 43L.toString())

        presenter.handleClickTranslatedEntry(43L)
        verify(systemImpl).go(eq(ContentEntryDetailView.VIEW_NAME), eq(args), any())

    }


    @Test
    fun givenUserIsInDetailViewFromListView_WhenUpNavigationCalled_thenShouldReturnToListView() {

        val args = Hashtable<String, String>()
        args[ARG_CONTENT_ENTRY_UID] = 43L.toString()
        args[UstadMobileSystemCommon.ARG_REFERRER] = REFERRER_FULL_PATH

        val presenter = ContentEntryDetailPresenter(context,
                args, mockView!!,true, umAppDatabase, umAppRepository, mock(), mock(), null, UstadMobileSystemImpl.instance)
        presenter.onCreate(args)

        val argsresult = Hashtable<String, String>()
        argsresult[ARG_CONTENT_ENTRY_UID] = 42L.toString()

        presenter.handleUpNavigation()

        val lastGoToDest = UstadMobileSystemImpl.instance.lastDestination
        Assert.assertEquals("Last destination was ContentEntryListFragmentView",
                ContentEntryListFragmentView.VIEW_NAME, lastGoToDest!!.viewName)
        Assert.assertEquals("Last destination had expect content entry uid arg",
                "42", lastGoToDest.args[ARG_CONTENT_ENTRY_UID])
    }

    @Test
    fun givenUserIsInDetailViewFromNavigation_WhenUpNavigationCalled_thenShouldReturnToDummyView() {

        val args = Hashtable<String, String>()
        args[ARG_CONTENT_ENTRY_UID] = 42L.toString()
        args[UstadMobileSystemCommon.ARG_REFERRER] = REFERRER_NO_PATH

        val presenter = ContentEntryDetailPresenter(context,
                args, mockView!!,true, umAppDatabase, umAppRepository, mock(), mock(), null, UstadMobileSystemImpl.instance)
        presenter.onCreate(args)

        args.remove(UstadMobileSystemCommon.ARG_REFERRER)

        presenter.handleUpNavigation()

        val lastGoToDest = UstadMobileSystemImpl.instance.lastDestination
        Assert.assertEquals("Last destination view name is hoem view", HomeView.VIEW_NAME,
                lastGoToDest!!.viewName)
    }


    companion object {

        private const val REFERRER_FULL_PATH = "/DummyView?/ContentEntryList?entryid=41/ContentEntryList?entryid=42/ContentEntryDetail?entryid=43"
        private const val REFERRER_NO_PATH = ""
        private const val flags = UstadMobileSystemCommon.GO_FLAG_CLEAR_TOP or UstadMobileSystemCommon.GO_FLAG_SINGLE_TOP
    }

}
