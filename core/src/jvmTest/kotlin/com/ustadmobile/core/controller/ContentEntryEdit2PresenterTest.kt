package com.ustadmobile.core.controller

import com.nhaarman.mockitokotlin2.*
import com.ustadmobile.core.container.ContainerManagerCommon
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ContentEntryDao
import com.ustadmobile.core.impl.UMStorageDir
import com.ustadmobile.core.impl.UmResultCallback
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.networkmanager.downloadmanager.ContainerDownloadManager
import com.ustadmobile.core.view.ContentEntryEdit2View
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.util.test.AbstractSetup
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test


class ContentEntryEdit2PresenterTest : AbstractSetup() {

    lateinit var systemImpl: UstadMobileSystemImpl

    private lateinit var mockView: ContentEntryEdit2View

    private lateinit var activeAccount: DoorMutableLiveData<UmAccount?>

    private lateinit var context: Any

    private lateinit var db: UmAppDatabase

    private lateinit var repo: UmAppDatabase

    private lateinit var mockLifecycleOwner: DoorLifecycleOwner

    private lateinit var container: Container

    private lateinit var contentEntry: ContentEntryWithLanguage

    private lateinit var containerManager: ContainerDownloadManager

    private val parentUid: Long = 12345678L

    private val entryUid: Long = 100L

    private lateinit var mockEntryDao:ContentEntryDao

    private val errorMessage: String = "Dummy error"

    @Before
    fun setUp() {
        context = Any()
        container = createMockContainer()
        contentEntry = createMockEntryWithLanguage()
        mockLifecycleOwner = mock { }
        activeAccount = DoorMutableLiveData(UmAccount(42, "bobjones", "",
                "http://localhost"))
        val realDb = UmAppDatabase.getInstance(context)
        db =  spy(realDb) { }
        db.clearAllTables()

        mockEntryDao = spy{
            onBlocking { insertAsync(any()) }.thenReturn(entryUid)
        }
        repo = spy(realDb) {
            on{contentEntryDao}.thenAnswer{mockEntryDao}
        }

        containerManager = spy{}

        systemImpl = mock{
            on { getStorageDirs(any(), any()) }.thenAnswer {
                (it.getArgument(1) as UmResultCallback<List<UMStorageDir>>).onDone(
                        mutableListOf(UMStorageDir("", "", removableMedia = false,
                        isAvailable = false, isUserSpecific = false)))
            }
            on { getString(any(), any()) }.thenAnswer{errorMessage}
        }

        mockView = mock{
            onBlocking {
                saveContainerOnExit(entryUid, "", db, repo)}.thenAnswer{container}
            on {selectedStorageIndex}.thenAnswer {0}
        }
    }

    @After
    fun tearDown() {}

    private fun createMockEntryWithLanguage(): ContentEntryWithLanguage{
        val language = Language()
        language.iso_639_2_standard = "en"
        language.langUid = 23
        val content = ContentEntryWithLanguage()
        content.title = "Dummy Title"
        content.description = "Dummy description"
        content.licenseName = "Dummy Licence Name"
        content.licenseType = 1
        content.leaf = true
        content.language = language
        return content
    }

    private fun createMockContainer(): Container {
        val container = Container()
        container.containerUid = 90
        container.fileSize = 23459
        return container
    }


    @Test
    fun givenPresenterCreatedAndEntryNotCreated_whenClickSave_shouldCreateAnEntry() {
        val presenter = ContentEntryEdit2Presenter(context, mapOf(UstadView.ARG_PARENT_ENTRY_UID to parentUid.toString())
                ,mockView,mockLifecycleOwner,systemImpl,db,repo,containerManager, activeAccount )

        presenter.onCreate(null)
        presenter.handleClickSave(contentEntry)

        argumentCaptor<ContentEntryWithLanguage>().apply {
            verifyBlocking(mockEntryDao){
                insertAsync(capture())
            }
            assertEquals("Got expected content entry title",contentEntry.title, firstValue.title)
        }

        argumentCaptor<Long>().apply {
            verifyBlocking(mockView){
                mockView.saveContainerOnExit(capture(), any(), eq(db), eq(repo))
            }
            assertEquals("Got expected content entry uid",entryUid, firstValue)
        }

    }

    @Test
    fun givenPresenterCreatedAndFolderNotCreated_whenClickSave_shouldCreateAFolder() {
        contentEntry.leaf = false
        val presenter = ContentEntryEdit2Presenter(context, mapOf(UstadView.ARG_PARENT_ENTRY_UID to parentUid.toString())
                ,mockView,mockLifecycleOwner,systemImpl,db,repo,containerManager, activeAccount )

        presenter.onCreate(null)
        presenter.handleClickSave(contentEntry)

        argumentCaptor<ContentEntryWithLanguage>().apply {
            verifyBlocking(mockEntryDao){
                insertAsync(capture())
            }
            assertEquals("Got expected folder title",contentEntry.title, firstValue.title)
        }

        //verify that container was not created
        verifyBlocking(mockView, times(0)){
            mockView.saveContainerOnExit(any(), any(), eq(db), eq(repo))
        }

    }

    @Test
    fun givenPresenterCreatedAndEntryCreated_whenClickSave_shouldUpdateAnEntry() {
        contentEntry.contentEntryUid = entryUid
        val presenter = ContentEntryEdit2Presenter(context, mapOf(UstadView.ARG_PARENT_ENTRY_UID to parentUid.toString())
                ,mockView,mockLifecycleOwner,systemImpl,db,repo,containerManager, activeAccount )

        presenter.onCreate(null)
        presenter.handleClickSave(contentEntry)

        argumentCaptor<ContentEntryWithLanguage>().apply {
            verifyBlocking(mockEntryDao){
                updateAsync(capture())
            }
            assertEquals("Got expected content entry title",contentEntry.title, firstValue.title)
        }

        argumentCaptor<Long>().apply {
            verifyBlocking(mockView){
                mockView.saveContainerOnExit(capture(), any(), eq(db), eq(repo))
            }
            assertEquals("Got expected content entry uid",entryUid, firstValue)
        }


    }

    @Test
    fun givenPresenterCreatedAndEntryFieldsAreNotFilled_whenClickSave_shouldShowErrorMessage() {
        contentEntry.title = null
        contentEntry.description = null
        val presenter = ContentEntryEdit2Presenter(context, mapOf()
                ,mockView,mockLifecycleOwner,systemImpl,db,repo,containerManager, activeAccount )

        presenter.onCreate(null)
        presenter.handleClickSave(contentEntry)

        argumentCaptor<String>().apply {
            verify(mockView, timeout(0)).showFeedbackMessage(capture(), any(), any())
            assertEquals("Got expected content error message",errorMessage, firstValue)
        }
    }

}