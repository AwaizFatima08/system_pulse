package com.systempulse.app.engine

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.systempulse.app.data.model.IspInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IspResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    suspend fun resolve(): IspInfo = withContext(Dispatchers.IO) {
        // Primary: ip-api.com
        try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/?fields=status,message,country,city,isp,org,as,query")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JsonParser.parseString(body).asJsonObject
                    if (json.get("status")?.asString == "success") {
                        val isp = json.get("isp")?.asString ?: "Unknown ISP"
                        val query = json.get("query")?.asString ?: "0.0.0.0"
                        val asField = json.get("as")?.asString ?: ""
                        val asn = asField.split(" ").firstOrNull() ?: "AS0000"
                        val city = json.get("city")?.asString ?: ""
                        val country = json.get("country")?.asString ?: ""

                        return@withContext IspInfo(
                            asn = asn,
                            ispName = isp,
                            ipPublic = query,
                            city = city,
                            country = country
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to Cloudflare trace
        }

        // Fallback: Cloudflare trace
        try {
            val cfRequest = Request.Builder()
                .url("https://1.1.1.1/cdn-cgi/trace")
                .build()

            client.newCall(cfRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val map = body.lines().associate { line ->
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                    }
                    val ip = map["ip"] ?: "0.0.0.0"
                    val loc = map["loc"] ?: ""
                    return@withContext IspInfo(
                        asn = "Cloudflare CDN",
                        ispName = "Internet Service Provider",
                        ipPublic = ip,
                        city = loc,
                        country = loc
                    )
                }
            }
        } catch (_: Exception) {}

        IspInfo(
            asn = "AS-UNKNOWN",
            ispName = "Default ISP",
            ipPublic = "127.0.0.1",
            city = "Local",
            country = "Global"
        )
    }
}
