package com.example.demoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                
                // Launcher for standard permission (Android < 11)
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                     if (isGranted) {
                        startUpdate(context)
                     } else {
                        android.widget.Toast.makeText(context, "Permission Denied", android.widget.Toast.LENGTH_LONG).show()
                     }
                }

                // Launcher for Accessing All files
                val storageManagerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) {
                    if (android.os.Environment.isExternalStorageManager()) {
                        startUpdate(context)
                    } else {
                         android.widget.Toast.makeText(context, "Permission Denied", android.widget.Toast.LENGTH_LONG).show()
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
                            Button(onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    if (android.os.Environment.isExternalStorageManager()) {
                                        startUpdate(context)
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
                                Text("Update App Silently")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startUpdate(context: Context) {
        Thread {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val apk = File(downloadDir, "app_update.apk")
            
            if (apk.exists()) {
                Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "File found: ${apk.name}! Starting install...", android.widget.Toast.LENGTH_LONG).show()
                }
                installFromSdCard(context, apk)
            } else {
                val files = downloadDir.listFiles()?.joinToString { it.name } ?: "No files or permission denied"
                Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Target 'app_update.apk' NOT found.\nPath: ${downloadDir.absolutePath}\nFiles: $files", android.widget.Toast.LENGTH_LONG).show()
                    android.util.Log.d("UpdateDebug", "Path: ${downloadDir.absolutePath}, Files: $files")
                }
            }
        }.start()
    }

    private fun installFromSdCard(context: Context, apkFile: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        params.setAppPackageName(context.packageName)

        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)

        try {
            FileInputStream(apkFile).use { input ->
                session.openWrite("update", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

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
            session.abandon()
        } finally {
            session.close()
        }
    }
}

