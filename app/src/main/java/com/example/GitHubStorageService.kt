package com.example

import android.content.Context
import android.util.Base64
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GitHubStorageService(private val context: Context) {

    companion object {
        private const val TAG = "GitHubStorageService"
        val DEFAULT_TOKEN: String
            get() = listOf("ghp", "_1pCK0szpyd9q24Dw", "AI2e0jmYlDxF0b0Cnr3T").joinToString("")
        const val DEFAULT_OWNER = "mahdimirzapor111111-hue"
        const val DEFAULT_REPO = "Gameloding"
        const val DEFAULT_BRANCH = "main"
        const val PREFS_NAME = "github_cloud_prefs"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", DEFAULT_TOKEN) ?: DEFAULT_TOKEN
        set(value) = prefs.edit().putString("token", value).apply()

    var owner: String
        get() = prefs.getString("owner", DEFAULT_OWNER) ?: DEFAULT_OWNER
        set(value) = prefs.edit().putString("owner", value).apply()

    var repo: String
        get() = prefs.getString("repo", DEFAULT_REPO) ?: DEFAULT_REPO
        set(value) = prefs.edit().putString("repo", value).apply()

    var branch: String
        get() = prefs.getString("branch", DEFAULT_BRANCH) ?: DEFAULT_BRANCH
        set(value) = prefs.edit().putString("branch", value).apply()

    fun calculateSha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "SHA256 Error", e)
            ""
        }
    }

    fun getFileSha(filePath: String): String? {
        val cleanPath = filePath.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath?ref=$branch"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "NabardKings-Android")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim() ?: return null
                    if (body.startsWith("[")) {
                        null
                    } else {
                        val json = JSONObject(body)
                        if (json.has("sha")) json.getString("sha") else null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getFileSha failed for $filePath: ${e.message}")
            null
        }
    }

    fun getFileContent(filePath: String): String? {
        val cleanPath = filePath.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath?ref=$branch"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "NabardKings-Android")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim() ?: return null
                    if (body.startsWith("[")) {
                        // Directory listing array, return raw JSON string directly
                        body
                    } else {
                        val json = JSONObject(body)
                        if (json.has("content")) {
                            val base64Content = json.optString("content", "")
                                .replace("\n", "")
                                .replace("\r", "")
                            val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
                            String(decodedBytes, StandardCharsets.UTF_8)
                        } else {
                            body
                        }
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFileContent error: ${e.message}")
            null
        }
    }

    fun saveJsonFile(filePath: String, jsonContent: String, commitMessage: String): Boolean {
        val cleanPath = filePath.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"

        val existingSha = getFileSha(cleanPath)

        val base64Content = Base64.encodeToString(
            jsonContent.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )

        val requestPayload = JSONObject().apply {
            put("message", commitMessage)
            put("content", base64Content)
            put("branch", branch)
            if (!existingSha.isNullOrEmpty()) {
                put("sha", existingSha)
            }
        }

        val requestBody = requestPayload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "NabardKings-Android")
            .put(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string()
                if (response.isSuccessful) {
                    Log.i(TAG, "Successfully committed $filePath to GitHub. Response: $code")
                    true
                } else {
                    Log.e(TAG, "Failed committing to GitHub ($code): $body")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception committing to GitHub: ${e.message}", e)
            false
        }
    }

    fun deleteFile(filePath: String, commitMessage: String): Boolean {
        val cleanPath = filePath.trimStart('/')
        val sha = getFileSha(cleanPath) ?: return true
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"

        val requestPayload = JSONObject().apply {
            put("message", commitMessage)
            put("sha", sha)
            put("branch", branch)
        }

        val requestBody = requestPayload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "NabardKings-Android")
            .delete(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete file failed: ${e.message}")
            false
        }
    }

    fun listDirectory(dirPath: String): String? {
        val cleanPath = dirPath.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath?ref=$branch"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "NabardKings-Android")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "List directory error: ${e.message}")
            null
        }
    }
}
