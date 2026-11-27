package com.gesture.explode.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale

enum class SearchItemType { APP, ICON_PACK, CONTACT, SETTINGS, SYSTEM_ACTION }

enum class DeviceBrand(val displayNameZh: String, val displayNameEn: String) {
    SAMSUNG("Samsung One UI", "Samsung One UI"),
    XIAOMI("小米 HyperOS / MIUI", "Xiaomi HyperOS / MIUI"),
    OPLUS("OPPO / 一加 / 真我 ColorOS", "OPPO / OnePlus / Realme"),
    VIVO("vivo / iQOO OriginOS", "vivo / iQOO OriginOS"),
    HUAWEI_HONOR("华为 / 荣耀", "Huawei / Honor"),
    PIXEL_AOSP("Android 原生", "Stock Android")
}

data class SearchItem(
    val title: String,
    val subtitle: String,
    val initials: String,
    val initialIndices: List<Int>,
    val launchData: String,
    val type: SearchItemType,
    val contactId: String? = null,
    val aliases: List<String> = emptyList(),
    val resolvedIntent: Intent? = null,
    val photoUri: String? = null,
    val phoneNumber: String? = null
)

fun detectDeviceBrand(): DeviceBrand {
    val fingerprint = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
    return when {
        fingerprint.contains("samsung") -> DeviceBrand.SAMSUNG
        fingerprint.contains("xiaomi") || fingerprint.contains("redmi") || fingerprint.contains("poco") -> DeviceBrand.XIAOMI
        fingerprint.contains("oppo") || fingerprint.contains("oneplus") || fingerprint.contains("realme") -> DeviceBrand.OPLUS
        fingerprint.contains("vivo") || fingerprint.contains("iqoo") -> DeviceBrand.VIVO
        fingerprint.contains("huawei") || fingerprint.contains("honor") -> DeviceBrand.HUAWEI_HONOR
        else -> DeviceBrand.PIXEL_AOSP
    }
}

/**
 * Extracts Chinese Pinyin initials + English word/CamelCase initials.
 * Examples:
 * - "WeChat" -> "wc"
 * - "YouTube" -> "yt"
 * - "GitHub" -> "gh"
 * - "电池" -> "dc"
 */
fun getSearchInitialsInfo(text: String): Pair<String, List<Int>> {
    val initials = StringBuilder()
    val indices = mutableListOf<Int>()
    var isNewWord = true
    var prevWasLower = false

    for (i in text.indices) {
        val c = text[i]
        val pinyinArray: Array<String>? = PinyinHelper.toHanyuPinyinStringArray(c)
        if (!pinyinArray.isNullOrEmpty()) {
            initials.append(pinyinArray[0][0].lowercaseChar())
            indices.add(i)
            isNewWord = false
            prevWasLower = false
        } else if (c.isLetterOrDigit()) {
            val isCamelCaseBoundary = c.isUpperCase() && prevWasLower
            if (isNewWord || isCamelCaseBoundary) {
                initials.append(c.lowercaseChar())
                indices.add(i)
                isNewWord = false
            }
            prevWasLower = c.isLowerCase()
        } else {
            isNewWord = true
            prevWasLower = false
        }
    }
    return Pair(initials.toString(), indices)
}

