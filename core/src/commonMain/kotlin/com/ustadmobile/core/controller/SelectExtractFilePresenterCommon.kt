package com.ustadmobile.core.controller

import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.contentjob.ContentJobProcessContext
import com.ustadmobile.core.contentjob.ContentPluginManager
import com.ustadmobile.core.contentjob.MetadataResult
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.NavigateForResultOptions
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.createTemporaryDir
import com.ustadmobile.core.util.ext.logErrorReport
import com.ustadmobile.core.util.ext.putEntityAsJson
import com.ustadmobile.core.util.ext.putFromOtherMapIfPresent
import com.ustadmobile.core.util.safeStringify
import com.ustadmobile.core.view.ContentEntryEdit2View
import com.ustadmobile.core.view.ContentEntryEdit2View.Companion.BLOCK_REQUIRED
import com.ustadmobile.core.view.SelectExtractFileView
import com.ustadmobile.core.view.SelectFileView
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.door.DoorUri
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ErrorReport
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.on

class SelectExtractFilePresenterCommon(
    context: Any,
    arguments: Map<String, String>,
    view: SelectExtractFileView,
    di: DI
) : UstadBaseController<SelectExtractFileView>(context, arguments, view, di)  {

    val accountManager: UstadAccountManager by instance()

    private val pluginManager: ContentPluginManager by on(accountManager.activeAccount).instance()

    val repo: UmAppDatabase by on(accountManager.activeAccount).instance(tag = UmAppDatabase.TAG_REPO)

    val systemImpl: UstadMobileSystemImpl by instance()

    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)
        view.acceptedMimeTypes =  arguments[SelectFileView.ARG_MIMETYPE_SELECTED].toString().split(";")
    }

    fun handleUriSelected(uri: String?){

        if(uri == null) {
            ustadNavController.currentBackStackEntry?.viewName?.let {
                ustadNavController.popBackStack(
                    it,true)
            }
            return
        }

        presenterScope.launch {

            val doorUri = DoorUri.parse(uri)
            ContentJobProcessContext(doorUri, createTemporaryDir("content"),
                mutableMapOf(), null, di).use { processContext ->
                try {
                    val metadata = pluginManager.extractMetadata(DoorUri.parse(uri), processContext)

                    when {
                        (arguments[UstadView.ARG_RESULT_DEST_VIEWNAME] == ContentEntryEdit2View.VIEW_NAME) -> {
                            finishWithResult(
                                safeStringify(
                                    di,
                                    ListSerializer(MetadataResult.serializer()),
                                    listOf(metadata)
                                )
                            )
                        }
                        else -> {
                            val args = mutableMapOf<String, String>()
                            args.putEntityAsJson(
                                ContentEntryEdit2View.ARG_IMPORTED_METADATA,
                                MetadataResult.serializer(), metadata
                            )
                            args.putFromOtherMapIfPresent(arguments, UstadView.ARG_LEAF)
                            args.putFromOtherMapIfPresent(arguments, UstadView.ARG_PARENT_ENTRY_UID)
                            args.putFromOtherMapIfPresent(arguments, BLOCK_REQUIRED)
                            args.putFromOtherMapIfPresent(arguments, UstadView.ARG_CLAZZUID)

                            navigateForResult(
                                NavigateForResultOptions(
                                    this@SelectExtractFilePresenterCommon,
                                    null,
                                    ContentEntryEdit2View.VIEW_NAME,
                                    ContentEntry::class,
                                    ContentEntry.serializer(),
                                    arguments = args
                                )
                            )
                        }
                    }
                }catch (e: Exception){
                    view.unSupportedFileError = systemImpl.getString(MessageID.import_link_content_not_supported, context)
                    Napier.e("Error extracting metadata", e)
                    repo.errorReportDao.logErrorReport(ErrorReport.SEVERITY_ERROR, e,
                        this@SelectExtractFilePresenterCommon)
                }

            }

        }

    }

}