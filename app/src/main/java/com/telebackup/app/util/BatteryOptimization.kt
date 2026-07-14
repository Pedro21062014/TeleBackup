package com.telebackup.app.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimization {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return try {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens the **native Android dialog** asking to allow unrestricted background
     * (ignore battery optimizations) for this package.
     *
     * Must be started from an [Activity] context so the system shows a modal dialog
     * instead of jumping to a full settings page.
     *
     * Requires REQUEST_IGNORE_BATTERY_OPTIMIZATIONS in the manifest.
     *
     * @return true if the request was launched (or already unrestricted)
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        if (isIgnoringBatteryOptimizations(context)) return true

        val pkg = context.packageName
        val activity = context.findActivity()

        // 1) Preferred: system modal for this app only
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
            }
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return true
        } catch (_: Exception) {
            // fall through
        }

        // 2) Fallback: battery optimization list
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (activity != null) activity.startActivity(intent)
            else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return true
        } catch (_: Exception) {
        }

        // 3) Last resort: app details
        return openAppDetails(context)
    }

    fun openBatterySettings(context: Context): Boolean {
        val activity = context.findActivity()
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (activity != null) activity.startActivity(intent)
            else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return true
        } catch (_: Exception) {
            return openAppDetails(context)
        }
    }

    private fun openAppDetails(context: Context): Boolean {
        return try {
            val activity = context.findActivity()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (activity != null) activity.startActivity(intent)
            else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