// Focused, high-precision popular app abbreviations (no overly broad category tags like "ai" or "vpn")
private val APP_ALIAS_RULES_BY_PKG: List<Pair<String, List<String>>> = listOf(
    // Messaging & Social
    "org.telegram" to listOf("tg", "telegram"),
    "tw.nekomimi.nekogram" to listOf("tg", "telegram"),
    "com.exteragram" to listOf("tg", "telegram"),
    "org.thunderdog.challegram" to listOf("tg", "tgx", "telegram"),
    "uz.unnarsx.cherrygram" to listOf("tg", "telegram"),
    "com.tencent.mm" to listOf("wc", "wx", "vx", "wechat", "weixin"),
    "com.whatsapp" to listOf("wa", "whatsapp"),
    "com.tencent.mobileqq" to listOf("qq"),
    "com.tencent.tim" to listOf("tim"),
    "com.discord" to listOf("dc", "discord"),
    "com.twitter.android" to listOf("x", "tw", "twitter"),
    "com.instagram.android" to listOf("ig", "ins"),
    "com.facebook.katana" to listOf("fb"),
    "com.facebook.orca" to listOf("fbm", "messenger"),
    "jp.naver.line.android" to listOf("line"),
    "org.thoughtcrime.securesms" to listOf("signal"),
    "com.xingin.xhs" to listOf("xhs"),
    "com.sina.weibo" to listOf("wb", "weibo"),
    "com.zhihu.android" to listOf("zh", "zhihu"),
    "com.douban.frodo" to listOf("douban"),
    "com.alibaba.android.rimet" to listOf("dd", "dingtalk"),
    "com.ss.android.lark" to listOf("fs", "lark", "feishu"),
    "com.tencent.wework" to listOf("qywx", "wework"),

    // Media & Entertainment
    "com.google.android.youtube" to listOf("yt", "ytb", "youtube"),
    "app.revanced.android.youtube" to listOf("yt", "ytb", "youtube"),
    "com.vanced.android.youtube" to listOf("yt", "ytb", "youtube"),
    "com.google.android.apps.youtube.music" to listOf("ytm"),
    "tv.danmaku.bili" to listOf("bl", "bili", "bilibili"),
    "com.bilibili" to listOf("bl", "bili", "bilibili"),
    "com.ss.android.ugc.aweme" to listOf("dy", "douyin"),
    "com.zhiliaoapp.musically" to listOf("tt", "tiktok"),
    "com.ss.android.ugc.trill" to listOf("tt", "tiktok"),
    "com.spotify.music" to listOf("sp", "spotify"),
    "com.netease.cloudmusic" to listOf("wyy", "ncm"),
    "com.tencent.qqmusic" to listOf("qqyy"),
    "com.netflix.mediaclient" to listOf("nf", "netflix"),
    "com.qiyi.video" to listOf("aqy", "iqiyi"),
    "com.tencent.qqlive" to listOf("txsp"),
    "com.youku.phone" to listOf("yk", "youku"),

    // Shopping, Finance & Travel
    "com.eg.android.AlipayGphone" to listOf("zfb", "alipay"),
    "com.taobao.taobao" to listOf("tb", "taobao"),
    "com.jingdong.app.mall" to listOf("jd", "jingdong"),
    "com.xunmeng.pinduoduo" to listOf("pdd"),
    "com.taobao.idlefish" to listOf("xy", "xianyu"),
    "com.sankuai.meituan" to listOf("mt", "meituan"),
    "me.ele" to listOf("elm"),
    "com.sdu.didi.psnger" to listOf("dd", "didi"),
    "com.autonavi.minimap" to listOf("gd", "gaode", "amap"),
    "com.baidu.BaiduMap" to listOf("bddt"),
    "com.google.android.apps.maps" to listOf("gmap"),
    "ctrip.android.view" to listOf("xc", "ctrip"),
    "com.MobileTicket" to listOf("12306"),

    // Productivity & Tools
    "com.openai.chatgpt" to listOf("gpt", "cgpt"),
    "com.deepseek.chat" to listOf("ds", "deepseek"),
    "com.github.android" to listOf("gh", "github"),
    "com.google.android.gm" to listOf("gmail"),
    "com.android.chrome" to listOf("chrome"),
    "com.microsoft.emmx" to listOf("edge"),
    "org.mozilla.firefox" to listOf("ff", "firefox"),
    "com.coolapk.market" to listOf("ka", "coolapk"),
    "com.baidu.netdisk" to listOf("bdwp"),
    "com.alicloud.databox" to listOf("alyp"),
    "com.quark.browser" to listOf("kk", "quark"),
    "cn.wps.moffice" to listOf("wps"),
    "com.v2ray.ang" to listOf("v2", "v2ray"),
    "com.github.kr328.clash" to listOf("clash"),
    "com.follow.clash" to listOf("fc", "clash"),
    "io.nekohasekai.sfa" to listOf("singbox"),
    "moe.nb4a" to listOf("neko"),

    // Samsung Companion Apps
    "com.samsung.android.goodlock" to listOf("gl", "goodlock"),
    "com.samsung.android.spay" to listOf("spay")
)

