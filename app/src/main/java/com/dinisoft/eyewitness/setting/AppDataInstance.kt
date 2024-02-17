package com.dinisoft.eyewitness.setting
/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

import android.content.Context
import infix.imrankst1221.rocket.library.data.NavigationMenu

class AppDataInstance(context: Context) {
    companion object {
        private var INSTANCE: AppDataInstance? = null
        var notificationUrl = ""
        var notificationUrlOpenType = ""
        var deepLinkUrl = ""
        var navigationMenus: ArrayList<NavigationMenu> = arrayListOf()

        fun getINSTANCE(mContext: Context): AppDataInstance {
            if (INSTANCE == null)
                INSTANCE = AppDataInstance(mContext.applicationContext)
            return INSTANCE as AppDataInstance
        }
    }
}