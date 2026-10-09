package com.jeremy.browser.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateManager(private val context: Context) {

    private val repoOwner = "jch0029987-glitch"
    private val repoName = "Geek-browser"

    suspend fun checkForUpdate(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val apiURL = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            val connection = (apiURL.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)

                val tagName = json.getString("tag_name") // e.g. "v1.1.0"
                val cleanRemoteVersion = tagName.removePrefix("v")
                
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pInfo.versionName ?: "1.0.0"

                if (cleanRemoteVersion != currentVersion) {
                    val assets = json.getJSONArray("assets")
                    var downloadUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    if (downloadUrl.isNotEmpty()) {
                        return@withContext GitHubRelease(
                            tagName = tagName,
                            releaseNotes = json.optString("body", "Performance improvements and bug fixes."),
                            downloadUrl = downloadUrl
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun downloadAndInstall(downloadUrl: String, onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection)
            connection.connect()

            // Automatically manage an 'updates' subfolder in cache
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val file = File(updatesDir, "update.apk")
            
            val input = connection.inputStream
            val output = file.outputStream()

            val fileSize = connection.contentLength
            var downloaded = 0
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (fileSize > 0) {
                    val progress = ((downloaded * 100L) / fileSize).toInt()
                    onProgress(progress)
                }
            }
            output.close()
            input.close()

            triggerInstall(file)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun triggerInstall(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
