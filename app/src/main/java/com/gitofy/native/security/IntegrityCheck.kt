package com.gitofy.native.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class IntegrityStatus { OK, TAMPERED, UNAVAILABLE }

data class IntegrityReport(val status: IntegrityStatus, val signatureSha256: String, val reason: String? = null)

/**
 * Anti-tamper for a native app: verify the installed APK is signed with the
 * expected certificate. This replaces the web app's bundle-hash check (which only
 * made sense for a repackaged JS bundle) with the stronger platform guarantee.
 *
 * Set EXPECTED_SIGNATURE to the release keystore's SHA-256 (see docs/signing.md).
 * While it is blank the check reports UNAVAILABLE (dev builds).
 */
@Singleton
class IntegrityCheck @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        /** Fill with the release signing cert SHA-256; blank disables (dev). */
        const val EXPECTED_SIGNATURE = ""
    }

    fun verify(): IntegrityReport {
        val actual = signingSha256() ?: return IntegrityReport(IntegrityStatus.UNAVAILABLE, "", "no signature")
        if (EXPECTED_SIGNATURE.isBlank()) return IntegrityReport(IntegrityStatus.UNAVAILABLE, actual, "no anchor set")
        val ok = actual.equals(EXPECTED_SIGNATURE, ignoreCase = true)
        return IntegrityReport(
            if (ok) IntegrityStatus.OK else IntegrityStatus.TAMPERED,
            actual,
            if (ok) null else "signature mismatch",
        )
    }

    private fun signingSha256(): String? = runCatching {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = pm.getPackageInfo(context.packageName, flags)
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") info.signatures
        } ?: return null
        val sig = signatures.firstOrNull() ?: return null
        val md = MessageDigest.getInstance("SHA-256")
        md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
    }.getOrNull()
}
