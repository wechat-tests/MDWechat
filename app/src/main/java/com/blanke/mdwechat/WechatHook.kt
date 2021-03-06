package com.blanke.mdwechat

import com.blanke.mdwechat.Common.isVXPEnv
import com.blanke.mdwechat.config.HookConfig
import com.blanke.mdwechat.config.ViewTreeConfig
import com.blanke.mdwechat.config.WxVersionConfig
import com.blanke.mdwechat.hookers.*
import com.blanke.mdwechat.hookers.base.HookerProvider
import com.blanke.mdwechat.util.LogUtil
import com.blanke.mdwechat.util.LogUtil.log
import com.joshcai.mdwechat.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class WechatHook : IXposedHookLoadPackage {

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            if (!(lpparam.packageName.contains("com.tencent") && lpparam.packageName.contains("mm")))
                return
            // 暂时不 hook 小程序
            if (lpparam.processName.contains(":")) {
                return
            }
            WeChatHelper.initPrefs()
            if (!HookConfig.is_hook_switch) {
                log("模块总开关已关闭")
                return
            }
            log("模块加载中...")
            val hookers = mutableListOf(
                    StatusBarHooker,
                    ActionBarHooker,
                    LauncherUIHooker,
                    AvatarHooker,
                    ListViewHooker,
                    ConversationHooker,
                    ContactHooker,
                    DiscoverHooker,
                    SettingsHooker,
                    SchemeHooker,
                    LogHooker,
                    NightModeHooker
            )
//            region test
//            log("Hookers 总数: ${hookers.count()}")
//            val asd = HookConfig.debug_config_text.split(" ")
//            for (i in asd[3].toInt() downTo asd[2].toInt()) {
//                hookers.removeAt(i)
//            }
//            for (i in asd[1].toInt() downTo asd[0].toInt()) {
//                hookers.removeAt(i)
//            }
//            log("激活的 Hookers 数量: ${hookers.count()}，分别为：")
//            hookers.forEach {
//                log(it::class.java.name)
//            }
            LogUtil.logStackTraces()
//            //endregion

            if ((!isVXPEnv) && (HookConfig.is_hook_debug || HookConfig.is_hook_debug2)) {
                hookers.add(0, DebugHooker)
            }
            hookMain(lpparam, hookers)
            log("模块加载成功")
        } catch (e: Throwable) {
            log(e)
        }
    }

    private fun hookMain(lpparam: XC_LoadPackage.LoadPackageParam, plugins: List<HookerProvider>) {
        WechatGlobal.init(lpparam)
        try {
            WechatGlobal.wxVersionConfig = WxVersionConfig.loadConfig(WechatGlobal.wxVersion!!.toString())
            ViewTreeConfig.set(WechatGlobal.wxVersion!!)
        } catch (e: Exception) {
            log("${WechatGlobal.wxVersion} 配置文件不存在或解析失败")
            return
        }
        log("wechat version=" + WechatGlobal.wxVersion
                + ",processName=" + lpparam.processName
                + ",isVXPEnv = " + isVXPEnv
                + ",MDWechat version=" + BuildConfig.VERSION_NAME)
        plugins.forEach { provider ->
            provider.provideStaticHookers()?.forEach { hooker ->
                if (!hooker.hasHooked) {
                    hooker.hook()
                    hooker.hasHooked = true
                }
            }
        }
    }
}

