package com.ustadmobile.core.controller

import com.nhaarman.mockitokotlin2.*
import com.ustadmobile.core.container.ContainerManager
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.ContentEntryExportView
import com.ustadmobile.util.test.AbstractContentEntryExportTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class ContentEntryExportPresenterTest : AbstractContentEntryExportTest(){

    private lateinit var umAppRepository: UmAppDatabase

    private lateinit var umAppDatabase: UmAppDatabase

    private lateinit var mockView: ContentEntryExportView

    private lateinit var presenter: ContentEntryExportPresenter

    private val context = Any()

    private var arguments: HashMap<String, String?> = hashMapOf()


    @Before
    fun setUp(){
        umAppRepository = UmAccountManager.getRepositoryForActiveAccount(context)
        umAppDatabase = UmAppDatabase.getInstance(context)
        insertContainer(umAppDatabase, umAppRepository)
        val containerTmpDir = File.createTempFile("testcontainerdir", "tmp")
        containerTmpDir.delete()
        containerTmpDir.mkdir()
        val containerManager = ContainerManager(container!!, umAppDatabase, umAppRepository,
                containerTmpDir.absolutePath)

        val pathList = mutableListOf("path1.txt", "path2.txt", "path3.txt", "path4.txt")
        val fileSources = mutableListOf<ContainerManager.FileEntrySource>()
        pathList.forEach {
            fileSources.add(ContainerManager.FileEntrySource(
                    File.createTempFile("tmp", it), it))
        }
        runBlocking {
            containerManager.addEntries(*fileSources.toTypedArray())
        }

        arguments[ContentEntryExportView.ARG_CONTENT_ENTRY_UID] = contentEntryUid.toString()
        arguments[ContentEntryExportView.ARG_CONTENT_ENTRY_TITLE] = "Sample title"

        mockView = mock {
            on { runOnUiThread(any()) }.doAnswer {invocation ->
                Thread(invocation.getArgument<Any>(0) as Runnable).start()
                Unit
            }
        }

    }



    @Test
    fun givenExportOptionSelected_whenPreparing_thenShouldCheckFilePermission(){
        presenter = ContentEntryExportPresenter(context,arguments, mockView,
                umAppDatabase,umAppRepository,UstadMobileSystemImpl.instance )
        presenter.onCreate(null)

        verify(mockView, times(1)).checkFilePermissions()
    }


    @Test
    fun givenExportDialogIsShown_whenPositiveButtonIsClicked_thenShouldStartExportingByShowingProgress(){
        presenter = ContentEntryExportPresenter(context,arguments, mockView,
                umAppDatabase,umAppRepository,UstadMobileSystemImpl.instance )
        presenter.onCreate(null)
        presenter.handleClickPositive()

        verify(mockView, times(1)).prepareProgressView(eq(true))
        File(presenter.destinationZipFile).delete()
    }


    @Test
    fun givenExportDialogIsShown_whenNegativeButtonIsClicked_shouldQuitExportingAndDismissDialog(){
        runBlocking {
            presenter = ContentEntryExportPresenter(context,arguments, mockView,
                    umAppDatabase,umAppRepository,UstadMobileSystemImpl.instance )
            presenter.onCreate(null)
            presenter.handleClickNegative()

            verify(mockView, times(0)).prepareProgressView(any())
            delay(TimeUnit.SECONDS.toMillis(1))
            verify(mockView, times(1)).dismissDialog()
            File(presenter.destinationZipFile).delete()
        }
    }


}