private val APP_ALIAS_RULES_BY_NAME: List<Pair<String, List<String>>> = listOf(
    "telegram" to listOf("tg"),
    "youtube" to listOf("yt", "ytb"),
    "wechat" to listOf("wc", "wx", "vx"),
    "微信" to listOf("wc", "wx", "vx"),
    "whatsapp" to listOf("wa"),
    "instagram" to listOf("ig", "ins"),
    "facebook" to listOf("fb"),
    "twitter" to listOf("tw", "x"),
    "discord" to listOf("dc"),
    "spotify" to listOf("sp"),
    "netflix" to listOf("nf"),
    "tiktok" to listOf("tt"),
    "bilibili" to listOf("bl", "bili"),
    "哔哩哔哩" to listOf("bl", "bili"),
    "chatgpt" to listOf("gpt"),
    "deepseek" to listOf("ds"),
    "github" to listOf("gh"),
    "alipay" to listOf("zfb"),
    "支付宝" to listOf("zfb")
)

private fun lookupAppAliases(packageName: String, appName: String): List<String> {
    val pkgLower = packageName.lowercase(Locale.ROOT)
    val nameLower = appName.lowercase(Locale.ROOT)
    val result = linkedSetOf<String>()

    for ((pkgPrefix, aliases) in APP_ALIAS_RULES_BY_PKG) {
        if (pkgLower == pkgPrefix || pkgLower.startsWith("$pkgPrefix.")) {
            result.addAll(aliases)
        }
    }
    for ((nameKey, aliases) in APP_ALIAS_RULES_BY_NAME) {
        if (nameLower.contains(nameKey)) {
            result.addAll(aliases)
        }
    }
    return result.toList()
}

private data class SettingSpec(
    val titleZh: String,
    val titleEn: String,
    val aliases: List<String>,
    val intentCandidates: List<() -> Intent>
)

