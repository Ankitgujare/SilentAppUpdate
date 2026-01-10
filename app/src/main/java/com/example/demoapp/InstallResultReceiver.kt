package com.example.demoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("InstallResultReceiver", "App updated! Relaunching...")
            android.widget.Toast.makeText(context, "Update Complete! Relaunching...", android.widget.Toast.LENGTH_LONG).show()
            
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            
            try {
                context.startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("InstallResultReceiver", "Could not start activity: ${e.message}")
            }

            // Fallback / Confirmation Notification
            showUpdateNotification(context, launchIntent)
            return
        }

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Unknown Error"

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d("InstallResultReceiver", "Requesting User Action")
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirmIntent)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("InstallResultReceiver", "Success! Waiting for MY_PACKAGE_REPLACED...")
            }
            PackageInstaller.STATUS_FAILURE, PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.e("InstallResultReceiver", "Install Failed: $message")
                android.widget.Toast.makeText(context, "Install Failed: $message", android.widget.Toast.LENGTH_LONG).show()
            }
            else -> {
                Log.d("InstallResultReceiver", "Status: $status, Msg: $message")
            }
        }
    }

    private fun showUpdateNotification(context: Context, pendingIntent: Intent?) {
        val channelId = "update_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "App Updates",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pIntent = android.app.PendingIntent.getActivity(
            context, 0, pendingIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("App Updated")
            .setContentText("Tap to open the new version")
            .setContentIntent(pIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}
