package com.example.demoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.os.Handler
import com.example.demoapp.ui.theme.DemoAppTheme
import java.io.File
import java.io.FileInputStream
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button



class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        setContent {
            DemoAppTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                var isInstalling by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                // Launcher for standard permission (Android < 11)
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                     if (isGranted) {
                        checkInstallPermissionAndStart(context) { isInstalling = true }
                     } else {
                        android.widget.Toast.makeText(context, "Storage Permission Denied", android.widget.Toast.LENGTH_LONG).show()
                     }
                }

                // Launcher for Accessing All files
                val storageManagerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) {
                    if (android.os.Environment.isExternalStorageManager()) {
                        checkInstallPermissionAndStart(context) { isInstalling = true }
                    } else {
                         android.widget.Toast.makeText(context, "Storage Permission Denied", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "App Version: $version")
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isInstalling) {
                                androidx.compose.material3.CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Installing Update...")
                            } else {
                                Button(onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        if (android.os.Environment.isExternalStorageManager()) {
                                            checkInstallPermissionAndStart(context) { isInstalling = true }
                                        } else {
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                                intent.addCategory("android.intent.category.DEFAULT")
                                                intent.data = android.net.Uri.parse(String.format("package:%s", context.packageName))
                                                storageManagerLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                val intent = Intent()
                                                intent.action = android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                                storageManagerLauncher.launch(intent)
                                            }
                                        }
                                    } else {
                                         permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                }) {
                                    Text("Update App")
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(onClick = {
                                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                                    try {
                                        dpm.clearDeviceOwnerApp(context.packageName)
                                        android.widget.Toast.makeText(context, "Device Owner Removed. You can now uninstall.", android.widget.Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to remove: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }) {
                                    Text("Remove Device Owner (Allow Uninstall)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkInstallPermissionAndStart(context: Context, onLoading: () -> Unit) {
        // 1. Check Install Packages Permission (Request Install Unknown Apps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                android.widget.Toast.makeText(context, "Please allow 'Install unknown apps' permission", android.widget.Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                return
            }
        }

        // 2. Check "Display over other apps" (Required for Background Relaunch)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                 android.widget.Toast.makeText(context, "Please allow 'Display over other apps' for auto-relaunch", android.widget.Toast.LENGTH_LONG).show()
                 val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                 }
                 context.startActivity(intent)
                 return
            }
        }

        onLoading()
        startUpdate(context)
    }

    private fun startUpdate(context: Context) {
        Thread {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            // Assuming filename is app_update.apk
            val apk = File(downloadDir, "app_update.apk")
            
            if (apk.exists()) {
                Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Installing...", android.widget.Toast.LENGTH_SHORT).show()
                }

                // Use Standard PackageInstaller
                // If App has INSTALL_PACKAGES permission (System/Privileged), this will be silent.
                // Otherwise, it shows the system dialog.
                installPackage(context, apk)
            } else {
                val files = downloadDir.listFiles()?.joinToString { it.name } ?: "No files or permission denied"
                Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "File 'app_update.apk' not found in Downloads.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun installPackage(context: Context, apkFile: File) {
        // Diagnostic: Check if we are Device Owner
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
             Handler(android.os.Looper.getMainLooper()).post {
                 android.widget.Toast.makeText(context, "SILENT INSTALL ERROR: App is NOT Device Owner!", android.widget.Toast.LENGTH_LONG).show()
             }
        }

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        // Set package name to ensure consistency now that we know they match
        params.setAppPackageName(context.packageName)

        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        var session: PackageInstaller.Session? = null
        try {
            val sessionId = installer.createSession(params)
            session = installer.openSession(sessionId)

            val size = apkFile.length()
            FileInputStream(apkFile).use { input ->
                session.openWrite("package", 0, size).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            // Reuse the existing receiver to capture status
            val intent = Intent(context, InstallResultReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            e.printStackTrace()
            session?.abandon()
            Handler(android.os.Looper.getMainLooper()).post {
                 android.widget.Toast.makeText(context, "Install Exception: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        } finally {
            session?.close()
        }
    }
}



