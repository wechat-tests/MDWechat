package com.blanke.mdwechat.hookers

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blanke.mdwechat.CC
import com.blanke.mdwechat.Version
import com.blanke.mdwechat.WeChatHelper
import com.blanke.mdwechat.WeChatHelper.defaultImageRippleDrawable
import com.blanke.mdwechat.WeChatHelper.drawableTransparent
import com.blanke.mdwechat.WechatGlobal
import com.blanke.mdwechat.config.AppCustomConfig
import com.blanke.mdwechat.config.HookConfig
import com.blanke.mdwechat.hookers.base.Hooker
import com.blanke.mdwechat.hookers.base.HookerProvider
import com.blanke.mdwechat.hookers.main.BackgroundImageHook
import com.blanke.mdwechat.util.LogUtil
import com.blanke.mdwechat.util.NightModeUtils
import com.blanke.mdwechat.util.ViewTreeUtils
import com.blanke.mdwechat.util.ViewUtils
import com.blanke.mdwechat.util.ViewUtils.findLastChildView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import com.blanke.mdwechat.ViewTreeRepoThisVersion as VTTV

object ListViewHooker : HookerProvider {
    private var wechatId: CharSequence = ""
    private val excludeContext = arrayOf("com.tencent.mm.plugin.mall.ui.MallIndexUI")

    private val titleTextColor: Int
        get() {
            return NightModeUtils.getTitleTextColor()
        }
    private val summaryTextColor: Int
        get() {
            return NightModeUtils.getContentTextColor()
        }

    private val isHookTextColor: Boolean
        get() {
            return HookConfig.is_hook_main_textcolor || NightModeUtils.isNightMode()
        }

    override fun provideStaticHookers(): List<Hooker>? {
        return listOf(listViewHook)
    }