private fun resolveFirstLaunchableIntent(pm: PackageManager, candidates: List<() -> Intent>): Intent? {
    for (factory in candidates) {
        val candidate = factory()
        val info = try {
            pm.resolveActivity(candidate, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (_: Exception) {
            null
        }
        // On Android 14+, launching a non-exported activity across apps throws SecurityException.
        if (info?.activityInfo != null && info.activityInfo.exported) {
            return candidate
        }
    }
    return null
}

private fun componentIntent(pkg: String, cls: String): () -> Intent = {
    Intent().setComponent(ComponentName(pkg, cls))
}

private fun actionIntent(action: String): () -> Intent = {
    Intent(action)
}

private fun packageLaunchIntent(pm: PackageManager, pkg: String): () -> Intent = {
    pm.getLaunchIntentForPackage(pkg) ?: Intent().setPackage(pkg)
}

/**
 * Brand-specific settings modules:
 * Separates Battery main page from Battery Saver and adds verified OEM-specific pages.
 */
@SuppressLint("InlinedApi")
private fun buildBrandSpecificSettings(pm: PackageManager, brand: DeviceBrand): List<SettingSpec> {
    return when (brand) {
        DeviceBrand.SAMSUNG -> listOf(
            // Battery main page on Samsung One UI (China & Global variants + Android fallback)
            SettingSpec(
                titleZh = "电池",
                titleEn = "Battery",
                aliases = listOf("dc", "dl", "battery"),
                intentCandidates = listOf(
                    actionIntent("com.samsung.android.sm.ACTION_BATTERY"),
                    componentIntent("com.samsung.android.sm_cn", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                    componentIntent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                    componentIntent("com.android.settings", "com.android.settings.Settings\$PowerUsageSummaryActivity"),
                    actionIntent(Intent.ACTION_POWER_USAGE_SUMMARY)
                )
            ),
            SettingSpec(
                titleZh = "设备维护 / 智能管理器",
                titleEn = "Device Care",
                aliases = listOf("sbwh", "znglq"),
                intentCandidates = listOf(
                    actionIntent("com.samsung.android.sm.ACTION_DASHBOARD"),
                    packageLaunchIntent(pm, "com.samsung.android.sm_cn"),
                    packageLaunchIntent(pm, "com.samsung.android.lool")
                )
            ),
            SettingSpec(
                titleZh = "模式与日常程序",
                titleEn = "Modes and Routines",
                aliases = listOf("routines", "bixby"),
                intentCandidates = listOf(
                    actionIntent("com.samsung.android.app.routines.action.LAUNCH_ROUTINES"),
                    packageLaunchIntent(pm, "com.samsung.android.app.routines")
                )
            ),
            SettingSpec(
                titleZh = "侧屏幕面板",
                titleEn = "Edge Panels",
                aliases = listOf("edge"),
                intentCandidates = listOf(
                    actionIntent("com.samsung.android.app.cocktailbarservice.settings"),
                    componentIntent(
                        "com.samsung.android.app.cocktailbarservice",
                        "com.samsung.android.app.cocktailbarservice.settings.EdgePanelsActivity"
                    )
                )
            ),
            SettingSpec(
                titleZh = "息屏显示 / AOD",
                titleEn = "Always On Display",
                aliases = listOf("aod"),
                intentCandidates = listOf(
                    componentIntent(
                        "com.samsung.android.app.aodservice",
                        "com.samsung.android.app.aodservice.settings.AODSettingsMain"
                    ),
                    actionIntent("android.settings.LOCK_SCREEN_SETTINGS")
                )
            ),
            SettingSpec(
                titleZh = "护眼模式 / 舒适度保护",
                titleEn = "Eye Comfort Shield",
                aliases = emptyList(),
                intentCandidates = listOf(
                    componentIntent("com.android.settings", "com.android.settings.Settings\$BlueLightFilterSettingsActivity"),
                    actionIntent(Settings.ACTION_NIGHT_DISPLAY_SETTINGS)
                )
            ),
            SettingSpec(
                titleZh = "高级功能 / 动作与手势",
                titleEn = "Advanced Features",
                aliases = emptyList(),
                intentCandidates = listOf(
                    componentIntent("com.android.settings", "com.android.settings.Settings\$UsefulFeatureMainActivity")
                )
            ),
            SettingSpec(
                titleZh = "安全文件夹",
                titleEn = "Secure Folder",
                aliases = listOf("knox"),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.samsung.knox.securefolder")
                )
            ),
            SettingSpec(
                titleZh = "软件更新 / 系统更新",
                titleEn = "Software Update",
                aliases = emptyList(),
                intentCandidates = listOf(
                    componentIntent("com.wssyncmldm", "com.idm.fotaagent.enabler.ui.main.MainActivity"),
                    actionIntent("android.settings.SYSTEM_UPDATE_SETTINGS")
                )
            )
        )

        DeviceBrand.XIAOMI -> listOf(
            SettingSpec(
                titleZh = "电池",
                titleEn = "Battery",
                aliases = listOf("dc", "dl", "battery"),
                intentCandidates = listOf(
                    componentIntent("com.miui.securitycenter", "com.miui.powercenter.PowerMainActivity"),
                    actionIntent(Intent.ACTION_POWER_USAGE_SUMMARY)
                )
            ),
            SettingSpec(
                titleZh = "应用双开",
                titleEn = "Dual Apps",
                aliases = emptyList(),
                intentCandidates = listOf(
                    componentIntent("com.miui.securitycore", "com.miui.xspace.ui.activity.XSpaceSettingActivity")
                )
            ),
            SettingSpec(
                titleZh = "应用锁",
                titleEn = "App Lock",
                aliases = emptyList(),
                intentCandidates = listOf(
                    componentIntent("com.miui.securitycenter", "com.miui.applicationlock.SettingLockActivity")
                )
            ),
            SettingSpec(
                titleZh = "自启动管理",
                titleEn = "Autostart",
                aliases = emptyList(),
                intentCandidates = listOf(
                    componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                )
            ),
            SettingSpec(
                titleZh = "手机管家",
                titleEn = "Security Center",
                aliases = emptyList(),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.miui.securitycenter")
                )
            )
        )

        DeviceBrand.OPLUS -> listOf(
            SettingSpec(
                titleZh = "电池",
                titleEn = "Battery",
                aliases = listOf("dc", "dl", "battery"),
                intentCandidates = listOf(
                    componentIntent("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"),
                    packageLaunchIntent(pm, "com.oplus.battery"),
                    actionIntent(Intent.ACTION_POWER_USAGE_SUMMARY)
                )
            ),
            SettingSpec(
                titleZh = "应用分身",
                titleEn = "App Cloner",
                aliases = emptyList(),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.oplus.multiapp"),
                    packageLaunchIntent(pm, "com.coloros.multiapp")
                )
            ),
            SettingSpec(
                titleZh = "手机管家 / 权限隐私",
                titleEn = "Phone Manager",
                aliases = emptyList(),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.oplus.phonemanager"),
                    packageLaunchIntent(pm, "com.coloros.phonemanager")
                )
            )
        )

        DeviceBrand.VIVO -> listOf(
            SettingSpec(
                titleZh = "电池",
                titleEn = "Battery",
                aliases = listOf("dc", "dl", "battery"),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.iqoo.powersaving"),
                    actionIntent(Intent.ACTION_POWER_USAGE_SUMMARY)
                )
            ),
            SettingSpec(
                titleZh = "i管家",
                titleEn = "iManager",
                aliases = emptyList(),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.iqoo.secure")
                )
            ),
            SettingSpec(
                titleZh = "应用分身",
                titleEn = "App Clone",
                aliases = emptyList(),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.vivo.doubleinstance")
                )
            )
        )

        DeviceBrand.HUAWEI_HONOR -> listOf(
            SettingSpec(
                titleZh = "电池",
                titleEn = "Battery",
                aliases = listOf("dc", "dl", "battery"),
                intentCandidates = listOf(
                    componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"),
                    componentIntent("com.hihonor.systemmanager", "com.hihonor.systemmanager.power.ui.HwPowerManagerActivity"),
                    actionIntent(Intent.ACTION_POWER_USAGE_SUMMARY)
                )
            ),
            SettingSpec(
                titleZh = "系统管家",
                titleEn = "Optimizer",
                aliases = emptyList(),
                intentCandidates = listOf(
                    packageLaunchIntent(pm, "com.huawei.systemmanager"),
                    packageLaunchIntent(pm, "com.hihonor.systemmanager")
                )
            )
        )

        DeviceBrand.PIXEL_AOSP -> listOf(
            SettingSpec(
                titleZh = "电池",
                titleEn = "Battery",
                aliases = listOf("dc", "dl", "battery"),
                intentCandidates = listOf(
                    actionIntent(Intent.ACTION_POWER_USAGE_SUMMARY)
                )
            )
        )
    }
}

