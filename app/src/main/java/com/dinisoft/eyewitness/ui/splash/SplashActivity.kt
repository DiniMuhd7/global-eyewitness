package com.dinisoft.eyewitness.ui.splash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import android.view.View
import com.dinisoft.eyewitness.AuthUIActivity
import com.dinisoft.eyewitness.DashUserActivity
import com.dinisoft.eyewitness.R
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_splash.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class SplashActivity : ThemeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mContext = this
        initView()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val misconduct = getString(R.string.channel_1)
            val accident = getString(R.string.channel_2)
            val theft = getString(R.string.channel_3)
            val murder = getString(R.string.channel_4)
            val assault = getString(R.string.channel_5)
            val firehazard = getString(R.string.channel_6)
            val flooding = getString(R.string.channel_7)
            val sexual_assault = getString(R.string.channel_8)
            val illegal_drugs = getString(R.string.channel_9)
            val terrorism = getString(R.string.channel_10)
            val redalarm = getString(R.string.channel_11)
            val eyelens = getString(R.string.channel_12)
            val chats = getString(R.string.channel_13)

            val descriptionText = getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val mChannelMisconduct = NotificationChannel(misconduct, misconduct, importance)
            mChannelMisconduct.description = descriptionText
            mChannelMisconduct.vibrationPattern
            mChannelMisconduct.lockscreenVisibility = 0
            val mChannelAccident = NotificationChannel(accident, accident, importance)
            mChannelAccident.description = descriptionText
            mChannelAccident.vibrationPattern
            mChannelAccident.lockscreenVisibility = 0
            val mChannelTheft = NotificationChannel(theft, theft, importance)
            mChannelTheft.description = descriptionText
            mChannelTheft.vibrationPattern
            mChannelTheft.lockscreenVisibility = 0
            val mChannelMurder = NotificationChannel(murder, murder, importance)
            mChannelMurder.description = descriptionText
            mChannelMurder.vibrationPattern
            mChannelMurder.lockscreenVisibility = 0
            val mChannelAssault = NotificationChannel(assault, assault, importance)
            mChannelAssault.description = descriptionText
            mChannelAssault.vibrationPattern
            mChannelAssault.lockscreenVisibility = 0
            val mChannelFirehazard = NotificationChannel(firehazard, firehazard, importance)
            mChannelFirehazard.description = descriptionText
            mChannelFirehazard.vibrationPattern
            mChannelFirehazard.lockscreenVisibility = 0
            val mChannelFlooding = NotificationChannel(flooding, flooding, importance)
            mChannelFlooding.description = descriptionText
            mChannelFlooding.vibrationPattern
            mChannelFlooding.lockscreenVisibility = 0
            val mChannelSexAssault = NotificationChannel(sexual_assault, sexual_assault, importance)
            mChannelSexAssault.description = descriptionText
            mChannelSexAssault.vibrationPattern
            mChannelSexAssault.lockscreenVisibility = 0
            val mChannelIllegalDrugs = NotificationChannel(illegal_drugs, illegal_drugs, importance)
            mChannelIllegalDrugs.description = descriptionText
            mChannelIllegalDrugs.vibrationPattern
            mChannelIllegalDrugs.lockscreenVisibility = 0
            val mChannelTerrorism = NotificationChannel(terrorism, terrorism, importance)
            mChannelTerrorism.description = descriptionText
            mChannelTerrorism.vibrationPattern
            mChannelTerrorism.lockscreenVisibility = 0

            val mChannelRedAlarm = NotificationChannel(redalarm, redalarm, importance)
            mChannelRedAlarm.description = descriptionText
            mChannelRedAlarm.vibrationPattern
            mChannelRedAlarm.lockscreenVisibility = 0
            val mChannelEyelens = NotificationChannel(eyelens, eyelens, importance)
            mChannelEyelens.description = descriptionText
            mChannelEyelens.vibrationPattern
            mChannelEyelens.lockscreenVisibility = 0
            val mChannelChats = NotificationChannel(chats, chats, importance)
            mChannelChats.description = descriptionText
            mChannelChats.vibrationPattern
            mChannelChats.lockscreenVisibility = 0

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannelMisconduct)
            notificationManager.createNotificationChannel(mChannelAccident)
            notificationManager.createNotificationChannel(mChannelTheft)
            notificationManager.createNotificationChannel(mChannelMurder)
            notificationManager.createNotificationChannel(mChannelAssault)
            notificationManager.createNotificationChannel(mChannelFirehazard)
            notificationManager.createNotificationChannel(mChannelFlooding)
            notificationManager.createNotificationChannel(mChannelSexAssault)
            notificationManager.createNotificationChannel(mChannelIllegalDrugs)
            notificationManager.createNotificationChannel(mChannelTerrorism)
            // Non review features
            notificationManager.createNotificationChannel(mChannelRedAlarm)
            notificationManager.createNotificationChannel(mChannelEyelens)
            notificationManager.createNotificationChannel(mChannelChats)
        }



        gotoNextView() // Go to Auth login activity
    }

    private fun initView(){
        txe_splash_quote.text = getString(R.string.splash_quote)
        txt_splash_footer.text = getString(R.string.splash_footer)

        val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext,UtilMethods.getThemePrimaryDarkColor())))
        gradientDrawable.cornerRadius = 0f
        view_background.background = gradientDrawable

        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_FULL_SCREEN_ACTIVE, false)){
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

    }

    private fun gotoNextView(){
        Handler().postDelayed({
            val intent = Intent(this, DashUserActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }, 3000)
    }

}