    private val listViewHook = Hooker {
        XposedHelpers.findAndHookMethod(AbsListView::class.java, "setSelector", Drawable::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                param?.args!![0] = drawableTransparent
            }
        })
        XposedHelpers.findAndHookMethod(AbsListView::class.java, "obtainView", CC.Int, BooleanArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                try {
                    val view = param?.result as View
                    val context = view.context
                    val tmp = excludeContext.find { context::class.java.name.contains(it) }
                    if (tmp != null) {
                        return
                    }

                    if ((!com.blanke.mdwechat.Common.isVXPEnv) && (HookConfig.is_hook_debug || HookConfig.is_hook_debug2)) {
                        LogUtil.log("----------抓取view start----------")
                        LogUtil.log(WechatGlobal.wxVersion.toString())
                        LogUtil.log("context=" + view.context)
                        LogUtil.logViewStackTraces(view)
                        LogUtil.logParentView(view, 10)
                        LogUtil.log("--------------------")
                    }

                    // 按照使用频率重排序
                    //气泡
                    if (((!NightModeUtils.isNightMode()) || HookConfig.is_hook_bubble_in_night_mode) && HookConfig.is_hook_chat_settings) {
                        // 聊天消息 item
                        if (ViewTreeUtils.equals(VTTV.ChatRightMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightMessageItem")

                            //chat_label
                            if (HookConfig.is_hook_chat_label_color)
                                VTTV.ChatRightMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }

                            val chatMsgRightTextColor = HookConfig.get_hook_chat_text_color_right
                            val msgView = ViewUtils.getChildView1(view, VTTV.ChatRightMessageItem.treeStacks["msgView"]!!) as View
//                    log("msgView=$msgView")
                            XposedHelpers.callMethod(msgView, "setTextColor", chatMsgRightTextColor)
                            XposedHelpers.callMethod(msgView, "setLinkTextColor", chatMsgRightTextColor)
                            XposedHelpers.callMethod(msgView, "setHintTextColor", chatMsgRightTextColor)
//                    val mText = XposedHelpers.getObjectField(msgView, "mText")
//                    log("msg right text=$mText")
                            val bubble = WeChatHelper.getRightBubble(msgView.resources)
                            msgView.background = bubble
                            if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                msgView.setPadding(30, 25, 45, 25)
                            }
                        } else if (ViewTreeUtils.equals(VTTV.ChatLeftMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftMessageItem")

                            if (HookConfig.is_hook_chat_label_color) {
                                //chat_label
                                VTTV.ChatLeftMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatLeftMessageItem.treeStacks["nickNameView"]?.apply {
                                    val nickNameView = ViewUtils.getChildView1(view, this) as TextView
                                    nickNameView.setTextColor(HookConfig.chat_label_color)
                                }
                            }
                            val chatMsgLeftTextColor = HookConfig.get_hook_chat_text_color_left
                            val msgView = ViewUtils.getChildView1(view, VTTV.ChatLeftMessageItem.treeStacks.get("msgView")!!) as View
//                    LogUtil.logXp("=======start=========")
//                    LogUtil.logXp("msgView=$msgView")
//                    val mText = XposedHelpers.getObjectField(msgView, "mText")
//                    LogUtil.logXp("msg left text=$mText")
//                    LogUtil.logViewXp(view)
//                    LogUtil.logStackTraceXp()
//                    LogUtil.logViewStackTracesXp(ViewUtils.getParentViewSafe(view, 111))
//                    LogUtil.logXp("=======end=========")
                            XposedHelpers.callMethod(msgView, "setTextColor", chatMsgLeftTextColor)
                            XposedHelpers.callMethod(msgView, "setLinkTextColor", chatMsgLeftTextColor)
                            XposedHelpers.callMethod(msgView, "setHintTextColor", chatMsgLeftTextColor)
                            // 聊天气泡
                            val bubble = WeChatHelper.getLeftBubble(msgView.resources)
                            msgView.background = bubble
                            if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                msgView.setPadding(45, 25, 30, 25)
                            }
                        }

                        // 聊天消息 audio
                        else if (ViewTreeUtils.equals(VTTV.ChatRightAudioMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightAudioMessageItem")
                            val chatMsgTextColor = HookConfig.get_hook_chat_text_color_right

                            //chat_label
                            if (HookConfig.is_hook_chat_label_color)
                                VTTV.ChatRightAudioMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }
                            //audioLengthView
                            VTTV.ChatRightAudioMessageItem.treeStacks["audioLengthView"]?.apply {
                                val audioLengthView = ViewUtils.getChildView1(view, this) as TextView
                                audioLengthView.setTextColor(chatMsgTextColor)
                            }

                            val msgView = ViewUtils.getChildView1(view, VTTV.ChatRightAudioMessageItem.treeStacks.get("msgView")!!) as TextView
                            //播放语音时的view
                            val msgAnimView = ViewUtils.getChildView1(view, VTTV.ChatRightAudioMessageItem.treeStacks.get("msgAnimView")!!) as View
                            val bubble = WeChatHelper.getRightBubble(msgView.resources)
                            msgView.background = null
                            ViewUtils.getParentViewSafe(msgView, 1).background = bubble
                            msgAnimView.background = null
                            if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                msgView.setPadding(30, 25, 45, 25)
//                                msgAnimView.setPadding(30, 25, 45, 25)
                            }

//                            //喇叭图标
                            val speakerIcon = msgView.compoundDrawables[2]
                            speakerIcon.setColorFilter(chatMsgTextColor, PorterDuff.Mode.SRC_ATOP)
                            msgView.setCompoundDrawables(null, null, speakerIcon, null)
                        } else if (ViewTreeUtils.equals(VTTV.ChatLeftAudioMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftAudioMessageItem")
                            val chatMsgTextColor = HookConfig.get_hook_chat_text_color_left

                            if (HookConfig.is_hook_chat_label_color) {
                                //chat_label
                                VTTV.ChatLeftAudioMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatLeftAudioMessageItem.treeStacks["nickNameView"]?.apply {
                                    val nickNameView = ViewUtils.getChildView1(view, this) as TextView
                                    nickNameView.setTextColor(HookConfig.chat_label_color)
                                }
                            }
                            //audioLengthView
                            VTTV.ChatLeftAudioMessageItem.treeStacks["audioLengthView"]?.apply {
                                val audioLengthView = ViewUtils.getChildView1(view, this) as TextView
                                audioLengthView.setTextColor(chatMsgTextColor)
                            }

                            val msgView = ViewUtils.getChildView1(view, VTTV.ChatLeftAudioMessageItem.treeStacks.get("msgView")!!) as TextView
                            val msgAnimView = ViewUtils.getChildView1(view, VTTV.ChatLeftAudioMessageItem.treeStacks.get("msgAnimView")!!) as View
                            val bubble = WeChatHelper.getLeftBubble(msgView.resources)
                            msgView.background = null
                            ViewUtils.getParentViewSafe(msgView, 1).background = bubble
                            msgAnimView.background = null
                            if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                msgView.setPadding(45, 25, 30, 25)
//                                msgAnimView.setPadding(45, 25, 30, 25)
                            }
//                            //喇叭图标
                            val speakerIcon = msgView.compoundDrawables[0]
                            speakerIcon.setColorFilter(chatMsgTextColor, PorterDuff.Mode.SRC_ATOP)
                            msgView.setCompoundDrawables(speakerIcon, null, null, null)
                        }

                        // 通话消息
                        else if (ViewTreeUtils.equals(VTTV.ChatRightCallMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightCallMessageItem")
                            val chatMsgTextColor = HookConfig.get_hook_chat_text_color_right
                            //chat_label
                            if (HookConfig.is_hook_chat_label_color) {
                                VTTV.ChatRightCallMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }
                            }
                            VTTV.ChatRightCallMessageItem.treeStacks.get("bgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as View
                                val bubble = WeChatHelper.getRightBubble(msgView.resources)
                                msgView.background = bubble
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    msgView.setPadding(30, 25, 45, 25)
                                }
                            }
                            //icon
                            VTTV.ChatRightCallMessageItem.treeStacks.get("icon")?.apply {
                                val icon = ViewUtils.getChildView1(view, this) as LinearLayout
                                val speakerIcon = icon.background
                                speakerIcon.setColorFilter(chatMsgTextColor, PorterDuff.Mode.SRC_ATOP)
                                icon.background = speakerIcon
                            }
                            //msgView
                            VTTV.ChatRightCallMessageItem.treeStacks.get("msgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                msgView.setTextColor(chatMsgTextColor)
                            }
                        } else if (ViewTreeUtils.equals(VTTV.ChatLeftCallMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftCallMessageItem")
                            val chatMsgTextColor = HookConfig.get_hook_chat_text_color_left

                            //chat_label
                            if (HookConfig.is_hook_chat_label_color)
                                VTTV.ChatLeftCallMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }

                            //msgView
                            VTTV.ChatLeftCallMessageItem.treeStacks.get("msgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                msgView.setTextColor(chatMsgTextColor)
                            }
                            //icon
                            VTTV.ChatLeftCallMessageItem.treeStacks.get("icon")?.apply {
                                val icon = ViewUtils.getChildView1(view, this) as LinearLayout
                                val speakerIcon = icon.background
                                speakerIcon.setColorFilter(chatMsgTextColor, PorterDuff.Mode.SRC_ATOP)
                                icon.background = speakerIcon
                            }
                            VTTV.ChatLeftCallMessageItem.treeStacks.get("bgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as View
                                val bubble = WeChatHelper.getLeftBubble(msgView.resources)
                                msgView.background = bubble
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    msgView.setPadding(30, 25, 45, 25)
                                }
                            }
                        }

                        // 引用消息 item
                        else if (ViewTreeUtils.equals(VTTV.RefRightMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.RefRightMessageItem")

                            //chat_label
                            if (HookConfig.is_hook_chat_label_color)
                                VTTV.RefRightMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }

                            val chatMsgRightTextColor = HookConfig.get_hook_chat_text_color_right
                            val msgView = ViewUtils.getChildView1(view, VTTV.RefRightMessageItem.treeStacks.get("msgView")!!) as View
                            XposedHelpers.callMethod(msgView, "setTextColor", chatMsgRightTextColor)
                            XposedHelpers.callMethod(msgView, "setLinkTextColor", chatMsgRightTextColor)
                            XposedHelpers.callMethod(msgView, "setHintTextColor", chatMsgRightTextColor)
                            val bubble = WeChatHelper.getRightBubble(msgView.resources)
                            msgView.background = bubble
                            if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                msgView.setPadding(30, 25, 45, 25)
                            }
                        } else if (ViewTreeUtils.equals(VTTV.RefLeftMessageItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftMessageItem")

                            if (HookConfig.is_hook_chat_label_color) {
                                //chat_label
                                VTTV.RefLeftMessageItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(HookConfig.chat_label_color)
                                }
                                //nickNameView
                                VTTV.RefLeftMessageItem.treeStacks["nickNameView"]?.apply {
                                    val nickNameView = ViewUtils.getChildView1(view, this) as TextView
                                    nickNameView.setTextColor(HookConfig.chat_label_color)
                                }
                            }

                            val chatMsgLeftTextColor = HookConfig.get_hook_chat_text_color_left
                            val msgView = ViewUtils.getChildView1(view, VTTV.RefLeftMessageItem.treeStacks.get("msgView")!!) as View
                            XposedHelpers.callMethod(msgView, "setTextColor", chatMsgLeftTextColor)
                            XposedHelpers.callMethod(msgView, "setLinkTextColor", chatMsgLeftTextColor)
                            XposedHelpers.callMethod(msgView, "setHintTextColor", chatMsgLeftTextColor)
                            // 聊天气泡
                            val bubble = WeChatHelper.getLeftBubble(msgView.resources)
                            msgView.background = bubble
                            if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                msgView.setPadding(45, 25, 30, 25)
                            }
                        }

                        //提示信息
                        else if (ViewTreeUtils.equals(VTTV.ChatHinterItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatHinterItem")

                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color
                                //chat_label
                                VTTV.ChatHinterItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                                VTTV.ChatHinterItem.treeStacks.get("msgView")?.apply {
                                    val msgView = ViewUtils.getChildView1(view, this) as View
                                    XposedHelpers.callMethod(msgView, "setTextColor", chat_label_color)
                                }
                            }
                        }
                        //图片
                        else if (ViewTreeUtils.equals(VTTV.ChatLeftPictureItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftPictureItem")
                            if (HookConfig.is_hook_chat_label_color) {

                                val chat_label_color = HookConfig.chat_label_color

                                //timeView
                                VTTV.ChatLeftPictureItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatLeftPictureItem.treeStacks.get("nickNameView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                        } else if (ViewTreeUtils.equals(VTTV.ChatRightPictureItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightPictureItem")
                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color

                                //timeView
                                VTTV.ChatLeftPictureItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                        }
                        //名片
                        else if (ViewTreeUtils.equals(VTTV.ChatLeftContactCardItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftContactCardItem")
                            val chat_label_color = HookConfig.chat_label_color
                            val chat_text_color_left = HookConfig.get_hook_chat_text_color_left

                            if (HookConfig.is_hook_chat_label_color) {
                                //timeView
                                VTTV.ChatLeftContactCardItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatLeftContactCardItem.treeStacks.get("nickNameView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                            VTTV.ChatLeftContactCardItem.treeStacks.get("msgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color_left)
                            }
                            VTTV.ChatLeftContactCardItem.treeStacks.get("titleView")?.apply {
                                val titleView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color_left)
                            }
                            // 聊天气泡
                            VTTV.ChatLeftContactCardItem.treeStacks.get("bgView")?.apply {
                                val bgView = ViewUtils.getChildView1(view, this) as View
                                bgView.background = WeChatHelper.getLeftBubble(bgView.resources)
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    bgView.setPadding(10, 0, 20, 25)
                                }
                            }
                        } else if (ViewTreeUtils.equals(VTTV.ChatRightContactCardItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightContactCardItem")
                            val chat_text_color_right = HookConfig.get_hook_chat_text_color_right

                            //timeView
                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color
                                VTTV.ChatRightContactCardItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                            VTTV.ChatRightContactCardItem.treeStacks.get("msgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color_right)
                            }
                            VTTV.ChatRightContactCardItem.treeStacks.get("msgView1")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color_right)
                            }
                            VTTV.ChatRightContactCardItem.treeStacks.get("titleView")?.apply {
                                val titleView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color_right)
                            }
                            // 聊天气泡
                            VTTV.ChatRightContactCardItem.treeStacks.get("bgView")?.apply {
                                val bgView = ViewUtils.getChildView1(view, this) as View
                                bgView.background = WeChatHelper.getRightBubble(bgView.resources)
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    bgView.setPadding(20, 20, 10, 25)
                                }
                            }
                        }
                        //位置
                        else if (ViewTreeUtils.equals(VTTV.ChatLeftPositionItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftPositionItem")
                            val chat_text_color_left = HookConfig.get_hook_chat_text_color_left

                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color
                                //timeView
                                VTTV.ChatLeftPositionItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatLeftPositionItem.treeStacks.get("nickNameView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                            VTTV.ChatLeftPositionItem.treeStacks.get("msgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color_left)
                            }
                            VTTV.ChatLeftPositionItem.treeStacks.get("msgView1")?.apply {
                                val msgView1 = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView1, "setTextColor", chat_text_color_left)
                            }
