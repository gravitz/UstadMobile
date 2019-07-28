package com.ustadmobile.core.controller

import androidx.paging.DataSource
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UmAccountManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import com.ustadmobile.core.view.PersonWithSaleInfoDetailView;


import com.ustadmobile.lib.db.entities.PersonWithSaleInfo

import com.ustadmobile.core.db.dao.SaleDao

import com.ustadmobile.core.db.dao.PersonDao
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.PersonWithSaleInfoDetailView.Companion.ARG_WE_UID
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.SaleListDetail

/**
 *  Presenter for PersonWithSaleInfoDetail view
 **/
class PersonWithSaleInfoDetailPresenter(context: Any,
                    arguments: Map<String, String>?,
                    view: PersonWithSaleInfoDetailView,
                    val systemImpl: UstadMobileSystemImpl,
                    private val repository: UmAppDatabase =
                                    UmAccountManager.getRepositoryForActiveAccount(context),
                    private val saleDao: SaleDao = repository.saleDao)
    : UstadBaseController<PersonWithSaleInfoDetailView>(context, arguments!!, view) {

    private var personDao: PersonDao? = null
    private var currentPerson: Person? = null
    private var loggedInPersonUid = 0L

    private lateinit var salesFactory : DataSource.Factory<Int, SaleListDetail>

    private var personUid :Long = 0

    init {
        personDao = repository.personDao
    }

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        //Any arguments

		if(arguments.containsKey(ARG_WE_UID)){
			personUid = (arguments[ARG_WE_UID]!!.toLong())
		}

        if (loggedInPersonUid != 0L) {
            GlobalScope.launch {
                var result = personDao!!.findByUidAsync(loggedInPersonUid)
                currentPerson = result
            }
        }

        //Populate view
        GlobalScope.launch {
            val person = personDao!!.findByUidAsync(personUid)
            if(person!= null) {
                view.updatePersonOnView(person)
                //Update sales
                salesFactory = saleDao!!.findAllSalesWithWEFilter(person.personUid)
                view.setSalesFactory(salesFactory)
            }
        }
    }
}