@SuppressLint("InlinedApi")
private fun buildCommonSettings(brand: DeviceBrand): List<SettingSpec> {
    val list = mutableListOf(
        SettingSpec("WLAN / 无线网络", "Wi-Fi", listOf("wifi", "wlan"), listOf(actionIntent(Settings.ACTION_WIFI_SETTINGS))),
        SettingSpec("蓝牙设置", "Bluetooth", listOf("bt", "bluetooth"), listOf(actionIntent(Settings.ACTION_BLUETOOTH_SETTINGS))),
        SettingSpec("移动网络 / 蜂窝", "Mobile Network", listOf("5g", "sim"), listOf(actionIntent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))),
        SettingSpec("数据使用 / 流量管理", "Data Usage", emptyList(), listOf(actionIntent(Settings.ACTION_DATA_USAGE_SETTINGS))),
        SettingSpec("个人热点 / 共享", "Hotspot & Tethering", listOf("hotspot"), listOf(actionIntent(Settings.ACTION_WIRELESS_SETTINGS))),
        SettingSpec("NFC / 触碰付款", "NFC", listOf("nfc"), listOf(actionIntent(Settings.ACTION_NFC_SETTINGS))),
        SettingSpec("飞行模式", "Airplane Mode", emptyList(), listOf(actionIntent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))),
        SettingSpec("投屏 / 多屏互动", "Cast", emptyList(), listOf(actionIntent(Settings.ACTION_CAST_SETTINGS))),
        SettingSpec("VPN 设置", "VPN", listOf("vpn"), listOf(actionIntent(Settings.ACTION_VPN_SETTINGS))),
        SettingSpec("显示与亮度 / 屏幕", "Display", emptyList(), listOf(actionIntent(Settings.ACTION_DISPLAY_SETTINGS))),
        SettingSpec("声音与振动 / 音量", "Sound & Vibration", emptyList(), listOf(actionIntent(Settings.ACTION_SOUND_SETTINGS))),
        SettingSpec("壁纸与个性化", "Wallpaper", emptyList(), listOf(actionIntent(Intent.ACTION_SET_WALLPAPER))),
        SettingSpec("日期和时间", "Date & Time", emptyList(), listOf(actionIntent(Settings.ACTION_DATE_SETTINGS))),
        SettingSpec("语言与地区", "Language & Region", emptyList(), listOf(actionIntent(Settings.ACTION_LOCALE_SETTINGS))),
        SettingSpec("输入法与键盘", "Keyboard & Input", emptyList(), listOf(actionIntent(Settings.ACTION_INPUT_METHOD_SETTINGS))),
        SettingSpec("位置信息 / 定位", "Location", listOf("gps"), listOf(actionIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))),
        SettingSpec("密码与安全 / 隐私", "Security & Privacy", emptyList(), listOf(actionIntent(Settings.ACTION_SECURITY_SETTINGS))),
        SettingSpec("生物识别 / 指纹面部", "Biometrics", emptyList(), listOf(actionIntent(Settings.ACTION_BIOMETRIC_ENROLL))),
        SettingSpec("锁屏与息屏", "Lock Screen", emptyList(), listOf(actionIntent("android.settings.LOCK_SCREEN_SETTINGS"))),
        SettingSpec("应用管理 / 列表", "Apps", emptyList(), listOf(actionIntent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))),
        SettingSpec("默认应用设置", "Default Apps", emptyList(), listOf(actionIntent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))),
        SettingSpec("通知管理 / 状态栏", "Notifications", emptyList(), listOf(actionIntent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS))),
        SettingSpec("悬浮窗权限", "Display Over Other Apps", emptyList(), listOf(actionIntent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))),
        SettingSpec("未知来源安装", "Install Unknown Apps", emptyList(), listOf(actionIntent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))),
        SettingSpec("画中画权限", "Picture-in-Picture", emptyList(), listOf(actionIntent("android.settings.PICTURE_IN_PICTURE_SETTINGS"))),
        SettingSpec("后台运行 / 电池优化", "Battery Optimization", emptyList(), listOf(actionIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))),
        SettingSpec("存储空间 / 内存", "Storage", emptyList(), listOf(actionIntent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))),
        SettingSpec("账号与同步", "Accounts & Sync", emptyList(), listOf(actionIntent(Settings.ACTION_SYNC_SETTINGS))),
        SettingSpec("关于手机 / 设备信息", "About Phone", emptyList(), listOf(actionIntent(Settings.ACTION_DEVICE_INFO_SETTINGS))),
        SettingSpec("开发者选项", "Developer Options", emptyList(), listOf(actionIntent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))),
        SettingSpec("省电模式", "Battery Saver", emptyList(), listOf(actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS))),
        SettingSpec("无障碍 / 辅助功能", "Accessibility", emptyList(), listOf(actionIntent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),
        SettingSpec("勿扰模式", "Do Not Disturb", listOf("dnd"), listOf(actionIntent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))),
        SettingSpec("默认桌面 / 主屏幕", "Home Screen", emptyList(), listOf(actionIntent(Settings.ACTION_HOME_SETTINGS))),
        SettingSpec("单手操作模式", "One-handed Mode", emptyList(), listOf(actionIntent("android.settings.action.ONE_HANDED_SETTINGS")))
    )

    if (brand != DeviceBrand.SAMSUNG) {
        list.add(
            SettingSpec("深色模式 / 护眼", "Dark Mode & Night Light", emptyList(), listOf(actionIntent(Settings.ACTION_NIGHT_DISPLAY_SETTINGS)))
        )
        list.add(
            SettingSpec("系统更新 / 软件升级", "System Update", emptyList(), listOf(actionIntent("android.settings.SYSTEM_UPDATE_SETTINGS")))
        )
    }

    return list
}

