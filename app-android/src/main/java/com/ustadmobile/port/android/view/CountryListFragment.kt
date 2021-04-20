package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentListBinding
import com.toughra.ustadmobile.databinding.ItemCountryBinding
import com.ustadmobile.core.controller.CountryListPresenter
import com.ustadmobile.core.controller.OnSearchSubmitted
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.ext.toNullableStringMap
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.CountryListView
import com.ustadmobile.lib.db.entities.Country
import com.ustadmobile.port.android.view.ext.saveResultToBackStackSavedStateHandle
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.kodein.di.direct
import org.kodein.di.instance
import java.io.IOException
import java.util.*

class CountryListFragment : UstadBaseFragment(), CountryListView, OnSearchSubmitted {

    class CountryViewHolder(var binding: ItemCountryBinding) : RecyclerView.ViewHolder(binding.root)

    inner class CountryRecyclerViewAdapter : ListAdapter<Country, CountryViewHolder>(DIFFUTIL_COUNTRY) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
            return CountryViewHolder(ItemCountryBinding.inflate(LayoutInflater.from(parent.context),
                    parent, false)).also {
                it.binding.fragment = this@CountryListFragment
            }
        }

        override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
            holder.binding.country = getItem(position)
        }
    }

    var mDataBinding: FragmentListBinding? = null

    private var mRecyclerAdapter: CountryRecyclerViewAdapter? = null

    private var mPresenter: CountryListPresenter? = null

    private var systemImpl: UstadMobileSystemImpl = di.direct.instance()

    fun allCountries(): List<Country> {
        val locale = systemImpl.getDisplayedLocale(requireContext())
        var json = ""
        try {
            json = requireContext().assets.open("countrynames/${locale}.json")
                    .bufferedReader().use { it.readText() }
        }catch (io: IOException){
            showSnackBar(systemImpl.getString(MessageID.error,
                    requireContext()), {})
        }
        return Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), json).map {
            Country(it.key, it.value)
        }
    }

    private val originalListOfCountries: List<Country> by lazy {
        allCountries()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mDataBinding = FragmentListBinding.inflate(inflater, container, false)
        mRecyclerAdapter = CountryRecyclerViewAdapter()
        mDataBinding?.fragmentListRecyclerview?.layoutManager = LinearLayoutManager(requireContext())
        mDataBinding?.fragmentListRecyclerview?.adapter = mRecyclerAdapter
        mRecyclerAdapter?.submitList(originalListOfCountries)

        mPresenter = CountryListPresenter(requireContext(), arguments.toStringMap(),
                this, di)
        mPresenter?.onCreate(savedInstanceState.toNullableStringMap())

        return mDataBinding?.root
    }

    fun handleClickCountry(country: Country) {
        mPresenter?.handleClickCountry(country)
    }

    override fun onSearchSubmitted(text: String?) {
        if (text == null) {
            mRecyclerAdapter?.submitList(originalListOfCountries)
            return
        }

        val searchWords = text.split(Regex("\\s+"))
        val filteredItems = originalListOfCountries.filter { country ->
            searchWords.any { country.code.contains(it, ignoreCase = true) }
                    || searchWords.any { country.name.contains(it, ignoreCase = true) }
        }

        mRecyclerAdapter?.submitList(filteredItems)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.menu_search).isVisible = true
        searchManager?.searchListener = this
    }

    override fun finishWithResult(country: Country) {
        saveResultToBackStackSavedStateHandle(listOf(country))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDataBinding?.fragmentListRecyclerview?.adapter = null
        mRecyclerAdapter = null
        mPresenter = null
        mDataBinding = null
    }

    companion object {
        val DIFFUTIL_COUNTRY = object : DiffUtil.ItemCallback<Country>() {
            override fun areItemsTheSame(oldItem: Country, newItem: Country): Boolean {
                return oldItem.code == newItem.code
            }

            override fun areContentsTheSame(oldItem: Country, newItem: Country): Boolean {
                return oldItem.code == newItem.code
            }
        }

        const val RESULT_COUNTRY_KEY = "country"

    }

}