package com.blanke.mdwechat.hookers

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.blanke.mdwechat.*
import com.blanke.mdwechat.Classes.LauncherUIBottomTabView
import com.blanke.mdwechat.Classes.MainTabUIPageAdapter
import com.blanke.mdwechat.Classes.WxViewPager
import com.blanke.mdwechat.Fields.HomeUI_mMainTabUI
import com.blanke.mdwechat.Fields.LauncherUI_mHomeUI
import com.blanke.mdwechat.Fields.MainTabUI_mCustomViewPager
import com.blanke.mdwechat.Methods.MainTabUIPageAdapter_onPageScrolled
import com.blanke.mdwechat.Methods.WxViewPager_selectedPage
import com.blanke.mdwechat.Objects.Main.LauncherUI_mTabLayout
import com.blanke.mdwechat.config.AppCustomConfig
import com.blanke.mdwechat.config.HookConfig
import com.blanke.mdwechat.hookers.base.Hooker
import com.blanke.mdwechat.hookers.base.HookerProvider
import com.blanke.mdwechat.hookers.main.BackgroundImageHook
import com.blanke.mdwechat.hookers.main.FloatMenuHook
import com.blanke.mdwechat.hookers.main.HomeActionBarHook
import com.blanke.mdwechat.hookers.main.TabLayoutHook
import com.blanke.mdwechat.util.LogUtil.log
import com.blanke.mdwechat.util.ViewUtils
import com.blanke.mdwechat.util.ViewUtils.measureHeight
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers


object LauncherUIHooker : HookerProvider {
    const val keyInit = "key_init"
    private var disablePageScrolledHook = false

    override fun provideStaticHookers(): List<Hooker>? {
        return listOf(
                launcherLifeHooker,
                mainTabUIPageAdapterHook, actionMenuHooker)
    }