//                            VTTV.ChatLeftPositionItem.treeStacks.get("titleView")?.apply {
//                                val titleView = ViewUtils.getChildView1(view, this) as TextView
//                                XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color_left)
//                            }
                            // 聊天气泡
                            VTTV.ChatLeftPositionItem.treeStacks.get("bgView")?.apply {
                                val bgView = ViewUtils.getChildView1(view, this) as View
                                bgView.background = WeChatHelper.getLeftBubble(bgView.resources)
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    bgView.setPadding(20, 25, 20, 45)
                                }
                            }
                        } else if (ViewTreeUtils.equals(VTTV.ChatRightPositionItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightPositionItem")
                            val chat_text_color = HookConfig.get_hook_chat_text_color_right

                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color
                                //timeView
                                VTTV.ChatRightPositionItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                            VTTV.ChatRightPositionItem.treeStacks.get("msgView")?.apply {
                                val msgView = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color)
                            }
                            VTTV.ChatRightPositionItem.treeStacks.get("msgView1")?.apply {
                                val msgView1 = ViewUtils.getChildView1(view, this) as TextView
                                XposedHelpers.callMethod(msgView1, "setTextColor", chat_text_color)
                            }
//                            VTTV.ChatRightPositionItem.treeStacks.get("titleView")?.apply {
//                                val titleView = ViewUtils.getChildView1(view, this) as TextView
//                                XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color_left)
//                            }
                            // 聊天气泡
                            VTTV.ChatRightPositionItem.treeStacks.get("bgView")?.apply {
                                val bgView = ViewUtils.getChildView1(view, this) as View
                                bgView.background = WeChatHelper.getRightBubble(bgView.resources)
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    bgView.setPadding(20, 25, 20, 45)
                                }
                            }
                        }
                        //分享 / 小程序
                        else if (ViewTreeUtils.equals(VTTV.ChatLeftSharingItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatLeftSharingItem")
                            val chat_text_color_left = HookConfig.get_hook_chat_text_color_left

                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color
                                //timeView
                                VTTV.ChatLeftSharingItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatLeftSharingItem.treeStacks.get("nickNameView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                            // 聊天气泡 miniProgramBgView
                            VTTV.ChatLeftSharingItem.treeStacks.get("miniProgramBgView")?.apply {
                                val miniProgramBgView = ViewUtils.getChildView1(view, this) as View
                                miniProgramBgView.background = WeChatHelper.getLeftBubble(miniProgramBgView.resources)
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    miniProgramBgView.setPadding(20, 25, 20, 25)
                                }

                                VTTV.ChatLeftSharingItem.treeStacks.get("miniProgramBgView_miniProgramNameView")?.apply {
                                    val miniProgramNameView = ViewUtils.getChildView1(miniProgramBgView, this) as TextView
                                    XposedHelpers.callMethod(miniProgramNameView, "setTextColor", chat_text_color_left)
                                }
                                VTTV.ChatLeftSharingItem.treeStacks.get("miniProgramBgView_miniProgramTitleView")?.apply {
                                    val miniProgramTitleView = ViewUtils.getChildView1(miniProgramBgView, this) as TextView
                                    XposedHelpers.callMethod(miniProgramTitleView, "setTextColor", chat_text_color_left)
                                }

                                // 聊天气泡 null
                                VTTV.ChatLeftSharingItem.treeStacks.get("miniProgramBgView_bgView")?.apply {
                                    val bgView = ViewUtils.getChildView1(miniProgramBgView, this) as View
                                    bgView.background = null
                                    VTTV.ChatLeftSharingItem.treeStacks.get("bgView_titleView")?.apply {
                                        val titleView = ViewUtils.getChildView1(bgView, this) as TextView
                                        XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color_left)
                                    }
                                    VTTV.ChatLeftSharingItem.treeStacks.get("bgView_fileNameView")?.apply {
                                        val fileNameView = ViewUtils.getChildView1(bgView, this) as View
                                        XposedHelpers.callMethod(fileNameView, "setTextColor", chat_text_color_left)
                                    }
                                    VTTV.ChatLeftSharingItem.treeStacks.get("bgView_msgView")?.apply {
                                        val msgView = ViewUtils.getChildView1(bgView, this) as TextView
                                        XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color_left)
                                    }
                                    VTTV.ChatLeftSharingItem.treeStacks.get("bgView_msgView1")?.apply {
                                        val msgView1 = ViewUtils.getChildView1(bgView, this) as TextView
                                        XposedHelpers.callMethod(msgView1, "setTextColor", chat_text_color_left)
                                    }
                                }
                            }
                        } else if (ViewTreeUtils.equals(VTTV.ChatRightSharingItem.item, view)) {
                            LogUtil.logOnlyOnce("ListViewHooker.ChatRightSharingItem")
                            val chat_text_color = HookConfig.get_hook_chat_text_color_right

                            if (HookConfig.is_hook_chat_label_color) {
                                val chat_label_color = HookConfig.chat_label_color
                                //timeView
                                VTTV.ChatRightSharingItem.treeStacks.get("timeView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                                //nickNameView
                                VTTV.ChatRightSharingItem.treeStacks.get("nickNameView")?.apply {
                                    val timeView = ViewUtils.getChildView1(view, this) as TextView
                                    timeView.setTextColor(chat_label_color)
                                }
                            }
                            // 聊天气泡 miniProgramBgView
                            VTTV.ChatRightSharingItem.treeStacks.get("miniProgramBgView")?.apply {
                                val miniProgramBgView = ViewUtils.getChildView1(view, this) as View
                                miniProgramBgView.background = WeChatHelper.getRightBubble(miniProgramBgView.resources)
                                if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                    miniProgramBgView.setPadding(20, 25, 20, 25)
                                }

                                VTTV.ChatRightSharingItem.treeStacks.get("miniProgramBgView_miniProgramNameView")?.apply {
                                    val miniProgramNameView = ViewUtils.getChildView1(miniProgramBgView, this) as TextView
                                    XposedHelpers.callMethod(miniProgramNameView, "setTextColor", chat_text_color)
                                }
                                VTTV.ChatRightSharingItem.treeStacks.get("miniProgramBgView_miniProgramTitleView")?.apply {
                                    val miniProgramTitleView = ViewUtils.getChildView1(miniProgramBgView, this) as TextView
                                    XposedHelpers.callMethod(miniProgramTitleView, "setTextColor", chat_text_color)
                                }

                                // 聊天气泡 null
                                VTTV.ChatRightSharingItem.treeStacks.get("miniProgramBgView_bgView")?.apply {
                                    val bgView = ViewUtils.getChildView1(miniProgramBgView, this) as View
                                    bgView.background = null
                                    VTTV.ChatRightSharingItem.treeStacks.get("bgView_titleView")?.apply {
                                        val titleView = ViewUtils.getChildView1(bgView, this) as TextView
                                        XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color)
                                    }
                                    VTTV.ChatRightSharingItem.treeStacks.get("bgView_fileNameView")?.apply {
                                        val fileNameView = ViewUtils.getChildView1(bgView, this) as View
                                        XposedHelpers.callMethod(fileNameView, "setTextColor", chat_text_color)
                                    }
                                    VTTV.ChatRightSharingItem.treeStacks.get("bgView_msgView")?.apply {
                                        val msgView = ViewUtils.getChildView1(bgView, this) as TextView
                                        XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color)
                                    }
                                    VTTV.ChatRightSharingItem.treeStacks.get("bgView_msgView1")?.apply {
                                        val msgView1 = ViewUtils.getChildView1(bgView, this) as TextView
                                        XposedHelpers.callMethod(msgView1, "setTextColor", chat_text_color)
                                    }
                                }
                            }
                        }

                        //红包 放最后
                        else if (HookConfig.is_hook_red_packet) {
                            if (ViewTreeUtils.equals(VTTV.ChatLeftRedPacketItem.item, view)) {
                                LogUtil.logOnlyOnce("ListViewHooker.ChatLeftRedPacketItem")
                                val chat_text_color = HookConfig.get_hook_red_packet_text_color

                                if (HookConfig.is_hook_chat_label_color) {
                                    val chat_label_color = HookConfig.chat_label_color
                                    //timeView
                                    VTTV.ChatLeftRedPacketItem.treeStacks.get("timeView")?.apply {
                                        val timeView = ViewUtils.getChildView1(view, this) as TextView
                                        timeView.setTextColor(chat_label_color)
                                    }
                                    //nickNameView
                                    VTTV.ChatLeftRedPacketItem.treeStacks.get("nickNameView")?.apply {
                                        val timeView = ViewUtils.getChildView1(view, this) as TextView
                                        timeView.setTextColor(chat_label_color)
                                    }
                                }
                                VTTV.ChatLeftRedPacketItem.treeStacks.get("msgView")?.apply {
                                    val msgView = ViewUtils.getChildView1(view, this) as TextView
                                    XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color)
                                }
                                VTTV.ChatLeftRedPacketItem.treeStacks.get("msgView1")?.apply {
                                    val msgView = ViewUtils.getChildView1(view, this) as TextView
                                    XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color)
                                }
                                VTTV.ChatLeftRedPacketItem.treeStacks.get("titleView")?.apply {
                                    val titleView = ViewUtils.getChildView1(view, this) as TextView
                                    XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color)
                                }
                                // 聊天气泡
                                VTTV.ChatLeftRedPacketItem.treeStacks.get("bgView")?.apply {
                                    val bgView = ViewUtils.getChildView1(view, this) as View
                                    bgView.background = WeChatHelper.getLeftRedPacketBubble(bgView.resources)
                                    if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                        bgView.setPadding(10, 0, 10, 25)
                                    }
                                }
                                VTTV.ChatLeftRedPacketItem.treeStacks.get("adsView")?.apply {
                                    val adsView = ViewUtils.getChildView1(view, this) as FrameLayout
                                    adsView.visibility = View.GONE
                                }
                                // 左侧图标
                                VTTV.ChatLeftRedPacketItem.treeStacks.get("leftPicView")?.apply {
                                    val leftPicView = ViewUtils.getChildView1(view, this) as ImageView
                                    leftPicView.setImageDrawable(null)
                                }
                            } else if (ViewTreeUtils.equals(VTTV.ChatRightRedPacketItem.item, view)) {
                                LogUtil.logOnlyOnce("ListViewHooker.ChatRightRedPacketItem")
                                val chat_text_color = HookConfig.get_hook_red_packet_text_color

                                if (HookConfig.is_hook_chat_label_color) {
                                    val chat_label_color = HookConfig.chat_label_color
                                    //timeView
                                    VTTV.ChatRightRedPacketItem.treeStacks.get("timeView")?.apply {
                                        val timeView = ViewUtils.getChildView1(view, this) as TextView
                                        timeView.setTextColor(chat_label_color)
                                    }
                                }
                                VTTV.ChatRightRedPacketItem.treeStacks.get("msgView")?.apply {
                                    val msgView = ViewUtils.getChildView1(view, this) as TextView
                                    XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color)
                                }
                                VTTV.ChatRightRedPacketItem.treeStacks.get("msgView1")?.apply {
                                    val msgView = ViewUtils.getChildView1(view, this) as TextView
                                    XposedHelpers.callMethod(msgView, "setTextColor", chat_text_color)
                                }
                                VTTV.ChatRightRedPacketItem.treeStacks.get("titleView")?.apply {
                                    val titleView = ViewUtils.getChildView1(view, this) as TextView
                                    XposedHelpers.callMethod(titleView, "setTextColor", chat_text_color)
                                }
                                // 聊天气泡
                                VTTV.ChatRightRedPacketItem.treeStacks.get("bgView")?.apply {
                                    val bgView = ViewUtils.getChildView1(view, this) as View
                                    bgView.background = WeChatHelper.getRightRedPacketBubble(bgView.resources)
                                    if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                                        bgView.setPadding(10, 0, 10, 25)
                                    }
                                }
                                VTTV.ChatRightRedPacketItem.treeStacks.get("adsView")?.apply {
                                    val adsView = ViewUtils.getChildView1(view, this) as FrameLayout
                                    adsView.visibility = View.GONE
                                }
                                // 左侧图标
                                VTTV.ChatRightRedPacketItem.treeStacks.get("leftPicView")?.apply {
                                    val leftPicView = ViewUtils.getChildView1(view, this) as ImageView
                                    leftPicView.setImageDrawable(null)
                                }
                            }
                        }
                    }

                    // ConversationFragment 聊天列表 item
                    if (ViewTreeUtils.equals(VTTV.ConversationListViewItem.item, view)) {
                        LogUtil.logOnlyOnce("ListViewHooker.ConversationListViewItem")
                        try {
                            view.background.alpha = HookConfig.get_hook_conversation_background_alpha
                        } catch (e: Exception) {
                        }
                        val chatNameView = ViewUtils.getChildView1(view, VTTV.ConversationListViewItem.treeStacks["chatNameView"])
                        val chatTimeView = ViewUtils.getChildView1(view, VTTV.ConversationListViewItem.treeStacks["chatTimeView"])
                        val recentMsgView = ViewUtils.getChildView1(view, VTTV.ConversationListViewItem.treeStacks["recentMsgView"])
                        val unreadCountView = ViewUtils.getChildView1(view, VTTV.ConversationListViewItem.treeStacks["unreadCountView"]) as TextView
                        val unreadView = ViewUtils.getChildView1(view, VTTV.ConversationListViewItem.treeStacks["unreadView"]) as ImageView
//                    LogUtil.logXp("chatNameView=$chatNameView,chatTimeView=$chatTimeView,recentMsgView=$recentMsgView")
                        if (isHookTextColor) {
                            XposedHelpers.callMethod(chatNameView, "setTextColor", titleTextColor)
                            XposedHelpers.callMethod(chatTimeView, "setTextColor", summaryTextColor)
                            XposedHelpers.callMethod(recentMsgView, "setTextColor", summaryTextColor)
                        }
                        unreadCountView.backgroundTintList = ColorStateList.valueOf(NightModeUtils.colorTip)
                        unreadCountView.setTextColor(HookConfig.get_color_tip_num)
                        unreadView.backgroundTintList = ColorStateList.valueOf(NightModeUtils.colorTip)
                        val contentView = ViewUtils.getChildView(view, 1) as ViewGroup
                        contentView.background = defaultImageRippleDrawable
                        return
                    }

                    view.background = drawableTransparent
                    // 联系人列表
                    if (ViewTreeUtils.equals(VTTV.ContactListViewItem.item, view)) {
                        LogUtil.logOnlyOnce("ListViewHooker.ContactListViewItem")
                        // 标题下面的线
                        if (VTTV.ContactListViewItem.treeStacks["headerView"] != null) {
                            ViewUtils.getChildView1(view, VTTV.ContactListViewItem.treeStacks["headerView"])
                                    ?.background = drawableTransparent
                        }
                        //内容下面的线 innerView
                        ViewUtils.getChildView1(view, VTTV.ContactListViewItem.treeStacks["innerView"])
                                ?.background = drawableTransparent

                        ViewUtils.getChildView1(view, VTTV.ContactListViewItem.treeStacks["contentView"])
                                ?.background = drawableTransparent

                        val titleView = ViewUtils.getChildView1(view, VTTV.ContactListViewItem.treeStacks["titleView"])
                        titleView?.background = drawableTransparent
                        val titleView_8_0 = ViewUtils.getChildView1(view, VTTV.ContactListViewItem.treeStacks["titleView_8_0"])
                        titleView_8_0?.background = drawableTransparent
                        if (isHookTextColor) {
                            val headTextView = ViewUtils.getChildView1(view, VTTV.ContactListViewItem.treeStacks["headTextView"]) as TextView
                            headTextView.setTextColor(summaryTextColor)
                            titleView?.apply { XposedHelpers.callMethod(this, "setNickNameTextColor", ColorStateList.valueOf(titleTextColor)) }
                            titleView_8_0?.apply {
                                XposedHelpers.callMethod(titleView_8_0, "setTextColor", titleTextColor)
                            }
                        }
                    }

                    // 联系人列表头部
                    if (ViewTreeUtils.equals(VTTV.ContactHeaderItem.item, view)) {
                        LogUtil.logOnlyOnce("ListViewHooker.ContactHeaderItem")

                        //公众号的下边界 —— ContactHeaderItem.ContactWorkItemBorderTop
                        ViewUtils.getChildView1(view, VTTV.ContactHeaderItem.treeStacks["ContactWorkItemBorderTop"])?.apply {
                            this.background = drawableTransparent
                        }

                        //企业联系人分组
                        ViewUtils.getChildView1(view, VTTV.ContactHeaderItem.treeStacks["ContactWorkItem"])?.apply {
                            if (ViewTreeUtils.equals(VTTV.ContactWorkItem.item, this)) {
                                LogUtil.logOnlyOnce("ListViewHooker.ContactWorkItem")

                                //region borderLineTop
                                ViewUtils.getChildView1(this, VTTV.ContactWorkItem.treeStacks["borderLineTop"])?.apply {
                                    this.background = drawableTransparent
                                }
                                //endregion

                                var ContactContentsItem = ViewUtils.getChildView1(this, VTTV.ContactWorkItem.treeStacks["ContactContentsItem"])
                                if (ContactContentsItem != null) {
                                    //region 企业联系人
                                    if (ViewTreeUtils.equals(VTTV.ContactWorkContactsItem.item, ContactContentsItem)) {
                                        LogUtil.logOnlyOnce("ListViewHooker.ContactWorkContactsItem")
                                        if (isHookTextColor) {
                                            val headTextView = ViewUtils.getChildView1(ContactContentsItem, VTTV.ContactWorkContactsItem.treeStacks["headTextView"]) as TextView
                                            headTextView.setTextColor(titleTextColor)
                                        }
                                        //  titleView
                                        ViewUtils.getChildView1(ContactContentsItem, VTTV.ContactWorkContactsItem.treeStacks["titleView"])
                                                ?.background = defaultImageRippleDrawable
                                        ViewUtils.getChildView1(ContactContentsItem, VTTV.ContactWorkContactsItem.treeStacks["borderLineBottom"])
                                                ?.background = defaultImageRippleDrawable
                                        //endregion


                                        val tmpView = ViewUtils.getChildView1(this, VTTV.ContactWorkItem.treeStacks["ContactContentsItem1"])
                                        tmpView?.apply {
                                            ContactContentsItem = tmpView
                                        }
                                    }
                                    // 我的企业
                                    if (ViewTreeUtils.equals(VTTV.ContactMyWorkItem.item, ContactContentsItem!!)) {
                                        LogUtil.logOnlyOnce("ListViewHooker.ContactMyWorkItem")
                                        //  titleView
                                        ViewUtils.getChildView1(ContactContentsItem!!, VTTV.ContactMyWorkItem.treeStacks["titleView"])
                                                ?.background = defaultImageRippleDrawable
                                        ViewUtils.getChildView1(ContactContentsItem!!, VTTV.ContactMyWorkItem.treeStacks["borderLineBottom"])
                                                ?.background = drawableTransparent
                                        if (isHookTextColor) {
                                            val headTextView = ViewUtils.getChildView1(ContactContentsItem!!, VTTV.ContactMyWorkItem.treeStacks["headTextView"]) as TextView
                                            headTextView.setTextColor(titleTextColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 发现 设置 item
                    else if (ViewTreeUtils.equals(VTTV.DiscoverViewItem.item, view)) {
//                        LogUtil.logViewStackTraces(view)
                        LogUtil.logOnlyOnce("ListViewHooker.DiscoverViewItem")
                        val iconImageView = ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["iconImageView"]) as View
                        if (iconImageView.visibility == View.VISIBLE) {
                            val titleView = ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["titleView"]) as TextView
                            if (isHookTextColor) {
                                titleView.setTextColor(titleTextColor)
                            }
                        }
//                        LogUtil.logViewStackTraces(view)
                        //group顶部横线
                        ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["groupBorderTop"])
                                ?.background = drawableTransparent
                        //内容分割线
                        ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["contentBorder"])
                                ?.background = drawableTransparent

                        //group底部横线
                        ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["groupBorderBottom"])
                                ?.background = drawableTransparent

                        ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["borderRight"])
                                ?.background = drawableTransparent



                        ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["unreadPointView"])
                                ?.backgroundTintList = ColorStateList.valueOf(NightModeUtils.colorTip)
                        ViewUtils.getChildView1(view, VTTV.DiscoverViewItem.treeStacks["unreadCountView"])
                                ?.apply {
                                    this.backgroundTintList = ColorStateList.valueOf(NightModeUtils.colorTip)
                                    if (this is TextView) this.setTextColor(HookConfig.get_color_tip_num)
                                }
                    }

                    // 设置 头像
                    else if (ViewTreeUtils.equals(VTTV.SettingAvatarView.item, view)) {
                        LogUtil.logOnlyOnce("ListViewHooker.SettingAvatarView")

//                        微信号
                        ViewUtils.getChildView1(view, VTTV.SettingAvatarView.treeStacks["wechatTextView"])?.apply {
                            this as TextView
                            if (this.text.contains(": ") || this.text.contains("：")) {

                                //隐藏微信号
                                if (HookConfig.is_hide_wechatId) {
                                    if (wechatId.length == 0) wechatId = this.text
                                    this.text = "点击显示微信号"
                                    try {
                                        this.setOnClickListener {
                                            this.text = wechatId
                                            LogUtil.log("已显示微信号")
                                        }
                                    } catch (e: Exception) {
                                        LogUtil.log("显示微信号错误")
                                        LogUtil.log(e)
                                    }
                                }

                                //微信号颜色
                                if (isHookTextColor) {
                                    this.setTextColor(titleTextColor)
                                    ViewUtils.getChildView1(view, VTTV.SettingAvatarView.treeStacks["nickNameView"])?.apply {
                                        XposedHelpers.callMethod(this, "setTextColor", titleTextColor)
                                    }
                                }
                            }
                        }
                        if (WechatGlobal.wxVersion!! >= Version("8.0.0")) {
                            if (!HookConfig.is_settings_page_transparent) {
                                VTTV.SettingAvatarView.treeStacks["headView"]?.apply {
                                    ViewUtils.getChildView1(view, this)?.apply {
                                        //生成背景
                                        if (HookConfig.is_hook_bg_immersion) {
                                            if (BackgroundImageHook._backgroundBitmap[4] != null) {
                                                this.background = NightModeUtils.getBackgroundDrawable(BackgroundImageHook._backgroundBitmap[4])
                                                return
                                            } else {
                                                //2s之后如果没生成背景就放弃
                                                BackgroundImageHook.setMainPageBitmap("设置页头像栏", this, AppCustomConfig.getTabBg(3), 4, 4)
                                            }
                                        } else {
                                            this.background = if (NightModeUtils.isWechatNightMode()) ColorDrawable(WeChatHelper.wechatDark) else ColorDrawable(WeChatHelper.wechatWhite)
                                        }
                                    }
                                }
                                VTTV.SettingAvatarView.treeStacks["q1"]?.apply {
                                    ViewUtils.getChildView1(view, this)?.apply {
                                        //生成背景
                                        if (HookConfig.is_hook_bg_immersion) {
                                            if (BackgroundImageHook._backgroundBitmap[5] != null) {
                                                this.background = NightModeUtils.getBackgroundDrawable(BackgroundImageHook._backgroundBitmap[5])

                                            } else {
                                                //2s之后如果没生成背景就放弃
                                                BackgroundImageHook.setMainPageBitmap("设置页头像栏 (状态) ", this, AppCustomConfig.getTabBg(3), 5, 4)
                                            }
                                        } else {
                                            this.background = if (NightModeUtils.isWechatNightMode()) ColorDrawable(WeChatHelper.wechatDark) else ColorDrawable(WeChatHelper.wechatWhite)
                                        }
                                    }
                                }
                            }
                        } else {
                            VTTV.SettingAvatarView.treeStacks["headView"]?.apply {
                                ViewUtils.getChildView1(view, this)?.background = drawableTransparent
                            }
                        }
                    }

                    // (7.0.7 以上) 下拉小程序框
                    else if (HookConfig.is_hook_tab_bg && ViewTreeUtils.equals(VTTV.ActionBarItem.item, view)) {
                        LogUtil.logOnlyOnce("ListViewHooker.ActionBarItem")
                        try {
                            ViewUtils.getChildView1(view, VTTV.ActionBarItem.treeStacks["miniProgramPage"])?.apply {
                                val miniProgramPage = this as RelativeLayout

                                // old action bar
                                ViewUtils.getChildView1(miniProgramPage, VTTV.ActionBarItem.treeStacks["miniProgramPage_actionBarPage"])?.apply {
                                    val actionBarPage = this as LinearLayout
//                            val title: TextView
//                            title = ViewUtils.getChildView1(actionBarPage,
//                                    VTTV.ActionBarItem.treeStacks.get("actionBarPage_title")!!) as TextView
//
//                            title.gravity = Gravity.CENTER;
//                            title.text = HookConfig.value_mini_program_title
//                            val lp = title.layoutParams as LinearLayout.LayoutParams
//                            lp.setMargins(0, 0, 0, 0)
                                    ViewUtils.getChildView1(actionBarPage, VTTV.ActionBarItem.treeStacks["actionBarPage_addIcon"])?.apply {
                                        actionBarPage.removeView(this)
                                    }
                                    ViewUtils.getChildView1(actionBarPage, VTTV.ActionBarItem.treeStacks["actionBarPage_searchIcon"])?.apply {
                                        actionBarPage.removeView(this)
                                    }
                                }
//                            actionBarPage.removeView(title)
                                ViewUtils.getChildView1(miniProgramPage, VTTV.ActionBarItem.treeStacks["miniProgramPage_appBrandDesktopView"])?.apply {
                                    val appBrandDesktopView = this as ViewGroup

                                    // 小程序搜索框
                                    ViewUtils.getChildView1(appBrandDesktopView, VTTV.ActionBarItem.treeStacks["appBrandDesktopView_searchEditText"])?.apply {
                                        val searchEditText = this as EditText
                                        searchEditText.setBackgroundColor(Color.parseColor("#30000000"))
                                    }
                                    //  小程序字体
                                    setMiniProgramTitleColor(appBrandDesktopView)
                                    ViewUtils.getChildView1(appBrandDesktopView, VTTV.ActionBarItem.treeStacks["appBrandDesktopView_miniProgramTitle"])?.apply {
                                        setMiniProgramTitleColor(this as ViewGroup)
                                    }
                                }
//                    logXp("---------------------miniProgramPage------------------")
//                    LogUtil.logViewStackTracesXp(miniProgramPage)
//                    logXp("---------------------appBrandDesktopView------------------")
//                    LogUtil.logViewStackTracesXp(appBrandDesktopView)
//                    logXp("---------------------getChildView------------------")
//                    LogUtil.logViewStackTracesXp(ViewUtils.getChildView(appBrandDesktopView, 2, 0, 0) as ViewGroup)
                            }
                        } catch (e: ClassCastException) {
//                            LogUtil.log(e)
//                            LogUtil.logViewStackTraces(view)
                            return
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.log(e)
                }
            }
        })
    }

    fun setMiniProgramTitleColor(fatherView: ViewGroup) {
        val childCount = fatherView.childCount
        for (i in 0 until childCount) {
            val view0 = fatherView.getChildAt(i)
            if (view0 is ViewGroup) {
                val textView = findLastChildView(view0, CC.TextView.name)
                if (textView is TextView) {
                    textView.setTextColor(titleTextColor)
                }
            }
        }

    }
}