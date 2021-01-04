package com.ustadmobile.port.android.screen

import com.kaspersky.kaspresso.screens.KScreen
import com.agoda.kakao.text.KTextView
import com.agoda.kakao.web.KWebView
import com.toughra.ustadmobile.R
import com.ustadmobile.port.android.view.SiteTermsDetailFragment

object  SiteTermsDetailScreen : KScreen<SiteTermsDetailScreen>() {
    override val layoutId: Int
        get() = R.layout.fragment_site_terms_detail

    override val viewClass: Class<*>?
        get() = SiteTermsDetailFragment::class.java

    val webView = KWebView { withId(R.id.terms_webview) }

}