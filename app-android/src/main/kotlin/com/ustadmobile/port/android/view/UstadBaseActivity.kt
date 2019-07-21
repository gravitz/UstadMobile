package com.ustadmobile.port.android.view

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.squareup.seismic.ShakeDetector
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.UstadBaseController
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.impl.UstadMobileSystemImpl.Companion.ACTION_LOCALE_CHANGE
import com.ustadmobile.core.impl.UstadMobileSystemImpl.Companion.instance
import com.ustadmobile.core.util.UMCalendarUtil
import com.ustadmobile.core.view.Login2View
import com.ustadmobile.core.view.Login2View.Companion.ARG_LOGIN_USERNAME
import com.ustadmobile.core.view.UstadViewWithNotifications
import com.ustadmobile.core.view.ViewWithErrorNotifier
import com.ustadmobile.lib.db.entities.UmAccount
import com.ustadmobile.port.android.impl.LastActive
import com.ustadmobile.port.android.impl.UserFeedbackException
import com.ustadmobile.sharedse.network.NetworkManagerBleAndroidService
import com.ustadmobile.port.android.netwokmanager.UmAppDatabaseSyncService
import com.ustadmobile.port.sharedse.util.RunnableQueue
import com.ustadmobile.sharedse.network.NetworkManagerBle
import kotlinx.coroutines.Runnable
import org.acra.ACRA
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.HashMap


/**
 * Base activity to handle interacting with UstadMobileSystemImpl
 *
 *
 * Created by mike on 10/15/15.
 */
