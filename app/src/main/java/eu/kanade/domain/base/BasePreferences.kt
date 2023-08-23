package eu.kanade.domain.base

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.core.preference.getEnum
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.util.system.DeviceUtil

class BasePreferences(
    val context: Context,
    private val preferenceStore: PreferenceStore,
) {

    fun confirmExit() = preferenceStore.getBoolean("pref_confirm_exit", false)

    fun downloadedOnly() = preferenceStore.getBoolean("pref_downloaded_only", false)

    fun incognitoMode() = preferenceStore.getBoolean("incognito_mode", false)

    fun automaticExtUpdates() = preferenceStore.getBoolean("automatic_ext_updates", true)

    fun extensionInstaller() = preferenceStore.getEnum(
        "extension_installer",
        if (DeviceUtil.isMiui) PreferenceValues.ExtensionInstaller.LEGACY else PreferenceValues.ExtensionInstaller.PACKAGEINSTALLER,
    )

    // acra is disabled
    fun acraEnabled() = preferenceStore.getBoolean("acra.enable", false)

    fun deviceHasPip() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}
