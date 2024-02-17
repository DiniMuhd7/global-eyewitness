package com.dinisoft.eyewitness.ui.home

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.MailTo
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.material.internal.ViewUtils
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.install.model.ActivityResult
import com.onesignal.OneSignal
import com.dinisoft.eyewitness.R
import com.dinisoft.eyewitness.setting.AppDataInstance
import com.dinisoft.eyewitness.setting.OneSignalNotificationOpenedHandler
import com.dinisoft.eyewitness.setting.ThemeConfig
import com.dinisoft.eyewitness.setting.WebviewGPSTrack
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class HomeActivity : ThemeBaseActivity(), NavigationView.OnNavigationItemSelectedListener{
    private val TAG: String = "---HomeActivity"

    var mFileCamMessage: String? = null
    private var mFileMessage: ValueCallback<Uri>? = null
    var mFilePath: ValueCallback<Array<Uri>>? = null
    private var isDoubleBackToExit = false
    private lateinit var mNewsMagazinesPopup: PopupWindow
    private lateinit var mSocialNetworkPopup: PopupWindow
    private lateinit var mEducationPopup: PopupWindow
    private lateinit var mVerifPopup: PopupWindow
    private lateinit var mResultPopup: PopupWindow
    private lateinit var mAboutUsPopup: PopupWindow

    private var mOnGoingDownload: Long? = null
    private var mDownloadManger: DownloadManager? = null
    private var isViewLoaded: Boolean = false
    private var isApplicationAlive = true

    private var mJsRequestCount = 0
    private var mSmartAdsIncrement: Int = 0
    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private lateinit var mInterstitial: InterstitialAd

    private var mWebviewPop : WebView? = null
    private var mPermissionRequest: PermissionRequest? = null

    private var mLastUrl : String = ""
    private var mSuccessLoadedUrl : String = ""

    private lateinit var mAdview: AdView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        mContext = this
        //setActiveFullScreen()

        initView()
        MobileAds.initialize(this) {}
        //initSliderMenu()

        val mItemURL = intent.getStringExtra("Tag")
        loadWebView(mItemURL.toString())


        ThemeConfig(mContext, this).initThemeColor()
        ThemeConfig(mContext, this).initThemeStyle()


        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PERMISSION_DIALOG_ACTIVE, true))
            //requestPermission()

        if (savedInstanceState == null) {
            loadWebView(mItemURL.toString())
        }

        initClickEvent()

        initAdMob()

        initOneSignal()

        initExtraConfig()
    }

    private fun initExtraConfig(){
        // if you need any customization
    }

    @SuppressLint("RestrictedApi")
    private fun initView(){
        txt_no_internet_title.text = PreferenceUtils.getInstance().getStringValue(Constants.KEY_NO_INTERNET_TITLE, "")
        txt_no_internet_detail.text = PreferenceUtils.getInstance().getStringValue(Constants.KEY_NO_INTERNET_DETAILS, "")
        btn_try_again.text = PreferenceUtils.getInstance().getStringValue(Constants.KEY_TRY_AGAIN_TEXT, getString(R.string.label_tryAgain))
        txt_error_title.text = PreferenceUtils.getInstance().getStringValue(Constants.KEY_ERROR_TITLE, "")
        txt_error_detail.text = PreferenceUtils.getInstance().getStringValue(Constants.KEY_ERROR_TEXT, "")
        btn_error_try_again.text = PreferenceUtils.getInstance().getStringValue(Constants.KEY_TRY_AGAIN_TEXT, getString(R.string.label_tryAgain))


        // fullscreen setup
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_FULL_SCREEN_ACTIVE, false)){
            //setActiveFullScreen()
        }

        // pull to refresh enable
        swap_view.isEnabled = PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PULL_TO_REFRESH_ENABLE, false)

        mDownloadManger = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // webview swap conflict
        if(PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PULL_TO_REFRESH_ENABLE, false)) {
            web_view.viewTreeObserver.addOnScrollChangedListener {
                this.swap_view.isEnabled = web_view.scrollY === 0
            }
        }

        // on full screen keyboard showing status bar issue
        root_container.viewTreeObserver.addOnGlobalLayoutListener {
            if (root_container != null) {
                val heightDiff: Int = root_container.rootView.height - root_container.height
                if (heightDiff > ViewUtils.dpToPx(mContext, 100)) {
                    //setActiveFullScreen()
                } else {
                    //setActiveFullScreen()
                }
            }
        }

        /**
         * Turn on/off dark mode
         * Flag for off: AppCompatDelegate.MODE_NIGHT_NO
         * Flag for on: AppCompatDelegate.MODE_NIGHT_YES
         */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    }

    private fun initOneSignal(){
        try {
            OneSignal.startInit(mContext)
                    .setNotificationOpenedHandler(OneSignalNotificationOpenedHandler(mContext))
                    .autoPromptLocation(true)
                    .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                    .unsubscribeWhenNotificationsAreDisabled(true)
                    .init()

            val userId = OneSignal.getPermissionSubscriptionState().subscriptionStatus.userId
            if (userId != "") {
                UtilMethods.printLog(TAG, "userId = $userId")
                PreferenceUtils.editStringValue(Constants.KEY_ONE_SIGNAL_USER_ID, userId)
            }
        }catch (ex: Exception){
            UtilMethods.printLog(TAG, ""+ex.message)
        }
    }


    private fun initAdMob(){
        //MobileAds.initialize(this)
        // Banner ad
        val adMobBannerID = getString(R.string.admob_banner_id)
        if(adMobBannerID!!.isEmpty()){
            layout_footer.visibility = View.GONE
            Log.d(TAG, "Banner adMob ID is empty!")
        }else {
            initBannerAdMob(adMobBannerID)
        }
    }

    /**
     * Set adMob with Banner ID which get from adMob account
     * When ID is "" (empty) then ad will hide
     * */
    private fun initBannerAdMob(adMobBannerID: String){
        val adMobBanner = AdView(this)
        adMobBanner.adSize = AdSize.BANNER
        adMobBanner.adUnitId = adMobBannerID
        val adRequest: AdRequest =  AdRequest.Builder()
                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                //.addTestDevice("ab890a35-8bba-430a-b612-502d42ca8174")
                .build()

        adMobBanner.adListener = object: AdListener() {
            override fun onAdLoaded() {
                runOnUiThread {
                    view_admob.visibility = View.VISIBLE
                    if(layout_footer.visibility == View.GONE){
                        layout_footer.visibility = View.VISIBLE
                        val slideUp: Animation  = AnimationUtils.loadAnimation(mContext, R.anim.anim_slide_up)
                        layout_footer.startAnimation(slideUp)
                    }
                }
            }

            override fun onAdFailedToLoad(errorCode : Int) {
                // Code to be executed when an ad request fails.
                UtilMethods.printLog(TAG, "Banner ad error code: $errorCode")

                Handler().postDelayed({
                    if(isApplicationAlive)
                        initBannerAdMob(adMobBannerID)
                }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,10000).toLong())


            }

            override fun onAdLeftApplication() {
                super.onAdLeftApplication()
                isApplicationAlive = false
            }
        }