fun getSystemSettingsItems(context: Context, isChinese: Boolean, brand: DeviceBrand): List<SearchItem> {
    val pm = context.packageManager
    val specs = buildBrandSpecificSettings(pm, brand) + buildCommonSettings(brand)
    val seenTitles = hashSetOf<String>()
    val result = mutableListOf<SearchItem>()

    for (spec in specs) {
        val name = if (isChinese) spec.titleZh else spec.titleEn
        if (!seenTitles.add(name)) continue
        val resolved = resolveFirstLaunchableIntent(pm, spec.intentCandidates) ?: continue
        val (initials, indices) = getSearchInitialsInfo(name)
        val launchTag = resolved.action ?: resolved.component?.flattenToShortString() ?: name
        result.add(
            SearchItem(
                title = name,
                subtitle = "",
                initials = initials,
                initialIndices = indices,
                launchData = launchTag,
                type = SearchItemType.SYSTEM_ACTION,
                aliases = spec.aliases,
                resolvedIntent = resolved
            )
        )
    }
    return result
}

private val ICON_PACK_INTENT_ACTIONS = listOf(
    "org.adw.launcher.THEMES",
    "com.novalauncher.THEME",
    "com.teslacoilsw.launcher.THEME",
    "com.anddoes.launcher.THEME",
    "com.gau.go.launcherex.theme",
    "com.fede.launcher.THEME_ICONPACK",
    "app.lawnchair.icons.THEMED_ICON",
    "com.dlto.atom.launcher.THEME",
    "com.lge.launcher2.THEME",
    "com.sonymobile.home.ICON_PACK"
)

