package com.example.workouttracker.ui.nutrition

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

object OAuth1FatSecret {
    private const val OAUTH_VERSION = "1.0"
    private const val SIGN_METHOD = "HMAC-SHA1"

    // percent-encode per RFC3986
    private fun pctEncode(s: String): String {
        return URLEncoder.encode(s, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun nonce(): String = UUID.randomUUID().toString().replace("-", "")
    private fun timestamp(): String = (System.currentTimeMillis() / 1000).toString()

    private fun hmacSha1(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        val spec = SecretKeySpec(key.toByteArray(UTF_8), "HmacSHA1")
        mac.init(spec)
        val raw = mac.doFinal(data.toByteArray(UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    // Выполняет GET запрос к FatSecret REST API с OAuth1 (HMAC-SHA1).
    // baseUrl — "https://platform.fatsecret.com/rest/server.api"
    // apiParams — map с параметрами API (method, формат и т.д.)
    // consumerKey / consumerSecret — креды FatSecret OAuth1 (consumer)
    suspend fun callFatSecret(
        client: OkHttpClient,
        baseUrl: String,
        apiParams: Map<String, String>,
        consumerKey: String,
        consumerSecret: String
    ): String? = withContext(Dispatchers.IO) {
        val oauthParams = TreeMap<String, String>()
        oauthParams["oauth_consumer_key"] = consumerKey
        oauthParams["oauth_nonce"] = nonce()
        oauthParams["oauth_signature_method"] = SIGN_METHOD
        oauthParams["oauth_timestamp"] = timestamp()
        oauthParams["oauth_version"] = OAUTH_VERSION

        // Собираем все параметры (API + oauth), percent-encode ключи и значения
        val allParams = TreeMap<String, String>()
        apiParams.forEach { (k, v) -> allParams[pctEncode(k)] = pctEncode(v) }
        oauthParams.forEach { (k, v) -> allParams[pctEncode(k)] = pctEncode(v) }

        val paramString = allParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val httpMethod = "GET"
        val baseString = httpMethod + "&" + pctEncode(baseUrl) + "&" + pctEncode(paramString)

        // signing key: pctEncode(consumerSecret) + "&" (no token secret)
        val signingKey = pctEncode(consumerSecret) + "&"
        val signature = hmacSha1(baseString, signingKey)

        // Authorization header: oauth params + oauth_signature
        val authParams = oauthParams.toMutableMap()
        authParams["oauth_signature"] = signature

        val authHeaderValue = "OAuth " + authParams.entries.joinToString(", ") {
            "${pctEncode(it.key)}=\"${pctEncode(it.value)}\""
        }

        // Собираем URL с query (только apiParams)
        val query = apiParams.entries.joinToString("&") { "${pctEncode(it.key)}=${pctEncode(it.value)}" }
        val url = if (baseUrl.contains("?")) "$baseUrl&$query" else "$baseUrl?$query"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", authHeaderValue)
            .build()

        val response = runCatching { client.newCall(request).execute() }.getOrNull() ?: return@withContext null
        if (!response.isSuccessful) {
            // бросим исключение для удобства отладки — вызывающий код должен ловить
            val err = response.body?.string().orEmpty()
            throw RuntimeException("FatSecret call failed ${response.code}: $err")
        }
        response.body?.string()
    }
}
