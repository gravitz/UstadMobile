package com.ustadmobile.core.impl

import com.ustadmobile.core.util.CountryProvider
import com.ustadmobile.core.viewmodel.CountryData

expect open class CountryProviderImp: CountryProvider {

    override fun getCountry(): CountryData

}