private fun detectIconPackPackages(pm: PackageManager): Set<String> {
    val iconPackPkgs = hashSetOf<String>()
    for (action in ICON_PACK_INTENT_ACTIONS) {
        try {
            val matches = pm.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL)
            for (ri in matches) {
                ri.activityInfo?.packageName?.let { iconPackPkgs.add(it) }
            }
        } catch (_: Exception) {
        }
    }
    // Also check ADW icon-pack category on ACTION_MAIN (used by many CandyBar / Blueprint icon packs)
    try {
        val mainIconPack = Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME")
        pm.queryIntentActivities(mainIconPack, PackageManager.MATCH_ALL).forEach {
            it.activityInfo?.packageName?.let { pkg -> iconPackPkgs.add(pkg) }
        }
    } catch (_: Exception) {
    }
    return iconPackPkgs
}

private fun isIconPackHeuristic(packageName: String, appName: String): Boolean {
    val pkg = packageName.lowercase(Locale.ROOT)
    val name = appName.lowercase(Locale.ROOT)
    // Do not misclassify launchers or theme managers as icon packs
    if (pkg.contains("launcher") && !pkg.contains("iconpack") && !name.contains("图标包") && !name.contains("icon pack")) {
        return false
    }
    return name.contains("图标包") ||
        name.contains("icon pack") ||
        name.contains("iconpack") ||
        pkg.contains("iconpack") ||
        pkg.contains("icon_pack")
}

