package com.example.workouttracker.ui.nutrition

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FatSecretTokenManager(
    context: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fatsecret_oauth", Context.MODE_PRIVATE)

    suspend fun getValidAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        val token = prefs.getString(KEY_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        if (!forceRefresh && !token.isNullOrBlank() && expiresAt - 30 > now) {
            return@withContext token
        }

        val clientId = prefs.getString(KEY_CLIENT_ID, null)
        val clientSecret = prefs.getString(KEY_CLIENT_SECRET, null)
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) return@withContext null

        val form = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", "basic")
            .build()

        val request = Request.Builder()
            .url("https://oauth.fatsecret.com/connect/token")
            .post(form)
            .header("Authorization", okhttp3.Credentials.basic(clientId, clientSecret))
            .build()

        val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return@withContext null
        if (!response.isSuccessful) return@withContext null

        val json = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull() ?: return@withContext null
        val accessToken = json.optString("access_token", "")
        val expiresIn = json.optLong("expires_in", 0L)
        if (accessToken.isBlank() || expiresIn <= 0) return@withContext null

        prefs.edit()
            .putString(KEY_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, now + expiresIn)
            .apply()

        accessToken
    }

    fun storeClientCredentialsTemporarily(clientId: String, clientSecret: String) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, clientId)
            .putString(KEY_CLIENT_SECRET, clientSecret)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
    }
}