    private val launcherLifeHooker = Hooker {
//        XposedHelpers.findAndHookMethod(CC.Activity, "onDestroy", object : XC_MethodHook() {
//            override fun afterHookedMethod(param: MethodHookParam) {
//                val activity = param.thisObject as? Activity ?: return
//                if (activity::class.java != Classes.LauncherUI) {
//                    return
//                }
////                if (!LogUtil.logStackTraceXp("onRecreate")) {
////                    LogUtil.logXp("\n\n\n\nLauncherUI onDestroy()")
//                Objects.clear()
////                }
//            }
//        })
        XposedHelpers.findAndHookMethod(CC.Activity, "onPostResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        log("activity resume = $activity")
                        if (activity::class.java != Classes.LauncherUI) {
                            return
                        }
                        WeChatHelper.reloadPrefs()
                        val isInit = XposedHelpers.getAdditionalInstanceField(activity, keyInit)
                        if (isInit != null) {
                            log("LauncherUI 已经hook过")
                            return
                        }
                        log("LauncherUI onResume(), start hook")
                        initHookLauncherUI(activity)
                    }

                    private fun initHookLauncherUI(activity: Activity) {
                        try {
                            val density = activity.resources.displayMetrics.density
                            AppCustomConfig.bitmapScale = density / 3F

                            Objects.Main.LauncherUI = activity
                            val homeUI = LauncherUI_mHomeUI.get(activity)
                            val mainTabUI = HomeUI_mMainTabUI.get(homeUI)
                            val viewPager = MainTabUI_mCustomViewPager.get(mainTabUI)
                            if (viewPager == null || viewPager !is View) {
                                log("MainTabUI_mCustomViewPager == null return;")
                                return
                            }

                            val linearViewGroup = viewPager.parent as ViewGroup
                            BackgroundImageHook.contactPageParent = linearViewGroup
                            val contentViewGroup = linearViewGroup.parent as ViewGroup
                            Objects.Main.LauncherUI_mContentLayout = contentViewGroup
                            Objects.Main.HomeUI_mActionBar = Fields.HomeUI_mActionBar.get(homeUI)
                            Objects.Main.LauncherUI_mViewPager = viewPager

                            // region 微信底栏 & action bar
                            val is_hook_tab = !HookConfig.is_key_hide_tab && HookConfig.is_hook_tab
                            val isTabLayoutOnBottom = is_hook_tab && !HookConfig.is_tab_layout_on_top
                            val isTabLayoutOnTop = is_hook_tab && HookConfig.is_tab_layout_on_top
                            val isKeyHideTab = isTabLayoutOnTop || (!is_hook_tab && HookConfig.is_key_hide_tab)
                            val shouldFix = isTabLayoutOnTop || HookConfig.is_hook_hide_actionbar
                            val floatButtonMarginBottom = if (isTabLayoutOnBottom || (!isKeyHideTab)) 1 else 0

                            val tabView = linearViewGroup.getChildAt(1) as ViewGroup
                            val tabViewUnderneathHeight = measureHeight(tabView)
                            if (BackgroundImageHook._tabLayoutHeightOnBottom < 0)
                                BackgroundImageHook._tabLayoutHeightOnBottom = tabViewUnderneathHeight

                            if (isKeyHideTab) {
                                // region 隐藏底栏
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    // 672报错
                                    val bottomLine = tabView.getChildAt(0)
                                    bottomLine.visibility = View.GONE
                                    bottomLine.layoutParams.height = 0
                                } else {
                                    linearViewGroup.removeView(tabView)
                                }
                                log("移除 tabView $tabView")
                                //endregion
                            }

                            when {
                                isTabLayoutOnTop -> {
                                    try {
                                        log("添加 TabLayout")
                                        TabLayoutHook.addTabLayout(linearViewGroup)
                                    } catch (e: Throwable) {
                                        log("添加 TabLayout 报错")
                                        log(e)
                                    }
                                }
                                isTabLayoutOnBottom -> {
                                    try {
                                        log("添加底栏")
                                        TabLayoutHook.addTabLayoutAtBottom(tabView, tabViewUnderneathHeight)
                                        log("添加底栏成功")
                                    } catch (e: Throwable) {
                                        log("添加底栏 报错")
                                        log(e)
                                    }
                                }
                                else -> {
                                    log("不用添加 TabLayout")
                                    BackgroundImageHook._tabLayoutLocation[1] = -1
                                }
                            }
                            if (shouldFix) {
                                // 隐藏 action bar 测试
                                HomeActionBarHook.fix(linearViewGroup)
                            }
                            log("fix completed")
                            //endregion

                            // float menu
                            if (HookConfig.is_hook_float_button) {
                                try {
                                    log("添加 FloatMenu")
                                    FloatMenuHook.addFloatMenu(contentViewGroup, floatButtonMarginBottom * tabViewUnderneathHeight)
                                } catch (e: Throwable) {
                                    log("添加 FloatMenu 报错")
                                    log(e)
                                }
                            }
                            XposedHelpers.setAdditionalInstanceField(activity, keyInit, true)
                            log("LaunchUI Hook Completed.")
                        } catch (e: Exception) {
                            log(e)
                        }
                    }
                })
    }

    private
    val mainTabUIPageAdapterHook = Hooker {
        XposedHelpers.findAndHookMethod(WxViewPager, WxViewPager_selectedPage.name, CC.Int, CC.Boolean, CC.Boolean, CC.Int, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val vp = param?.thisObject
                if (Objects.Main.LauncherUI_mViewPager == vp) {
                    val position = param?.args!![0] as Int
//                    log("WxViewPager_selectedPage position = $position , arg[1] =${param?.args!![1]}")
                    LauncherUI_mTabLayout?.currentTab = position
                    Objects.Main.pagePosition = position
                    BackgroundImageHook.setGuideBarBitmaps(position)
                }
            }
        })
        XposedHelpers.findAndHookMethod(MainTabUIPageAdapter, MainTabUIPageAdapter_onPageScrolled.name, CC.Int, Float::class.java, CC.Int, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val positionOffset = param?.args!![1] as Float
                val position = param.args[0]
//                log("MainTabUIPageAdapter_onPageScrolled ,positionOffset=$positionOffset,startScrollPosition=$position")
                if (disablePageScrolledHook || positionOffset.toString().contains("E")) {// ?
                    disablePageScrolledHook = true
                    return
                }
                LauncherUI_mTabLayout?.apply {
                    startScrollPosition = position as Int
                    indicatorOffset = positionOffset
                    Objects.Main.pagePosition = startScrollPosition
                    BackgroundImageHook.setGuideBarBitmaps(startScrollPosition)
                }
            }
        })

        XposedHelpers.findAndHookMethod(TextView::class.java, "setText", CharSequence::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val tv = param?.thisObject as View
                val text = param.args!![0]
                if (text == null || text !is String) {
                    return
                }
                val tabView = ViewUtils.getParentView(tv, 5)
                if (tabView != null && tabView.javaClass.name == LauncherUIBottomTabView.name) {
                    ViewUtils.getParentView(tv, 3)?.apply {
                        val tabViewItemParent = this.parent as ViewGroup
                        val position = tabViewItemParent.indexOfChild(this)
//                        log("unread position= $position,count = $text")
                        val number = if (text.length == 0) 0 else text.toIntOrNull()
                        LauncherUI_mTabLayout?.apply {
                            number?.apply {
                                showMsg(position, number)
                            }
                        }
                    }
                }
            }
        })
        XposedHelpers.findAndHookMethod(View::class.java, "setVisibility", CC.Int, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.thisObject !is ImageView) {
                    return
                }
                val visible = param.args!![0] as Int
                val view = param.thisObject as ImageView
                val tabView = ViewUtils.getParentView(view, 5)
                if (tabView != null && tabView.javaClass.name == LauncherUIBottomTabView.name) {
                    ViewUtils.getParentView(view, 3)?.apply {
                        val tabViewItemParent = this.parent as ViewGroup
                        val position = tabViewItemParent.indexOfChild(this)
//                        log("unread position= $position,visible = ${visible == View.VISIBLE}")
                        LauncherUI_mTabLayout?.apply {
                            if (visible == View.VISIBLE) {
                                showMsg(position, -1)
                            } else if (!hasMsg(position)) {
                                showMsg(position, 0)
                            }
                        }
                    }
                }
            }
        })
    }

    private
    val actionMenuHooker = Hooker {
        //hide menu item in actionBar
        XposedHelpers.findAndHookMethod(Classes.LauncherUI, "onCreateOptionsMenu", CC.Menu, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!HookConfig.is_hook_float_button) {
                    return
                }
                val menu = param.args[0] as Menu
                menu.removeItem(2)
            }
        })
        XposedHelpers.findAndHookMethod(Classes.ActionMenuView, "add",
                Int::class.java, Int::class.java, Int::class.java, CharSequence::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.args.size == 4) {
                            val str = param.args[3]
                            val menuItem = param.result as MenuItem
                            if (str == "微X模块") {
                                log("检测到 微X模块")
                                menuItem.isVisible = false
                                Objects.Main.LauncherUI_mWechatXMenuItem = menuItem
                            }
                        }
                    }
                })
    }
}