/*
        if(isApplicationAlive) {
            adMobBanner.loadAd(adRequest)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            view_admob.addView(adMobBanner, params)
        }
*/


        Handler().postDelayed({
            ->
            if(isApplicationAlive) {
                adMobBanner.loadAd(adRequest)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                view_admob.addView(adMobBanner, params)
            }
        }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,10000).toLong())



    }

    private fun initInterstitialAdMob(interstitialAdID : String){
        // setup interstitial ad
        val requestBuilder = AdRequest.Builder()
        mInterstitial = InterstitialAd(mContext)
        mInterstitial.adUnitId = interstitialAdID
        mInterstitial.loadAd(requestBuilder.build())
        val parentLayout: View = findViewById(android.R.id.content)
        Snackbar.make(parentLayout, "Loading Ads, Please Wait...", Snackbar.LENGTH_LONG)
                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                .show()
                mInterstitial.adListener = object: AdListener() {
                    override fun onAdLoaded() {
                        mInterstitial.show()
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        /*
                        mSmartAdsIncrement += 40000

                        Handler().postDelayed({
                            if(isApplicationAlive) {
                                initInterstitialAdMob(interstitialAdID)
                            }
                        }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,
                                1 * 60 * 10000).toLong() + mSmartAdsIncrement)

                         */
                    }

                    override fun onAdFailedToLoad(errorCode: Int) {
                        Log.e(TAG, "Admob Interstitial error -$errorCode")
                        /*
                        Handler().postDelayed({
                            if(isApplicationAlive) {
                                initInterstitialAdMob(interstitialAdID)
                            }
                        }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,
                                1 * 60 * 10000).toLong() + mSmartAdsIncrement)
                         */
                    }
                    override fun onAdLeftApplication() {
                        super.onAdLeftApplication()
                        isApplicationAlive = false
                    }
                }


    }

    private fun initRewardedAdMob(rewardedAdID : String){
        // setup Rewarded ad
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.loadAd(rewardedAdID, AdRequest.Builder().build())
        val parentLayout: View = findViewById(android.R.id.content)
        Snackbar.make(parentLayout, "Loading Ads, Please Wait...", Snackbar.LENGTH_LONG)
                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                .show()
            mRewardedVideoAd.rewardedVideoAdListener = object: RewardedVideoAdListener {
            override fun onRewardedVideoCompleted() {}

            override fun onRewardedVideoAdLoaded() {
                mRewardedVideoAd.show()


            }

            override fun onRewardedVideoAdOpened() {}

            override fun onRewardedVideoStarted() {}

            override fun onRewardedVideoAdClosed() {
                /*
                mSmartAdsIncrement += 60000

                //loadWebView("https://www.eyewitness.global/welcome")

                Handler().postDelayed({
                    if(isApplicationAlive) {
                        initRewardedAdMob(rewardedAdID)
                    }
                }, (PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,
                        2 * 60 * 10000).toLong()) + mSmartAdsIncrement)

                 */
            }

            override fun onRewarded(rewardItem: RewardItem) {}

            override fun onRewardedVideoAdLeftApplication() {}

            override fun onRewardedVideoAdFailedToLoad(i: Int) {
                Log.d(TAG, "RewardedAd failed error - $i")
                /*
                Handler().postDelayed({
                    if(isApplicationAlive) {
                        initRewardedAdMob(rewardedAdID)
                    }
                }, (PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,
                        2 * 60 * 10000).toLong()) + mSmartAdsIncrement)

                 */
            }
        }
    }

    private fun showWebView(){
        web_view.visibility = View.VISIBLE
    }
    private fun hideWebView(){
        web_view.visibility = View.VISIBLE
    }

    private fun showErrorView(){
        layout_error.visibility = View.VISIBLE
    }

    private fun hideErrorView(){
        layout_error.visibility = View.GONE
    }

    private fun showNoInternet(){
        layout_no_internet.visibility = View.VISIBLE
    }

    private fun hideNoInternet(){
        layout_no_internet.visibility = View.GONE
    }

    private fun showLoader() {
        if( PreferenceUtils.getInstance().getStringValue(Constants.KEY_LOADER, Constants.LOADER_HIDE) != Constants.LOADER_HIDE)
            layout_progress.visibility = View.VISIBLE
    }

    private fun hideLoader(){
        layout_progress.visibility = View.GONE
    }

    private fun initClickEvent() {
        // try again
        btn_try_again.setOnClickListener {
            if(UtilMethods.isConnectedToInternet(mContext)){
                // request for show website
                //webviewReload()
                web_view.reload()
            }else{
                UtilMethods.showSnackbar(root_container, getString(R.string.massage_nointernet))
            }
        }

        swap_view.setOnRefreshListener {
            //webviewReload()
            web_view.reload()
            Handler().postDelayed({
                swap_view.isRefreshing = false
            }, 2000)
        }


        // on error reload again
        btn_error_try_again.setOnClickListener{
            // request for reload again website
            //successLoadedUrl = ""
            isViewLoaded = false
            //web_view.clearCache(true)
            //web_view.clearHistory()
            if (mSuccessLoadedUrl != ""){
                loadWebView(mSuccessLoadedUrl)
            }else if (mLastUrl != ""){
                loadWebView(mLastUrl)
            }else {
                val mItemURL = intent.getStringExtra("Tag")
                loadWebView(mItemURL.toString())
            }
        }

        // on error go home
        btn_error_home.setOnClickListener {
            // request for reload again website
            mSuccessLoadedUrl = ""
            isViewLoaded = false
            web_view.clearCache(true)
            web_view.clearHistory()

            val mItemURL = intent.getStringExtra("Tag")
            loadWebView(mItemURL.toString())
        }


        // show or hide adMob
        btn_ad_show.setOnClickListener {
            if(view_admob.visibility == View.GONE){
                view_admob.visibility = View.VISIBLE
                img_ad_show.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_down_arrow))
            }else{
                view_admob.visibility = View.GONE
                img_ad_show.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_up_arrow))
            }
        }
    }

    private fun loadBaseWebView() {
        initDefaultURL()

        if(AppDataInstance.deepLinkUrl.isNotEmpty()){
            loadWebView(AppDataInstance.deepLinkUrl)
            AppDataInstance.deepLinkUrl = ""
        } else if(AppDataInstance.notificationUrl.isNotEmpty()) {
            when (Constants.WEBVIEW_OPEN_TYPE.valueOf(AppDataInstance.notificationUrlOpenType.toUpperCase())){
                Constants.WEBVIEW_OPEN_TYPE.EXTERNAL -> {
                    loadWebView(mDefaultURL)
                    Handler().postDelayed({
                        UtilMethods.browseUrlExternal(mContext, AppDataInstance.notificationUrl)
                        AppDataInstance.notificationUrl = ""
                    }, 5000)
                }
                Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB -> {
                    loadWebView(mDefaultURL)
                    Handler().postDelayed({
                        UtilMethods.browseUrlCustomTab(mContext, AppDataInstance.notificationUrl)
                        AppDataInstance.notificationUrl = ""
                    }, 5000)
                }
                else -> {
                    loadWebView(AppDataInstance.notificationUrl)
                    AppDataInstance.notificationUrl = ""
                }
            }
        }else {
            loadWebView(mDefaultURL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView(loadUrl: String) {
        showWebView()
        hideNoInternet()
        hideLoader()
        hideErrorView()

        initConfigureWebView(web_view, getString(R.string.user_agent_string))
        initWebClient()
        initChromeClient()

        // set download listener
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_DOWNLOADS_IN_WEBVIEW_ACTIVE, true)) {
            web_view.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                if (Build.VERSION.SDK_INT >= 23) {
                    if (askForPermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE, true)) {
                        //UtilMethods.browseUrlCustomTab(mContext, url)
                        startDownload(url, userAgent, mimetype)
                    }
                } else {
                    //UtilMethods.browseUrlCustomTab(mContext, url)
                    startDownload(url, userAgent, mimetype)
                }
            }
        } else{
            web_view.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                UtilMethods.browseUrlCustomTab(mContext, url)
            }
        }

        web_view.loadUrl(loadUrl) //Add zoom option
        web_view.getSettings().setBuiltInZoomControls(true);
        web_view.getSettings().setDisplayZoomControls(false);
        web_view.getSettings().setSupportZoom(true);
        web_view.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        web_view.setScrollbarFadingEnabled(false);
    }

    private fun webviewReload(){
        isViewLoaded = false
        //web_view.loadUrl("about:blank")
        web_view.clearCache(true)
        web_view.clearHistory()
        val parentLayout: View = findViewById(android.R.id.content)
        Snackbar.make(parentLayout, "Refreshing, Please Wait...", Snackbar.LENGTH_LONG)
                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                .show()
        web_view.reload()
    }

    //not used, optional
    fun clearCache() {
        web_view.clearCache(true)
        this.deleteDatabase("webview.db")
        this.deleteDatabase("webviewCache.db")
        web_view.clearCache(false)
    }

    private fun initWebClient(){
        web_view.webViewClient = object : WebViewClient() {
            // shouldOverrideUrlLoading only call on real device
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val url: String = url!!

                UtilMethods.printLog(TAG, "URL: $url")

                // already success loaded
                if(mSuccessLoadedUrl == url || url.startsWith("file:///android_asset")){
                    UtilMethods.printLog(TAG, "Page already loaded!")
                    return false
                }

                if (url.contains("google.navigation:")){
                    val gmmIntentUri = Uri.parse(url)
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                    return true
                }

                if (UtilMethods.isConnectedToInternet(mContext)) {
                    if (url.startsWith("http:") || url.startsWith("https:")) {
                        val host = Uri.parse(url).host?:""

                        /*if (host.contains("m.facebook.com")
                                || host.contains("facebook.co")
                                || host.contains("www.facebook.com")){

                            val activityCode = 104
                            val intent = Intent()
                            intent.setClassName("com.facebook.katana", "com.facebook.katana.ProxyAuth")
                            intent.putExtra("client_id", application.packageName)
                            startActivityForResult(intent, activityCode)
                            return true
                        }*/


                        if (url.contains("drive.google.com")){
                            UtilMethods.browseUrlCustomTab(mContext, url)
                            return true
                        }

                        if (url.contains("google.com/maps") ||
                                url.contains("maps.app.goo.gl") ||
                                url.contains("maps.google.com")){
                            UtilMethods.browseUrlExternal(mContext, url)
                            return true
                        }

                        if(host.contains("m.facebook.com")
                                || host.contains("facebook.co")
                                || host.contains("www.facebook.com")
                                || host.contains(".google.com")
                                || host.contains(".google.co")
                                || host.contains("accounts.google.com")
                                || host.contains("accounts.google.co.in")
                                || host.contains("www.accounts.google.com")
                                || host.contains("www.twitter.com")
                                || host.contains("secure.payu.in")
                                || host.contains("https://secure.payu.in")
                                || host.contains("oauth.googleusercontent.com")
                                || host.contains("content.googleapis.com")
                                || host.contains("ssl.gstatic.com")){
                             /*val intent = Intent(Intent.ACTION_VIEW)
                             intent.data = Uri.parse(url)
                             startActivity(intent)
                             return true*/
                            showLoader()
                            return false
                        } else if (host.contains ("t.me")) {
                            UtilMethods.browseUrlExternal (mContext, url)
                            return true
                        } else if (host == Uri.parse(mDefaultURL).host){
                            if (mWebviewPop != null) {
                                mWebviewPop!!.visibility = View.GONE
                                //frame_web_view.removeView(mWebviewPop)
                                mWebviewPop = null
                            }
                        }
                        showLoader()
                        return false
                    } else if (url.startsWith("tel:")) {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("sms:")) {
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    }else if (url.startsWith("mailto:")) {
                        val mail = Intent(Intent.ACTION_SEND)
                        mail.type = "message/rfc822"

                        val mailTo: MailTo = MailTo.parse(url)
                        //val addressMail = url.replace("mailto:", "")
                        mail.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailTo.to))
                        mail.putExtra(Intent.EXTRA_CC, mailTo.cc)
                        mail.putExtra(Intent.EXTRA_SUBJECT, mailTo.subject)
                        //mail.putExtra(Intent.EXTRA_TEXT, mailTo.body)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if(url.contains("intent:")){
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                            //try to find fallback url
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (fallbackUrl != null) {
                                web_view.loadUrl(fallbackUrl)
                                return true
                            }
                            //invite to install
                            val marketIntent = Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("market://details?id=" + intent.getPackage()!!))
                            if (marketIntent.resolveActivity(packageManager) != null) {
                                try{
                                    startActivity(intent)
                                }catch (ex: ActivityNotFoundException){
                                    UtilMethods.showShortToast(mContext, "Activity Not Found")
                                }
                                return true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    } else if(url.contains("whatsapp://") || url.contains("app.whatsapp")){
                        try{
                            val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            sendIntent.setPackage("com.whatsapp")
                            startActivity(sendIntent);
                        }catch (ex: Exception){
                            ex.printStackTrace()
                        }
                        return true;

                    } else if (url.contains("facebook.com/sharer")||
                            url.contains("twitter.com/intent")||
                            url.contains("plus.google.com")||
                            url.contains("pinterest.com/pin")){
                        UtilMethods.browseUrlExternal(mContext, url)
                        return true
                    } else if (url.contains("geo:")
                            || url.contains("market://")
                            || url.contains("market://")
                            || url.contains("play.google")
                            || url.contains("vid:")
                            || url.contains("youtube")
                            || url.contains("fb-messenger")
                            || url.contains("?target=external")) {

                        UtilMethods.browseUrlCustomTab(mContext, url)
                        return true
                    }
                    showLoader()
                    return false
                } else {
                    hideWebView()
                    showNoInternet()
                    hideLoader()
                }
                mLastUrl = url
                showLoader()
                return false
                //return super.shouldOverrideUrlLoading(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url: String = request.url.toString()

                UtilMethods.printLog(TAG, "URL: $url")

                // already success loaded
                if(mSuccessLoadedUrl == url || url.startsWith("file:///android_asset")){
                    UtilMethods.printLog(TAG, "Page already loaded!")
                    return false
                }

                if (url.contains("google.navigation:")){
                    val gmmIntentUri = Uri.parse(url)
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                    return true
                }

                if (UtilMethods.isConnectedToInternet(mContext)) {
                    if (url.startsWith("http:") || url.startsWith("https:")) {
                        val host = Uri.parse(url).host?:""

                        /*if (host.contains("m.facebook.com")
                              || host.contains("facebook.co")
                              || host.contains("www.facebook.com")){

                          val activityCode = 104
                          val intent = Intent()
                          intent.setClassName("com.facebook.katana", "com.facebook.katana.ProxyAuth")
                          intent.putExtra("client_id", application.packageName)
                          startActivityForResult(intent, activityCode)
                          return true
                      }*/


                        if (url.contains("drive.google.com")){
                            UtilMethods.browseUrlCustomTab(mContext, url)
                            return true
                        }

                        if (url.contains("google.com/maps") ||
                                url.contains("maps.app.goo.gl") ||
                                url.contains("maps.google.com")){
                            UtilMethods.browseUrlExternal(mContext, url)
                            return true
                        }

                        if(host.contains("m.facebook.com")
                                || host.contains("facebook.co")
                                || host.contains("www.facebook.com")
                                || host.contains(".google.com")
                                || host.contains(".google.co")
                                || host.contains("accounts.google.com")
                                || host.contains("accounts.google.co.in")
                                || host.contains("www.accounts.google.com")
                                || host.contains("www.twitter.com")
                                || host.contains("secure.payu.in")
                                || host.contains("https://secure.payu.in")
                                || host.contains("oauth.googleusercontent.com")
                                || host.contains("content.googleapis.com")
                                || host.contains("ssl.gstatic.com")){
                            /* val intent = Intent(Intent.ACTION_VIEW)
                             intent.data = Uri.parse(url)
                             startActivity(intent)
                             return true*/
                            showLoader()
                            return false
                        } else if (host.contains ("t.me")) {
                            UtilMethods.browseUrlExternal (mContext, url)
                            return true
                        } else if (host == Uri.parse(mDefaultURL).host){
                            if (mWebviewPop != null) {
                                mWebviewPop!!.visibility = View.GONE
                                //frame_web_view.removeView(mWebviewPop)
                                mWebviewPop = null
                            }
                        }
                        showLoader()
                        return false
                    } else if (url.startsWith("tel:")) {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("sms:")) {
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("mailto:")) {
                        val mail = Intent(Intent.ACTION_SEND)
                        mail.type = "message/rfc822"

                        val mailTo: MailTo = MailTo.parse(url)
                        mail.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailTo.to))
                        mail.putExtra(Intent.EXTRA_CC, mailTo.cc)
                        mail.putExtra(Intent.EXTRA_SUBJECT, mailTo.subject)
                        //mail.putExtra(Intent.EXTRA_TEXT, mailTo.body)
                        try{
                            startActivity(mail)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if(url.contains("intent:")){
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                            //try to find fallback url
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (fallbackUrl != null) {
                                web_view.loadUrl(fallbackUrl)
                                return true
                            }
                            //invite to install
                            val marketIntent = Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("market://details?id=" + intent.getPackage()!!))
                            if (marketIntent.resolveActivity(packageManager) != null) {
                                try{
                                    startActivity(intent)
                                }catch (ex: ActivityNotFoundException){
                                    UtilMethods.showShortToast(mContext, "Activity Not Found")
                                }
                                return true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    } else if(url.contains("whatsapp://") || url.contains("app.whatsapp")){
                        try{
                            val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            sendIntent.setPackage("com.whatsapp")
                            startActivity(sendIntent);
                        }catch (ex: Exception){
                            ex.printStackTrace()
                        }
                        return true;

                    } else if (url.contains("facebook.com/sharer")||
                            url.contains("twitter.com/intent")||
                            url.contains("plus.google.com")||
                            url.contains("pinterest.com/pin")){
                        UtilMethods.browseUrlExternal(mContext, url)
                        return true
                    } else if (url.contains("geo:")
                            || url.contains("market://")
                            || url.contains("market://")
                            || url.contains("play.google")
                            || url.contains("vid:")
                            || url.contains("youtube")
                            || url.contains("fb-messenger")
                            || url.contains("?target=external")) {

                        UtilMethods.browseUrlCustomTab(mContext, url)
                        return true
                    }
                    showLoader()
                    return false
                } else {
                    hideWebView()
                    showNoInternet()
                    hideLoader()
                }
                mLastUrl = url
                showLoader()
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (UtilMethods.isConnectedToInternet(mContext) || url.startsWith("file:///android_asset")) {
                    isViewLoaded = false
                    showWebView()
                    hideErrorView()
                    showLoader()

                    Handler().postDelayed({
                        hideNoInternet()
                        if(layout_progress.visibility == View.VISIBLE){
                            hideLoader()
                        }
                    }, PreferenceUtils.getInstance().getIntegerValue(Constants.KEY_LOADER_DELAY, 1000).toLong())
                }else {
                    hideWebView()
                    showNoInternet()
                    hideLoader()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                manageCookie(url, web_view)

                if(layout_error.visibility != View.VISIBLE){
                    showWebView()
                }

                isViewLoaded = true
                hideLoader()
                mSuccessLoadedUrl = url

                if (url.startsWith("https://m.facebook.com/v2.7/dialog/oauth")) {
                    if (mWebviewPop != null) {
                        mWebviewPop!!.visibility = View.GONE
                        mWebviewPop = null
                    }
                    web_view.loadUrl(mLastUrl)
                    return
                }

                // Load Ads after page finish loading
                if (url.equals("https://www.eyewitness.global/welcomescholarship-search/",true) || url.equals("https://www.waecdirect.org/",true)
                        || url.equals("https://ghana.waecdirect.org/",true) || url.equals("https://app.waecgambia.org/resultchecker/resultchecker.aspx",true)
                        || url.equals("https://results.liberiawaec.org/",true) || url.equals("https://www.waecsierra-leone.org/ResultChecker/",true)
                        || url.equals("https://result.neco.gov.ng/",true) || url.equals("https://eworld.nabteb.gov.ng/",true)
                        || url.equals("https://www.jamb.org.ng/examslipprinting2/CheckUTMEResults",true) || url.equals("https://www.eyewitness.global/welcomeconsultation/", true)) {
                    // Interstitial ad
                    val interstitialAdID = getString(R.string.admob_interstitial_id)
                    if(interstitialAdID!!.isEmpty()){
                        Log.d(TAG, "Interstitial adMob ID is empty!")
                    }else{
                        // Show interstitial Ads in some specific web page
                        initInterstitialAdMob(interstitialAdID)
                    }

                }

                if (url.equals("https://html5games.com/",true) || url.equals("https://m.youtube.com/",true)
                        || url.equals("https://verify.nysc.org.ng/",true) || url.equals("https://waeconline.org.ng/verify.beta",true)
                        || url.equals("https://tubitv.com/home/",true)) {
                    // Rewarded ad
                    val rewardedAdID = getString(R.string.admob_rewarded_id)
                    if(rewardedAdID!!.isEmpty()){
                        Log.d(TAG, "Rewarded adMob ID is empty!")
                    }else{
                        // Show interstitial Ads in some specific web page
                        initRewardedAdMob(rewardedAdID)

                    }

                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    UtilMethods.printLog(TAG, "${error!!.description}")

                    if(error.description.contains("net::ERR_UNKNOWN_URL_SCHEME")
                            || error.description.contains("net::ERR_FILE_NOT_FOUND")
                            || error.description.contains("net::ERR_CONNECTION_REFUSED")){
                        hideWebView()
                        hideLoader()
                        return
                    }else if(error.description.contains("net::ERR_TIMED_OUT") ||
                            error.description.contains("net::ERR_CONNECTION_RESET")) {
                        showErrorView()
                        return
                    }else if(error.description.contains("net::ERR_NAME_NOT_RESOLVED")) {
                        // webviewReload()
                        return
                    }else if(error.description.contains("net::ERR_CONNECTION_TIMED_OUT")){
                        // show offline page
                        return
                    }else if(error.description.contains("net::ERR_CLEARTEXT_NOT_PERMITTED")){
                        return
                    }else if(error.description.contains("net::ERR_INTERNET_DISCONNECTED")){
                        //hideWebView()
                        showNoInternet()
                    }
                }else{
                    UtilMethods.printLog(TAG, error.toString())
                }
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                UtilMethods.printLog(TAG, "HTTP Error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                UtilMethods.printLog(TAG, "SSL Error: ${error.toString()}")
            }

        }
    }

    private fun initChromeClient() {
        var mCustomView: View? = null
        var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
        var mOriginalOrientation: Int = 0
        var mOriginalSystemUiVisibility: Int = 0

        web_view.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                    webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams): Boolean {

                UtilMethods.printLog(TAG, fileChooserParams.acceptTypes[0])
                if(fileChooserParams.acceptTypes.isNotEmpty()) {
                    if(!askForPermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE, true)){
                        return false
                    }

                    if (!PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_FILE_UPLOAD_ENABLE, true)) {
                        UtilMethods.printLog(TAG, "File upload disable!")
                    }

                    if (mFilePath != null) {
                        mFilePath!!.onReceiveValue(null)
                    }
                    mFilePath = filePathCallback
                    var fileUploadIntent: Intent? = null

                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)

                    if (fileChooserParams.acceptTypes[0].contains("audio")) {
                        if (askForPermission(PERMISSIONS_REQUEST_MICROPHONE, true)){
                            contentSelectionIntent.type = fileChooserParams.acceptTypes[0]
                            //contentSelectionIntent.type = "audio/*"
                        } else {
                            return false
                        }
                    }else if (fileChooserParams.acceptTypes[0].contains("video")
                            || fileChooserParams.acceptTypes[0].contains("mp4")
                            || fileChooserParams.acceptTypes[0].contains("webm")
                            || fileChooserParams.acceptTypes[0].contains("mpg")
                            || fileChooserParams.acceptTypes[0].contains("wmv")
                            || fileChooserParams.acceptTypes[0].contains("mov")
                            || fileChooserParams.acceptTypes[0].contains("mpe")
                            || fileChooserParams.acceptTypes[0].contains("ogg")
                            || fileChooserParams.acceptTypes[0].contains("mpeg")) {
                        if (askForPermission(PERMISSIONS_REQUEST_CAMERA, true)) {
                            fileUploadIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

                            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                            contentSelectionIntent.type = fileChooserParams.acceptTypes[0]
                            //contentSelectionIntent.type = "video/*"
                            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_MULTIPLE_FILE_UPLOAD_ENABLE, false)) {
                                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                        } else {
                            return false
                        }
                    }else if (fileChooserParams.acceptTypes[0].contains("image")
                            || fileChooserParams.acceptTypes[0].contains("jpg")
                            || fileChooserParams.acceptTypes[0].contains("jpeg")
                            || fileChooserParams.acceptTypes[0].contains("gif")
                            || fileChooserParams.acceptTypes[0].contains("webp")
                            || fileChooserParams.acceptTypes[0].contains("tiff")
                            || fileChooserParams.acceptTypes[0].contains("bmp")
                            || fileChooserParams.acceptTypes[0].contains("ico")
                            || fileChooserParams.acceptTypes[0].contains("pjp")
                            || fileChooserParams.acceptTypes[0].contains("svg")
                            || fileChooserParams.acceptTypes[0].contains("tif")
                            || fileChooserParams.acceptTypes[0].contains("png") ) {
                        if (askForPermission(PERMISSIONS_REQUEST_CAMERA, true)) {
                            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PHOTO_UPLOAD_ENABLE, true)) {
                                fileUploadIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                if (fileUploadIntent.resolveActivity(this@HomeActivity.packageManager) != null) {
                                    var photoFile: File? = null
                                    try {
                                        photoFile = createImageFile()
                                        fileUploadIntent.putExtra("PhotoPath", mFileCamMessage)
                                    } catch (ex: IOException) {
                                        Log.e(TAG, "Image file creation failed", ex)
                                    }

                                    if (photoFile != null) {
                                        mFileCamMessage = "file:" + photoFile.absolutePath
                                        fileUploadIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                                    } else {
                                        fileUploadIntent = null
                                    }
                                }

                                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)

                                contentSelectionIntent.type = fileChooserParams.acceptTypes[0]
                                //contentSelectionIntent.type = "image/*"
                                if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_MULTIPLE_FILE_UPLOAD_ENABLE, false)) {
                                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }
                            }
                        } else {
                            return false
                        }
                    }else if (fileChooserParams.acceptTypes[0].contains("doc")
                            || fileChooserParams.acceptTypes[0].contains("docx")
                            || fileChooserParams.acceptTypes[0].contains("pdf")){
                        contentSelectionIntent.type = "application/*"
                    }else {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        contentSelectionIntent.type = "*/*"
                    }

                    val intentArray: Array<Intent?>
                    if (fileUploadIntent != null) {
                        intentArray = arrayOf(fileUploadIntent)
                    } else {
                        intentArray = arrayOfNulls(0)
                    }

                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.label_file_chooser))
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    startActivityForResult(chooserIntent, FILE_CHOOSER)

                    return true
                }
                return false

            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (Build.VERSION.SDK_INT < 23 || askForPermission(PERMISSIONS_REQUEST_LOCATION, true)) {
                    // location permissions were granted previously so auto-approve
                    callback.invoke(origin, true, false)
                    locationSettingsRequest()
                }
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val newWebView = WebView(mContext)
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.setSupportZoom(true)
                newWebView.settings.builtInZoomControls = true
                newWebView.settings.pluginState = WebSettings.PluginState.ON
                newWebView.settings.setSupportMultipleWindows(true)
                view!!.addView(newWebView)
                val transport = resultMsg!!.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()

                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        view.loadUrl(url)
                        return true
                    }
                }

                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                /**
                 * Set progress view
                 *
                 * progress.setProgress(newProgress);
                if (newProgress == 100) {
                progress.setProgress(0);
                }**/
            }

            override fun onShowCustomView(paramView: View?, paramCustomViewCallback: CustomViewCallback) {
                if (mCustomView != null) {
                    onHideCustomView()
                    return
                }
                mCustomView = paramView
                mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
                mOriginalOrientation = requestedOrientation
                mCustomViewCallback = paramCustomViewCallback
                (window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
                window.decorView.systemUiVisibility = 3846

            }


            override fun onHideCustomView() {
                (window.decorView as FrameLayout).removeView(mCustomView)
                mCustomView = null
                window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                requestedOrientation = mOriginalOrientation
                mCustomViewCallback?.onCustomViewHidden()
                mCustomViewCallback = null
            }

            override fun onPermissionRequest(permissionRequest: PermissionRequest?) {
                mPermissionRequest = permissionRequest

                UtilMethods.printLog(TAG, "onJSPermissionRequest")
                mJsRequestCount = permissionRequest?.resources?.size ?: 0
                for (request in permissionRequest?.resources!!) {
                    UtilMethods.printLog(TAG, "AskForPermission for" + permissionRequest.origin.toString() + "with" + request)
                    when (request) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> askForPermission(PERMISSIONS_REQUEST_MICROPHONE, true)
                        "android.webkit.resource.VIDEO_CAPTURE" -> askForPermission(PERMISSIONS_REQUEST_CAMERA, true)
                    }
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        @SuppressLint("SimpleDateFormat")
        val fileName = SimpleDateFormat("yyyy_mm_ss").format(Date())
        val newName = "file_" + fileName + "_"
        val sdDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(newName, ".jpg", sdDirectory)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun generateKey(): SecretKey {
        val random = SecureRandom()
        val key = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0)
        //random.nextBytes(key)
        return SecretKeySpec(key, "AES")
    }

    // download manager
    internal var mDownloadCompleteListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            clearDownloadingState()
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val fileUri = mDownloadManger!!.getUriForDownloadedFile(id)
        }
    }

    private fun startDownload(url: String, userAgent: String, mimetype: String) {
        val fileUri = Uri.parse(url)
        val fileName = fileUri.lastPathSegment
        val cookies = CookieManager.getInstance().getCookie(url)

        try {
            val request = DownloadManager.Request(fileUri)
            request.setMimeType(mimetype)
                    .addRequestHeader("cookie", cookies)
                    .addRequestHeader("User-Agent", userAgent)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            UtilMethods.showLongToast(mContext, "Downloading File")
        }catch (ex: java.lang.Exception){
            UtilMethods.showLongToast(mContext, "Download failed!")
            UtilMethods.printLog(TAG, "${ex.message}")
        }
    }


    //cancel download, no active
    protected fun cancelDownload() {
        if (mOnGoingDownload != null) {
            mDownloadManger!!.remove(mOnGoingDownload!!)
            clearDownloadingState()
        }
    }

    protected fun clearDownloadingState() {
        unregisterReceiver(mDownloadCompleteListener)
        //mCancel.setVisibility(View.GONE);
        mOnGoingDownload = null
    }

    // get file callback
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == UPDATE_IMMEDIATE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    UtilMethods.showLongToast(mContext, "Application update successfully!")
                }
                Activity.RESULT_CANCELED -> {
                    UtilMethods.showLongToast(mContext, "Application update is mandatory!")
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    UtilMethods.showLongToast(mContext, "Application update is mandatory!")
                }
            }
        }else if (resultCode == REQUEST_CHECK_SETTINGS){
            getLocation()
            return
        }

        if (Build.VERSION.SDK_INT >= 21) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(mContext, R.color.colorPrimary)
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FILE_CHOOSER) {
                    if (mFilePath == null) {
                        return
                    }
                    if (intent == null) {
                        if (mFileCamMessage != null) {
                            results = arrayOf(Uri.parse(mFileCamMessage))
                        }
                    } else {
                        val dataString = intent.dataString
                        if (intent.clipData != null) {
                            val numSelectedFiles: Int = intent.clipData?.itemCount?: 0
                            results = Array(numSelectedFiles) { Uri.EMPTY}
                            for (i in 0 until numSelectedFiles) {
                                results[i] = intent.clipData!!.getItemAt(i).uri
                            }
                        }else if (dataString != null) {
                            results = arrayOf(Uri.parse(dataString))
                        }else{
                            UtilMethods.printLog(TAG, "Image upload data is empty!")
                        }
                    }
                }
            }
            mFilePath?.onReceiveValue(results)
            mFilePath = null
        } else {
            if (requestCode == FILE_CHOOSER) {
                if (null == mFileMessage) return
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                mFileMessage?.onReceiveValue(result)
                mFileMessage = null
            }
        }
    }

    private fun requestPermission(){
        var mIndex: Int = -1
        val requestList: Array<String> = Array(10, { "" } )

        // Access photos Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        // Location Permission
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.ACCESS_FINE_LOCATION
        }else{
            getLocation()
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.CAMERA
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.RECORD_AUDIO
        }

        if(mIndex != -1){
            ActivityCompat.requestPermissions(this, requestList, PERMISSIONS_REQUEST_ALL)
        }
    }

    private fun jsPermissionAccepted(){
        mJsRequestCount --
        if (mPermissionRequest != null && mJsRequestCount == 0){
            mPermissionRequest!!.grant(mPermissionRequest!!.resources)
        }
    }
    private fun askForPermission(permissionCode: Int, request: Boolean): Boolean{
        when(permissionCode){
            PERMISSIONS_REQUEST_LOCATION ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION)){
                            UtilMethods.showSnackbar(root_container, "Location permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_CAMERA ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.CAMERA)){
                            UtilMethods.showSnackbar(root_container, "Camera permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                            UtilMethods.showSnackbar(root_container, "Write permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_MICROPHONE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.RECORD_AUDIO)) {
                            UtilMethods.showSnackbar(root_container, "Audio permission is required, Please allow from permission manager!!")
                        } else
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSIONS_REQUEST_MICROPHONE)
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ALL -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission accept location
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        UtilMethods.printLog(TAG, "External permission accept.")
                    }

                    // permission accept location
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        UtilMethods.printLog(TAG, "Location permission accept.")
                        getLocation()
                    }

                } else {
                    //UtilMethods.showSnackbar(root_container, "Permission Failed!")
                }
                return
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Write permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Write Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Camera permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Camera Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Location permission accept.")
                    getLocation()
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Location Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_MICROPHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Microphone Permission Accept.")
                    jsPermissionAccepted()
                }
                else {
                    UtilMethods.showSnackbar(root_container, "Microphone Permission Failed!")
                }
            }
        }
    }

    // get user location for
    private fun getLocation(): String {
        var newloc = "0,0"
        //Checking for location permissions
        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false)) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val gps = WebviewGPSTrack(mContext)
            val latitude = gps.getLatitude()
            val longitude = gps.getLongitude()
            if (gps.canGetLocation()) {
                if (latitude != 0.0 || longitude != 0.0) {
                    cookieManager.setCookie(mDefaultURL, "lat=$latitude")
                    cookieManager.setCookie(mDefaultURL, "long=$longitude")
                    newloc = "$latitude,$longitude"
                } else {
                    UtilMethods.printLog(TAG, "Location null.")
                }
            } else {
                UtilMethods.printLog(TAG, "Location read failed.")
            }
        }
        return newloc
    }

    private fun locationSettingsRequest() {
        val locationManager = mContext
                .getSystemService(Service.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false) && isGPSEnabled == false) {
            val mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000)

            val settingsBuilder = LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest)
            settingsBuilder.setAlwaysShow(true)

            val result = LocationServices.getSettingsClient(mContext)
                    .checkLocationSettings(settingsBuilder.build())
            result.addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                } catch (ex: ApiException) {

                    when (ex.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            Toast.makeText(mContext, "GPS IS OFF", Toast.LENGTH_SHORT).show()
                            val resolvableApiException = ex as ResolvableApiException
                            resolvableApiException.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {
                            Toast.makeText(mContext, "PendingIntent unable to execute request.", Toast.LENGTH_SHORT).show()

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            Toast.makeText(
                                    mContext,
                                    "Something is wrong in your GPS",
                                    Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    /*
    // News dialog
    private fun showNewsMagazines(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_news, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mNewsMagazinesPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mNewsMagazinesPopup.setBackgroundDrawable(colorDrawable)
        mNewsMagazinesPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mNewsMagazinesPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mNewsMagazinesPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mNewsMagazinesPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnNNews = popUpView.findViewById<View>(R.id.img_nnews) as ImageView
        val btnGNews = popUpView.findViewById<View>(R.id.img_gnews) as ImageView
        val btnBbc = popUpView.findViewById<View>(R.id.img_bbc) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        //txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        //txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")
        btnConfirm.setOnClickListener { mNewsMagazinesPopup.dismiss() }
        btnNNews.setOnClickListener {
            loadWebView("https://www.waecnigeria.org/news")
            mNewsMagazinesPopup.dismiss()
        }

        btnGNews.setOnClickListener {
            loadWebView("https://www.waecgh.org/all-news")
            mNewsMagazinesPopup.dismiss()
        }

        btnBbc.setOnClickListener {
            loadWebView("https://www.bbc.com/news/world/africa")
            mNewsMagazinesPopup.dismiss()
        }
    }


    // Verif dialog
    private fun showVerification(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_verif, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mVerifPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mVerifPopup.setBackgroundDrawable(colorDrawable)
        mVerifPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mVerifPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mVerifPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mVerifPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        //val btnWaec = popUpView.findViewById<View>(R.id.img_waec) as ImageView
        //val btnNysc = popUpView.findViewById<View>(R.id.img_nysc) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        //txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        //txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")
        btnConfirm.setOnClickListener { mVerifPopup.dismiss() }

        /*
        btnWaec.setOnClickListener {
            loadWebView("https://waeconline.org.ng/verify.beta")
            mVerifPopup.dismiss()
        }



        btnNysc.setOnClickListener {
            loadWebView("https://verify.nysc.org.ng/")
            mVerifPopup.dismiss()
        }

         */

    }
    */



    // Social Network dialog
    private fun showSocialNetwork(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_social, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mSocialNetworkPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mSocialNetworkPopup.setBackgroundDrawable(colorDrawable)
        mSocialNetworkPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mSocialNetworkPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSocialNetworkPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mSocialNetworkPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnFacebook = popUpView.findViewById<View>(R.id.img_facebook) as ImageView
        val btnTwitter = popUpView.findViewById<View>(R.id.img_twitter) as ImageView
        val btnTelegram = popUpView.findViewById<View>(R.id.img_telegram) as ImageView
        val btnYoutube = popUpView.findViewById<View>(R.id.img_youtube) as ImageView
        val btnQuora = popUpView.findViewById<View>(R.id.img_quora) as ImageView
        val btnSoundcloud = popUpView.findViewById<View>(R.id.img_soundcloud) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        //txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        //txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")
        btnConfirm.setOnClickListener { mSocialNetworkPopup.dismiss() }
        btnFacebook.setOnClickListener {
            loadWebView("https://mobile.facebook.com/")
            mSocialNetworkPopup.dismiss()
        }

        btnTwitter.setOnClickListener {
            loadWebView("https://mobile.twitter.com/")
            mSocialNetworkPopup.dismiss()
        }

        btnTelegram.setOnClickListener {
            loadWebView("https://web.telegram.org/")
            mSocialNetworkPopup.dismiss()
        }

        btnYoutube.setOnClickListener {
            loadWebView("https://m.youtube.com/")
            mSocialNetworkPopup.dismiss()
        }

        btnQuora.setOnClickListener {
            loadWebView("https://www.quora.com/")
            mSocialNetworkPopup.dismiss()
        }

        btnSoundcloud.setOnClickListener {
            loadWebView("https://music.youtube.com/")
            mSocialNetworkPopup.dismiss()
        }
    }


    /*
    // Education dialog
    private fun showEducation(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_education, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mEducationPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mEducationPopup.setBackgroundDrawable(colorDrawable)
        mEducationPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mEducationPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mEducationPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mEducationPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnCoursera = popUpView.findViewById<View>(R.id.img_coursera) as ImageView
        val btnAlison = popUpView.findViewById<View>(R.id.img_alison) as ImageView
        val btnEdx = popUpView.findViewById<View>(R.id.img_edx) as ImageView
        val btnKhan = popUpView.findViewById<View>(R.id.img_khan) as ImageView
        val btnUdacity = popUpView.findViewById<View>(R.id.img_udacity) as ImageView
        val btnUdemy = popUpView.findViewById<View>(R.id.img_udemy) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        //txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        //txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")
        btnConfirm.setOnClickListener { mEducationPopup.dismiss() }
        btnCoursera.setOnClickListener {
            loadWebView("https://www.coursera.org/browse")
            mEducationPopup.dismiss()
        }
        btnAlison.setOnClickListener {
            loadWebView("https://alison.com/")
            mEducationPopup.dismiss()
        }
        btnEdx.setOnClickListener {
            loadWebView("https://www.edx.org/search?tab=course")
            mEducationPopup.dismiss()
        }
        btnKhan.setOnClickListener {
            loadWebView("https://www.khanacademy.org/")
            mEducationPopup.dismiss()
        }
        btnUdacity.setOnClickListener {
            loadWebView("https://www.udacity.com/courses/all")
            mEducationPopup.dismiss()
        }
        btnUdemy.setOnClickListener {
            loadWebView("https://www.udemy.com/")
            mEducationPopup.dismiss()
        }


    }



    // Education dialog
    private fun showResult(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_result, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mResultPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mResultPopup.setBackgroundDrawable(colorDrawable)
        mResultPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mResultPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mResultPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mResultPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnNigeria = popUpView.findViewById<View>(R.id.img_nigeria) as ImageView
        val btnGhana = popUpView.findViewById<View>(R.id.img_ghana) as ImageView
        val btnGambia = popUpView.findViewById<View>(R.id.img_gambia) as ImageView
        val btnLiberia = popUpView.findViewById<View>(R.id.img_liberia) as ImageView
        val btnSierraLeone = popUpView.findViewById<View>(R.id.img_sierraleone) as ImageView

        val btnNeco = popUpView.findViewById<View>(R.id.img_neco) as ImageView
        val btnNabteb = popUpView.findViewById<View>(R.id.img_nabteb) as ImageView
        val btnJamb = popUpView.findViewById<View>(R.id.img_jamb) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        //txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        //txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")
        btnConfirm.setOnClickListener { mResultPopup.dismiss() }
        btnNigeria.setOnClickListener {
            loadWebView("https://www.waecdirect.org/")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnGhana.setOnClickListener {
            loadWebView("https://ghana.waecdirect.org/")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnGambia.setOnClickListener {
            loadWebView("https://app.waecgambia.org/resultchecker/resultchecker.aspx")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnLiberia.setOnClickListener {
            loadWebView("https://results.liberiawaec.org/")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnSierraLeone.setOnClickListener {
            loadWebView("https://www.waecsierra-leone.org/ResultChecker/")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnNeco.setOnClickListener {
            loadWebView("https://result.neco.gov.ng/")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnNabteb.setOnClickListener {
            loadWebView("https://eworld.nabteb.gov.ng/")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
        btnJamb.setOnClickListener {
            loadWebView("https://www.jamb.org.ng/examslipprinting2/CheckUTMEResults")
            mResultPopup.dismiss()
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Wishing you all the best ♥", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }


    }

*/



    // About us dialog
    private fun showAboutUs(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_about_us, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mAboutUsPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mAboutUsPopup.setBackgroundDrawable(colorDrawable)
        mAboutUsPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mAboutUsPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
        val btnEmail = popUpView.findViewById<View>(R.id.img_email) as ImageView
        val btnWebsite = popUpView.findViewById<View>(R.id.img_website) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        txtTitle.text = getString(R.string.title_user_version)
        txtDetail.text = getString(R.string.title_about_us)
        btnConfirm.setOnClickListener { mAboutUsPopup.dismiss() }
        btnCall.setOnClickListener {

        }

        btnWebsite.setOnClickListener {  }

        btnEmail.setOnClickListener {  UtilMethods.sandMailTo(mContext, "Contact with email!",
                getString(R.string.title_user_email),
                "Contact with via "+ R.string.app_name +" app", "") }
    }



    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            //setActiveFullScreen()
        }
    }

    private fun setActiveFullScreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }




    private fun storeOneSignalUserId(){
        val userId = OneSignal.getPermissionSubscriptionState().subscriptionStatus.userId
        if (userId != null) {
            UtilMethods.printLog(TAG, "userId = $userId")
            PreferenceUtils.editStringValue(Constants.KEY_ONE_SIGNAL_USER_ID, userId)
        }
    }

    fun notificationClickSync(){
        if (AppDataInstance.notificationUrl != "" && isApplicationAlive){
            loadBaseWebView()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web_view.saveState(outState)
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            readBundle(intent.extras)
        }else{
            UtilMethods.printLog(TAG, "New intent Extras is empty!")
        }
    }

    private fun readBundle(extras: Bundle?) {
        if(extras != null){
            AppDataInstance.notificationUrl = extras.getString(Constants.KEY_NOTIFICATION_URL).orEmpty()
            AppDataInstance.notificationUrlOpenType = extras.getString(Constants.KEY_NOTIFICATION_OPEN_TYPE).orEmpty()

            notificationClickSync()

            UtilMethods.printLog(TAG, "URL: "+AppDataInstance.notificationUrl)
            UtilMethods.printLog(TAG,  "Type: "+AppDataInstance.notificationUrlOpenType)

        }else{
            UtilMethods.printLog(TAG, "New intent Bundle is empty!!")
        }
    }

    override fun onStart() {
        super.onStart()
        isApplicationAlive = true
    }

    @SuppressLint("SwitchIntDef")
    override fun onResume() {
        super.onResume()
        isApplicationAlive = true
        initView()
        ThemeConfig(mContext, this).initThemeColor()
        ThemeConfig(mContext, this).initThemeStyle()
        web_view.onResume()
        try {
            mRewardedVideoAd.pause(this)
        }catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        web_view.onPause()
        isApplicationAlive = false
        if(::mRewardedVideoAd.isInitialized &&  mRewardedVideoAd.isLoaded){
            if(isApplicationAlive)
                mRewardedVideoAd.show()
        }
        try {
            if(::mRewardedVideoAd.isInitialized &&  mRewardedVideoAd.isLoaded){
                if(isApplicationAlive)
                    mRewardedVideoAd.pause(this)
            }
        }catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }
    }


    override fun onRestart() {
        super.onRestart()
        isApplicationAlive = true
        notificationClickSync()
        if(mLastUrl != ""){
            isViewLoaded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isApplicationAlive = false
        try {
            if(::mRewardedVideoAd.isInitialized &&  mRewardedVideoAd.isLoaded){
                if(isApplicationAlive)
                    mRewardedVideoAd.pause(this)
            }
        }catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        TODO("Not yet implemented")
    }
}