fun getInstalledApps(context: Context): List<SearchItem> {
    val pm = context.packageManager
    val iconPackPackages = detectIconPackPackages(pm)
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolveInfos.map {
        val name = it.loadLabel(pm).toString()
        val pkg = it.activityInfo.packageName
        val type = when {
            pkg == "com.android.settings" || pkg.contains("settings", true) -> SearchItemType.SETTINGS
            pkg in iconPackPackages || isIconPackHeuristic(pkg, name) -> SearchItemType.ICON_PACK
            else -> SearchItemType.APP
        }
        val (initials, indices) = getSearchInitialsInfo(name)
        val aliases = if (type == SearchItemType.APP) lookupAppAliases(pkg, name) else emptyList()
        SearchItem(
            title = name,
            subtitle = "",
            initials = initials,
            initialIndices = indices,
            launchData = pkg,
            type = type,
            aliases = aliases
        )
    }
}

private data class RawPhoneRecord(
    val contactId: String,
    val displayName: String,
    val rawNumber: String,
    val normalizedNumber: String,
    val phoneType: Int,
    val customLabel: String?,
    val photoUri: String?
)

private fun normalizePhoneNumber(number: String): String {
    val sb = StringBuilder()
    for (c in number) {
        if (c.isDigit() || (c == '+' && sb.isEmpty())) sb.append(c)
    }
    return sb.toString()
}

private fun formatPhoneTypeLabel(
    context: Context,
    phoneType: Int,
    customLabel: String?,
    isChinese: Boolean
): String {
    if (isChinese) {
        return when (phoneType) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "家庭"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> "公司"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "手机"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "主要"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM ->
                customLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "其他"
            else -> ContactsContract.CommonDataKinds.Phone
                .getTypeLabel(context.resources, phoneType, customLabel)
                .toString()
                .ifBlank { "其他" }
        }
    }
    return ContactsContract.CommonDataKinds.Phone
        .getTypeLabel(context.resources, phoneType, customLabel)
        .toString()
        .ifBlank { "Other" }
}

/**
 * Loads contacts with avatar URIs.
 * If a contact has multiple distinct phone numbers, splits them into separate entries
 * with a type suffix in parentheses, e.g. "张三 (家庭)" and "张三 (公司)".
 */
fun getContacts(context: Context, isChinese: Boolean): List<SearchItem> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }

    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.TYPE,
        ContactsContract.CommonDataKinds.Phone.LABEL,
        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
    )

    val groupedByContact = linkedMapOf<String, MutableList<RawPhoneRecord>>()

    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.getString(0)?.trim() ?: ""
            val num = cursor.getString(1)?.trim() ?: ""
            val id = cursor.getString(2) ?: ""
            val type = cursor.getInt(3)
            val label = cursor.getString(4)
            val photoUri = cursor.getString(5)

            if (name.isNotEmpty() && num.isNotEmpty()) {
                val normalized = normalizePhoneNumber(num)
                if (normalized.isNotEmpty()) {
                    val key = if (id.isNotEmpty()) id else name
                    val bucket = groupedByContact.getOrPut(key) { mutableListOf() }
                    if (bucket.none { it.normalizedNumber == normalized }) {
                        bucket.add(
                            RawPhoneRecord(
                                contactId = id,
                                displayName = name,
                                rawNumber = num,
                                normalizedNumber = normalized,
                                phoneType = type,
                                customLabel = label,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        }
    }

    val results = mutableListOf<SearchItem>()
    for ((_, records) in groupedByContact) {
        val hasMultipleNumbers = records.size > 1
        for (record in records) {
            val (initials, indices) = getSearchInitialsInfo(record.displayName)
            val displayTitle = if (hasMultipleNumbers) {
                val typeLabel = formatPhoneTypeLabel(context, record.phoneType, record.customLabel, isChinese)
                "${record.displayName} ($typeLabel)"
            } else {
                record.displayName
            }
            results.add(
                SearchItem(
                    title = displayTitle,
                    subtitle = record.rawNumber,
                    initials = initials,
                    initialIndices = indices,
                    launchData = record.rawNumber,
                    type = SearchItemType.CONTACT,
                    contactId = record.contactId,
                    photoUri = record.photoUri,
                    phoneNumber = record.rawNumber
                )
            )
        }
    }
    return results
}
