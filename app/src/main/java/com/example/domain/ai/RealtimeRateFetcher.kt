package com.example.domain.ai

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches REAL-TIME market rates from free, no-API-key public endpoints so the
 * brain can price items accurately against today's market:
 *   - FX: https://open.er-api.com/v6/latest/USD  (free, no key)
 *   - Gold/Silver: https://api.gold-api.com/price/{XAU|XAG}  (free, no key)
 * Everything is best-effort: failures return null and the caller falls back to
 * the cloud brain's knowledge.
 */
object RealtimeRateFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class RateResult(
        val ok: Boolean,
        val label: String,
        val value: Double?,
        val unit: String,
        val detail: String
    )

    private fun get(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", "DhanOM/1.2").build()
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (e: Exception) {
        null
    }

    /** Live USD → INR rate (and a few other common currencies). */
    fun usdToInr(): RateResult {
        val body = get("https://open.er-api.com/v6/latest/USD") ?: return RateResult(false, "USD/INR", null, "", "Could not reach exchange-rate service")
        return try {
            val json = JSONObject(body)
            val inr = json.optJSONObject("rates")?.optDouble("INR", 0.0) ?: 0.0
            if (inr <= 0) RateResult(false, "USD/INR", null, "", "No INR rate returned")
            else RateResult(true, "USD/INR", inr, "₹ per $", "1 USD = ₹$inr (live)")
        } catch (e: Exception) {
            RateResult(false, "USD/INR", null, "", "Bad response")
        }
    }

    /** Live gold price (USD per troy ounce), converted to INR using live FX. */
    fun goldPricePerGramInr(): RateResult {
        val body = get("https://api.gold-api.com/price/XAU") ?: return RateResult(false, "Gold", null, "", "Could not reach gold price service")
        return try {
            val json = JSONObject(body)
            val usdPerOz = json.optDouble("price", 0.0)
            if (usdPerOz <= 0) return RateResult(false, "Gold", null, "", "No price returned")
            val usdInr = usdToInr().value ?: 90.0
            val perGram = usdPerOz * usdInr / 31.1035
            RateResult(true, "Gold", perGram, "₹/gram", "≈ ₹%,.0f/gram (${"$"}${usdPerOz}/oz, live)".format(perGram))
        } catch (e: Exception) {
            RateResult(false, "Gold", null, "", "Bad response")
        }
    }

    /** Live silver price (USD per troy ounce) → INR per gram. */
    fun silverPricePerGramInr(): RateResult {
        val body = get("https://api.gold-api.com/price/XAG") ?: return RateResult(false, "Silver", null, "", "Could not reach silver price service")
        return try {
            val json = JSONObject(body)
            val usdPerOz = json.optDouble("price", 0.0)
            if (usdPerOz <= 0) return RateResult(false, "Silver", null, "", "No price returned")
            val usdInr = usdToInr().value ?: 90.0
            val perGram = usdPerOz * usdInr / 31.1035
            RateResult(true, "Silver", perGram, "₹/gram", "≈ ₹%,.0f/gram (live)".format(perGram))
        } catch (e: Exception) {
            RateResult(false, "Silver", null, "", "Bad response")
        }
    }
}
