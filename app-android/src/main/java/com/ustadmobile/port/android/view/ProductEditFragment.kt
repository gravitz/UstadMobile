package com.ustadmobile.port.android.view

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentProductEditBinding
import com.ustadmobile.core.controller.CategoryListListener
import com.ustadmobile.core.controller.ProductEditPresenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.networkmanager.defaultGson
import com.ustadmobile.core.util.ext.observeResult
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.ProductEditView
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.lib.db.entities.Category
import com.ustadmobile.lib.db.entities.Product
import com.ustadmobile.lib.db.entities.ProductPicture
import com.ustadmobile.lib.db.entities.Schedule
import com.ustadmobile.port.android.util.ext.createTempFileForDestination
import com.ustadmobile.port.android.util.ext.currentBackStackEntrySavedStateMap
import com.ustadmobile.port.android.view.ext.navigateToEditEntity
import com.ustadmobile.port.android.view.ext.navigateToPickEntityFromList
import java.io.File


interface ProductEditFragmentEventHandler {

    fun addNewCategory()
}

class ProductEditFragment: UstadEditFragment<Product>(), ProductEditView,
        ProductEditFragmentEventHandler, CategoryListListener {

    private var mBinding: FragmentProductEditBinding? = null

    private var mPresenter: ProductEditPresenter? = null

    private var categoryListRecyclerAdapter: CategoryListRecyclerAdapter? = null
    private var categoryListRecyclerView: RecyclerView? = null
    private val categoriesObserver = Observer<List<Category>?> {
        t ->
        run {
            categoryListRecyclerAdapter?.submitList(t)
        }
    }

    override val mEditPresenter: UstadEditPresenter<*, Product>?
        get() = mPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView: View
        mBinding = FragmentProductEditBinding.inflate(inflater, container, false).also {
            rootView = it.root
            it.activityEventHandler = this
        }

        categoryListRecyclerAdapter = CategoryListRecyclerAdapter(this, requireContext())
        categoryListRecyclerView = rootView.findViewById(R.id.fragment_product_edit_category_rv)
        categoryListRecyclerView?.adapter = categoryListRecyclerAdapter
        categoryListRecyclerView?.layoutManager = LinearLayoutManager(requireContext())

        mPresenter = ProductEditPresenter(requireContext(), arguments.toStringMap(), this,
                di, viewLifecycleOwner)

        return rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        navController.currentBackStackEntry?.savedStateHandle?.observeResult(this,
                Category::class.java) {
            val category = it.firstOrNull() ?: return@observeResult
            mPresenter?.handleAddOrEditCategory(category)
        }

        navController.currentBackStackEntry?.savedStateHandle?.observeResult(this,
                ProductPicture::class.java) {
            val pic= it.firstOrNull() ?: return@observeResult
            productPicture = pic
        }

        val a = navController.currentBackStackEntry?.savedStateHandle?.get<String>("productPicture_test")
        val pp = defaultGson().fromJson<ProductPicture>(a, ProductPicture::class.java)
        if(pp != null){
            productPicture = pp
        }
        mPresenter?.onCreate(navController.currentBackStackEntrySavedStateMap())

    }

    override fun onDestroyView() {
        super.onDestroyView()   
        mBinding = null
        mPresenter = null
        entity = null
        categoryListRecyclerView = null
        categoryListRecyclerAdapter = null
    }

    override fun onResume() {
        super.onResume()

        setEditFragmentTitle(R.string.add_product, R.string.edit_product)
    }

    override var entity: Product? = null
        get() = field
        set(value) {
            field = value
            mBinding?.product = value


            if(viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val productName = value?.getNameLocale(UMAndroidUtil.getCurrentLocale(requireContext()))
                if(productName?.isNotEmpty() == true) {
                    (activity as? AppCompatActivity)?.supportActionBar?.title = productName
                }
            }
        }
    override var productPicture: ProductPicture?
        get() = mBinding?.productPicture
        set(value) {
            mBinding?.productPicture = value
        }

    override var productPicturePath: String?
        get() {
            val boundPicUri = mBinding?.productPictureUri
            if(boundPicUri == null) {
                return null
            }else{
                val uriObj = Uri.parse(boundPicUri)
                if(uriObj.scheme == "file") {
                    return uriObj.toFile().absolutePath
                }else {
                    val tmpFile = findNavController().createTempFileForDestination(requireContext(),
                            "productPicture-${System.currentTimeMillis()}")
                    try {
                        val input = (context as Context).contentResolver.openInputStream(uriObj) ?: return null
                        val output = tmpFile.outputStream()
                        input.copyTo(tmpFile.outputStream())
                        output.flush()
                        output.close()
                        input.close()
                        return tmpFile.absolutePath
                    }catch(e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
        }

        set(value) {
            if(value != null) {
                mBinding?.productPictureUri = Uri.fromFile(File(value)).toString()
            }else {
                mBinding?.productPictureUri = null
            }
        }
    override var categories: DoorMutableLiveData<List<Category>>? = null
        set(value) {
            field?.removeObserver(categoriesObserver)
            field = value
            value?.observe(this, categoriesObserver)
        }

    override var fieldsEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            mBinding?.fieldsEnabled = value
        }

    override fun addNewCategory() {
        val navController = findNavController()
        navController.currentBackStackEntry?.savedStateHandle?.set("productPicture_test", defaultGson().toJson(productPicture))
        onSaveStateToBackStackStateHandle()
        //TODO: Save picture as well
        navigateToPickEntityFromList(Category::class.java, R.id.category_list_dest)
    }

    override fun onClickDelete(entry: Category) {
        mPresenter?.handleRemoveCategory(entry)
    }

    override fun onClickCategory(entry: Category) {
        onSaveStateToBackStackStateHandle()
        navigateToEditEntity(entry, R.id.category_edit_dest, Category::class.java)
    }
}