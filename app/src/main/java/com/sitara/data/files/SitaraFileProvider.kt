package com.sitara.data.files

import android.net.Uri
import androidx.core.content.FileProvider
import com.sitara.data.miniapps.MiniAppSigning

/**
 * Sitara's FileProvider. It tells other apps that *.sitapp files are mini app packages, so a package sent through
 * WhatsApp / Telegram / email keeps that type and tapping it on the other phone opens Sitara's installer
 * (Android doesn't know the extension, so plain FileProvider would say application/octet-stream).
 */
class SitaraFileProvider : FileProvider() {
    override fun getType(uri: Uri): String? =
        if (uri.path?.endsWith(".sitapp", ignoreCase = true) == true) MiniAppSigning.MIME else super.getType(uri)

    override fun getTypeAnonymous(uri: Uri): String? =
        if (uri.path?.endsWith(".sitapp", ignoreCase = true) == true) MiniAppSigning.MIME else super.getTypeAnonymous(uri)
}
