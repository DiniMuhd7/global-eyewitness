package com.dinisoft.eyewitness.setting

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.style.*
import com.dinisoft.eyewitness.ui.home.HomeActivity
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_home.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class ThemeConfig(){
    lateinit var mContext: Context
    lateinit var mActivity: HomeActivity

    constructor(context: Context, activity:HomeActivity) : this() {
        this.mContext = context
        this.mActivity = activity
    }

    fun initThemeColor(){
        // make problem on loading
        /*
        mActivity.layout_toolbar.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        mActivity.navigation_view.getHeaderView(0).background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))
        */


        // radius for button
        val buttonGradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        buttonGradientDrawable.cornerRadius = 20f
        mActivity.btn_try_again.background = buttonGradientDrawable
        mActivity.btn_error_home.background = buttonGradientDrawable
        mActivity.btn_error_try_again.background = buttonGradientDrawable

        // set other view color
        mActivity.loadingIndicator.setColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        mActivity.img_no_internet.setColorFilter(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        mActivity.txt_no_internet_title.setTextColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor()))
        mActivity.txt_no_internet_detail.setTextColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        mActivity.btn_ad_show.setColorFilter(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))

        mActivity.swap_view.setColorSchemeColors(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor()))
    }

    fun initThemeStyle(){
        // loader style
        val loaderStyle = PreferenceUtils.getInstance().getStringValue(Constants.KEY_LOADER, Constants.LOADER_HIDE)
        if(loaderStyle == Constants.LOADER_ROTATING_PLANE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(RotatingPlane())
        }else if(loaderStyle == Constants.LOADER_DOUBLE_BOUNCE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(DoubleBounce())
        }else if(loaderStyle == Constants.LOADER_WAVE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(Wave())
        }else if(loaderStyle == Constants.LOADER_WANDERING_CUBE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(WanderingCubes())
        }else if(loaderStyle == Constants.LOADER_PULSE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(Pulse())
        }else if(loaderStyle == Constants.LOADER_CHASING_DOTS){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(ChasingDots())
        }else if(loaderStyle == Constants.LOADER_THREE_BOUNCE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(ThreeBounce())
        }else if(loaderStyle == Constants.LOADER_CIRCLE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(Circle())
        }else if(loaderStyle == Constants.LOADER_CUBE_GRID){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(CubeGrid())
        }else if(loaderStyle == Constants.LOADER_FADING_CIRCLE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(FadingCircle())
        }else if(loaderStyle == Constants.LOADER_FOLDING_CUBE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(FoldingCube())
        }else if(loaderStyle == Constants.LOADER_ROTATING_CIRCLE){
            mActivity.loader_background.visibility = View.VISIBLE
            mActivity.loadingIndicator.visibility = View.VISIBLE
            mActivity.loadingIndicator.setIndeterminateDrawable(RotatingCircle())
        }else if(loaderStyle == Constants.LOADER_HIDE){
            mActivity.loader_background.visibility = View.GONE
            mActivity.loadingIndicator.visibility = View.GONE
        }


        /* / navigation bar style
        val navigationBar = PreferenceUtils.getInstance().getStringValue(Constants.KEY_NAVIGATION_STYLE, Constants.NAVIGATION_CLASSIC)
        if(navigationBar == Constants.NAVIGATION_STANDER){
            mActivity.layout_toolbar.visibility = View.VISIBLE
            mActivity.txt_toolbar_title.gravity = Gravity.LEFT
        }else if(navigationBar == Constants.NAVIGATION_ORDINARY){
            mActivity.layout_toolbar.visibility = View.VISIBLE
            mActivity.txt_toolbar_title.gravity = Gravity.RIGHT
        }else if(navigationBar == Constants.NAVIGATION_CLASSIC){
            mActivity.layout_toolbar.visibility = View.VISIBLE
            mActivity.txt_toolbar_title.gravity = Gravity.CENTER
        }else if(navigationBar == Constants.NAVIGATION_HIDE){
            mActivity.layout_toolbar.visibility = View.GONE
        }

        // navigation left menu style
        val navigationLeftMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_LEFT_MENU_STYLE, Constants.LEFT_MENU_SLIDER)
        if(navigationLeftMenu == Constants.LEFT_MENU_HIDE){
            mActivity.img_left_menu.visibility = View.GONE
            mActivity.img_left_menu.setImageResource(android.R.color.transparent)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_SLIDER){
            mActivity.img_left_menu.visibility = View.VISIBLE
            mActivity.img_left_menu.setImageResource(R.drawable.ic_menu)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_RELOAD){
            mActivity.img_left_menu.visibility = View.VISIBLE
            mActivity.img_left_menu.setImageResource(R.drawable.ic_reload)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_SHARE){
            mActivity.img_left_menu.visibility = View.VISIBLE
            mActivity.img_left_menu.setImageResource(R.drawable.ic_share_toolbar)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_HOME){
            mActivity.img_left_menu.visibility = View.VISIBLE
            mActivity.img_left_menu.setImageResource(R.drawable.ic_home_toolbar)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_EXIT){
            mActivity.img_left_menu.visibility = View.VISIBLE
            mActivity.img_left_menu.setImageResource(R.drawable.ic_exit_toolbar)
        }

        // navigation right menu style
        val navigationRightMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_RIGHT_MENU_STYLE, Constants.RIGHT_MENU_SHARE)
        if(navigationRightMenu == Constants.RIGHT_MENU_HIDE){
            mActivity.img_right_menu.visibility = View.GONE
            mActivity.img_right_menu.setImageResource(android.R.color.transparent)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_SLIDER){
            mActivity.img_right_menu.visibility = View.VISIBLE
            mActivity.img_right_menu.setImageResource(R.drawable.ic_menu)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_RELOAD){
            mActivity.img_right_menu.visibility = View.VISIBLE
            mActivity.img_right_menu.setImageResource(R.drawable.ic_reload)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_SHARE){
            mActivity.img_right_menu.visibility = View.VISIBLE
            mActivity.img_right_menu.setImageResource(R.drawable.ic_share_toolbar)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_HOME){
            mActivity.img_right_menu.visibility = View.VISIBLE
            mActivity.img_right_menu.setImageResource(R.drawable.ic_home_toolbar)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_EXIT){
            mActivity.img_right_menu.visibility = View.VISIBLE
            mActivity.img_right_menu.setImageResource(R.drawable.ic_exit_toolbar)
        }

        if (mActivity.layout_toolbar.visibility == View.VISIBLE && (navigationLeftMenu == Constants.LEFT_MENU_SLIDER || navigationRightMenu == Constants.RIGHT_MENU_SLIDER)){
            // enable drawer
            mActivity.drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

            // set navigation slider
            mActivity.navigation_view.setNavigationItemSelectedListener(mActivity)
            val toggle = ActionBarDrawerToggle(mActivity, mActivity.drawer_layout, null, R.string.open_drawer, R.string.close_drawer)
            mActivity.drawer_layout.addDrawerListener(toggle)
            toggle.syncState()
        }else{
            // disable drawer
            mActivity.drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

         */
    }
}
