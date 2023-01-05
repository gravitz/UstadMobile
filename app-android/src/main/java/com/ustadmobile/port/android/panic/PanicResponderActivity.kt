package com.ustadmobile.port.android.panic

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.port.android.presenter.PanicButtonSettingsPresenter.Companion.PREF_CLEAR_APP_DATA
import com.ustadmobile.port.android.presenter.PanicButtonSettingsPresenter.Companion.PREF_LOCK_AND_EXIT
import com.ustadmobile.port.android.presenter.PanicButtonSettingsPresenter.Companion.PREF_UNINSTALL_THIS_APP
import info.guardianproject.panic.PanicResponder
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.instance

/**
 * Activity that will respond to a PanicKit trigger. Roughly as per:
 * https://github.com/guardianproject/FakePanicResponder/blob/master/src/info/guardianproject/fakepanicresponder/ResponseActivity.java
 */
class PanicResponderActivity: AppCompatActivity(), DIAware {

    override val di by closestDI()

    val systemImpl: UstadMobileSystemImpl by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PanicResponder.receivedTriggerFromConnectedApp(this)) {
            if (systemImpl.getAppPref(PREF_UNINSTALL_THIS_APP)?.toBoolean() == true) {
                HidingManager().hide(this)
            }

            if (systemImpl.getAppPref(PREF_CLEAR_APP_DATA)?.toBoolean() == true) {
                PanicResponder.deleteAllAppData(this)
            }

            if (systemImpl.getAppPref(PREF_LOCK_AND_EXIT)?.toBoolean() == true) {
                ExitActivity.exitAndRemoveFromRecentApps(this)
            }
        } else if (PanicResponder.shouldUseDefaultResponseToTrigger(this)) {
            if (systemImpl.getAppPref(PREF_LOCK_AND_EXIT)?.toBoolean() == true) {
                ExitActivity.exitAndRemoveFromRecentApps(this)
            }
        }

        finish()
    }

}