abstract class UstadBaseActivity : AppCompatActivity(), ServiceConnection,
        UstadViewWithNotifications, ViewWithErrorNotifier, ShakeDetector.Listener {

    private var baseController: UstadBaseController<*>? = null

    /**
     * Get the toolbar that's used for the support action bar
     *
     * @return
     */
    protected lateinit var umToolbar: Toolbar

    /**
     * Currently running instance of NetworkManagerBleCommon
     */
    /**
     * @return Active NetworkManagerBleCommon
     */
     var networkManagerBle: NetworkManagerBle? = null

    private var fragmentList: MutableList<WeakReference<Fragment>>? = null

    private var localeChanged = false

    private var localeOnCreate: String? = null

    /**
     * Can be used to check if the activity has been started.
     *
     * @return true if the activity is started. false if it has not been started yet, or it was started, but has since stopped
     */
    var isStarted = false
        private set

    private var permissionRequestRationalesShown = false

    private var afterPermissionMethodRunner: Runnable? = null

    private val runWhenServiceConnectedQueue = RunnableQueue()

    private var permissionDialogTitle: String? = null

    private var permissionDialogMessage: String? = null

    private var permission: String? = null

    var checkLogout:Boolean = true

    val PREFKEY_LAST_ACTIVE = "prefke.lastactive"
    val TIMEOUT_LOGOUT = 600000L


    private val mSyncServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mSyncServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mSyncServiceBound = false
        }
    }

    /**
     * Ble service connection
     */
    private val bleServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            networkManagerBle = (service as NetworkManagerBleAndroidService.LocalServiceBinder)
                    .service.networkManagerBle
            bleServiceBound = true
            onBleNetworkServiceBound(networkManagerBle)
            runWhenServiceConnectedQueue.setReady(true)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bleServiceBound = false
            onBleNetworkServiceUnbound()
        }
    }

    private var mSyncServiceBound = false

    @Volatile
    private var bleServiceBound = false
    private var shakeDetector: ShakeDetector? = null
    private var sensorManager: SensorManager? = null
    internal var feedbackDialogVisible = false

    override val viewContext: Any
        get() = this


    override fun onCreate(savedInstanceState: Bundle?) {
        //bind to the LRS forwarding service
        instance.handleActivityCreate(this, savedInstanceState)
        fragmentList = ArrayList()
        val intentFilter = IntentFilter()

        super.onCreate(savedInstanceState)
        localeOnCreate = instance.getDisplayedLocale(this)


        val syncServiceIntent = Intent(this, UmAppDatabaseSyncService::class.java)
        bindService(syncServiceIntent, mSyncServiceConnection,
                Context.BIND_AUTO_CREATE or Context.BIND_ADJUST_WITH_ACTIVITY)

        //bind ble service
        val bleServiceIntent = Intent(this, NetworkManagerBleAndroidService::class.java)
        bindService(bleServiceIntent, bleServiceConnection,
                Context.BIND_AUTO_CREATE or Context.BIND_ADJUST_WITH_ACTIVITY)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(this)

    }

    override fun hearShake() {

        if (feedbackDialogVisible) {
            return
        }

        feedbackDialogVisible = true
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.send_feedback)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.view_feedback_layout, null)
        val editText = dialogView.findViewById<EditText>(R.id.feedback_edit_comment)
        builder.setView(dialogView)
        builder.setPositiveButton(R.string.send) { dialogInterface, whichButton ->
            ACRA.getErrorReporter().handleSilentException(UserFeedbackException(editText.text.toString()))
            Toast.makeText(this, R.string.feedback_thanks, Toast.LENGTH_LONG).show()
            dialogInterface.cancel()
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface, i -> dialogInterface.cancel() }
        builder.setOnDismissListener { dialogInterface -> feedbackDialogVisible = false }
        builder.setOnCancelListener { dialogInterface -> feedbackDialogVisible = false }
        val dialog = builder.create()
        dialog.show()

    }

    /**
     * Display the snackbar at the bottom of the page
     *
     * @param errorMessage    message for the snackbar
     * @param actionMessageId id of action name
     * @param action          action listener
     */
    override fun showErrorNotification(errorMessage: String, action: () -> Unit, actionMessageId: Int) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_LONG)
        val impl = instance
        if(actionMessageId != 0) {
            snackbar.setAction(impl.getString(actionMessageId, this)) { action() }
            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.accent))
        }
        snackbar.show()
    }

    /**
     * All activities descending from UstadBaseActivity bind to the network manager. This method
     * can be overriden when presenters need to use a reference to the networkmanager.
     *
     * @param networkManagerBle
     */
    protected open fun onBleNetworkServiceBound(networkManagerBle: NetworkManagerBle?) {}

    protected fun onBleNetworkServiceUnbound() {

    }

    override fun onResume() {
        super.onResume()

        checkTimeout()


        if (localeChanged) {
            if (instance.hasDisplayedLocaleChanged(localeOnCreate, this)) {
                Handler().postDelayed({ this.recreate() }, 200)
            }
        }
        if (shakeDetector != null && sensorManager != null) {
            shakeDetector!!.start(sensorManager)
        }
    }

    override fun onPause() {
        super.onPause()
        if (shakeDetector != null) {
            shakeDetector!!.stop()
        }
    }

    /**
     * UstadMobileSystemImpl will bind certain services to each activity (e.g. HTTP, P2P services)
     * If needed the child activity can override this method to listen for when the service is ready
     *
     * @param name
     * @param iBinder
     */
    override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {

    }

    override fun onServiceDisconnected(name: ComponentName) {

    }

    protected fun setUMToolbar(toolbarID: Int) {
        umToolbar = findViewById<View>(toolbarID) as Toolbar
        setSupportActionBar(umToolbar)
        supportActionBar!!.setHomeButtonEnabled(true)
    }


    protected fun setBaseController(baseController: UstadBaseController<*>) {
        this.baseController = baseController
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }


    public override fun onStart() {
        isStarted = true
        checkTimeout()
        super.onStart()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val systemTime = AtomicLong(UMCalendarUtil.getDateInMilliPlusDays(0))
        updateLastActive(systemTime)

        return super.onTouchEvent(event)
    }

    fun updateLastActive(time: AtomicLong){
        LastActive.instance.lastActive = time
        val impl = instance
        impl.setAppPref(PREFKEY_LAST_ACTIVE, time.toString(), viewContext)
    }

    private fun checkTimeout() {
        val impl = instance
        val lastInputEventTime = LastActive.instance.lastActive
        var lt:Long
        var lastActiveString = impl.getAppPref(PREFKEY_LAST_ACTIVE, viewContext)
        if (lastActiveString != null && !lastActiveString.isEmpty())
        {
            lt = java.lang.Long.parseLong(lastActiveString)
        }
        else
        {
            lt = 0
        }
        val timeoutExceeded = System.currentTimeMillis() - lt
        val logoutTimeout = TIMEOUT_LOGOUT //TODO: Get and set from app pref
        if (timeoutExceeded > logoutTimeout)
        {
            handleLogout()
        }
    }

    private fun handleLogout() {
        if (checkLogout)
        {
            var currentUsername:String ?= null
            if (UmAccountManager.getActiveAccount(viewContext) != null)
            {
                currentUsername = UmAccountManager.getActiveAccount(viewContext)!!.username
            }
            finishAffinity()
            val blankAccount = UmAccount(0, null, null,null)
            UmAccountManager.setActiveAccount(blankAccount, viewContext)
            val impl = UstadMobileSystemImpl.instance
            val args = HashMap<String, String>()
            if (currentUsername != null)
            {
                args.put(ARG_LOGIN_USERNAME, currentUsername)
            }
            impl.go(Login2View.VIEW_NAME, args, viewContext)
        }
    }



    public override fun onStop() {
        isStarted = false
        super.onStop()
    }

    public override fun onDestroy() {
        if (bleServiceBound) {
            unbindService(bleServiceConnection)
        }

        instance.handleActivityDestroy(this)
        if (mSyncServiceBound) {
            unbindService(mSyncServiceConnection)
        }
        shakeDetector = null
        sensorManager = null
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val impl = instance
                impl.go(impl.getAppConfigString(AppConfig.KEY_FIRST_DEST, null,
                        this), this)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        fragmentList!!.add(WeakReference<Fragment>(fragment))
    }


    /**
     * Handle our own delegation of back button presses.  This allows UstadBaseFragment child classes
     * to handle back button presses if they want to.
     */
    override fun onBackPressed() {
        for (fragmentReference in fragmentList!!) {
            if (fragmentReference.get() == null)
                continue

            if (!fragmentReference.get()!!.isVisible)
                continue

            if (fragmentReference.get() is UstadBaseFragment && (fragmentReference.get() as UstadBaseFragment).canGoBack()) {
                (fragmentReference.get() as UstadBaseFragment).goBack()
                return
            }
        }

        super.onBackPressed()
    }

    override fun attachBaseContext(newBase: Context) {
        val res = newBase.resources
        val config = res.configuration
        val languageSetting = instance.getLocale(newBase)

        if (Build.VERSION.SDK_INT >= 17) {
            val locale = if (languageSetting == UstadMobileSystemCommon.LOCALE_USE_SYSTEM)
                Locale.getDefault()
            else
                Locale(languageSetting)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }


    override fun showNotification(notification: String, length: Int) {
        runOnUiThread { Toast.makeText(this, notification, length).show() }
    }

    /**
     * Responsible for running task after checking permissions
     *
     * @param permission    Permission to be checked
     * @param runnable      Future task to be executed
     * @param dialogTitle   Permission dialog title
     * @param dialogMessage Permission dialog message
     */
    fun runAfterGrantingPermission(permission: String, runnable: Runnable?,
                                   dialogTitle: String?, dialogMessage: String?) {
        this.afterPermissionMethodRunner = runnable

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            afterPermissionMethodRunner!!.run()
            afterPermissionMethodRunner = null
            return
        }


        this.permissionDialogMessage = dialogMessage
        this.permissionDialogTitle = dialogTitle
        this.permission = permission

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (!permissionRequestRationalesShown) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(permissionDialogTitle)
                        .setMessage(permissionDialogMessage)
                        .setNegativeButton(getString(android.R.string.cancel)
                        ) { dialog, which -> dialog.dismiss() }
                        .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                            runAfterGrantingPermission(permission, afterPermissionMethodRunner,
                                    permissionDialogTitle, permissionDialogMessage)
                        }
                val dialog = builder.create()
                dialog.show()
                permissionRequestRationalesShown = true
            } else {
                permissionRequestRationalesShown = false
                ActivityCompat.requestPermissions(this, arrayOf(permission), RUN_TIME_REQUEST_CODE)
            }
        } else {
            afterPermissionMethodRunner!!.run()
            afterPermissionMethodRunner = null
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            RUN_TIME_REQUEST_CODE -> {
                var allPermissionGranted = grantResults.size == permissions.size
                for (result in grantResults) {
                    allPermissionGranted = allPermissionGranted and (result == PackageManager.PERMISSION_GRANTED)
                }

                if (!allPermissionGranted && permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    afterPermissionMethodRunner!!.run()
                    afterPermissionMethodRunner = null
                }

                if (allPermissionGranted) {
                    afterPermissionMethodRunner!!.run()
                    afterPermissionMethodRunner = null
                    return
                }
            }
        }
    }

    /**
     * Make sure NetworkManagerBleCommon is not null when running a certain logic
     *
     * @param runnable Future task to be executed
     */
    fun runAfterServiceConnection(runnable: Runnable) {
        runWhenServiceConnectedQueue.runWhenReady(runnable)
    }

    companion object {

        private const val RUN_TIME_REQUEST_CODE = 111
        const val ACTION_REMINDER_NOTIFICATION = 752
    }
}
