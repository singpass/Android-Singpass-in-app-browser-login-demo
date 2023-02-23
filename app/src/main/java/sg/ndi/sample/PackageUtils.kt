package sg.ndi.sample

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build

object PackageUtils {

    private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Number): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getPackageInfo(packageName, flags.toInt())
        }
    }

    fun isSpmInstalled(application: Application): Boolean {
        return try {
            val packageManager = application.packageManager
            val packageInfo = packageManager.getPackageInfoCompat("sg.ndi.sp", 0)
            packageInfo.applicationInfo.enabled
        } catch (ex: NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
