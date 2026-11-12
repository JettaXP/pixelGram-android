package com.pixelgram.android.feature.premium

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

object Card {
    fun luhn(number: String): Boolean {
        if (number.length !in 12..19 || number.any { !it.isDigit() }) return false
        var sum = 0
        number.reversed().forEachIndexed { i, c ->
            var d = c - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    fun brand(number: String): String? = when {
        number.startsWith("4") -> "Visa"
        number.take(2).toIntOrNull() in 51..55 || number.take(4).toIntOrNull() in 2221..2720 -> "Mastercard"
        number.take(4).toIntOrNull() in 2200..2204 -> "МИР"
        number.startsWith("34") || number.startsWith("37") -> "American Express"
        number.startsWith("62") -> "UnionPay"
        number.take(4).toIntOrNull() in 3528..3589 -> "JCB"
        number.startsWith("6011") || number.startsWith("65") -> "Discover"
        else -> null
    }

    fun cvcLength(number: String) = if (number.startsWith("34") || number.startsWith("37")) 4 else 3

    fun digits(text: String, max: Int) = text.filter { it in '0'..'9' }.take(max)

    fun month(text: String): String {
        val d = digits(text, 2)
        return if (d.length == 1 && d[0] > '1') "0$d" else d
    }

    fun monthValid(month: String) = month.length == 2 && month.toInt() in 1..12

    fun expiryValid(month: String, year: String): Boolean {
        if (!monthValid(month) || year.length != 2) return false
        val now = Calendar.getInstance()
        val y = 2000 + year.toInt()
        val current = now.get(Calendar.YEAR)
        if (y > current + 20) return false
        return y > current || (y == current && month.toInt() >= now.get(Calendar.MONTH) + 1)
    }

    val Grouping = VisualTransformation { text ->
        val raw = text.text
        val out = raw.chunked(4).joinToString(" ")
        TransformedText(
            AnnotatedString(out),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = if (offset == 0) 0 else (offset + (offset - 1) / 4).coerceAtMost(out.length)
                override fun transformedToOriginal(offset: Int) = (offset - offset / 5).coerceIn(0, raw.length)
            },
        )
    }

    data class Details(val number: String, val month: String, val year: String, val cvc: String, val holder: String, val country: String, val postal: String)

    fun providerName(provider: TdApi.PaymentProvider?) = when (provider) {
        is TdApi.PaymentProviderStripe -> "Stripe"
        is TdApi.PaymentProviderSmartGlocal -> "Smart Glocal"
        else -> null
    }

    suspend fun tokenize(provider: TdApi.PaymentProvider, test: Boolean, card: Details): String? = withContext(Dispatchers.IO) {
        runCatching {
            when (provider) {
                is TdApi.PaymentProviderStripe -> stripe(provider.publishableKey, card)
                is TdApi.PaymentProviderSmartGlocal -> smartGlocal(provider, test, card)
                else -> null
            }
        }.getOrNull()
    }

    private fun stripe(key: String, card: Details): String? {
        val fields = buildList {
            add("card[number]" to card.number)
            add("card[exp_month]" to card.month)
            add("card[exp_year]" to "20${card.year}")
            add("card[cvc]" to card.cvc)
            if (card.holder.isNotBlank()) add("card[name]" to card.holder)
            if (card.country.isNotBlank()) add("card[address_country]" to card.country)
            if (card.postal.isNotBlank()) add("card[address_zip]" to card.postal)
        }
        val body = fields.joinToString("&") { (k, v) -> URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8") }
        val response = post(URL("https://api.stripe.com/v1/tokens"), body, "application/x-www-form-urlencoded", "Authorization" to "Bearer $key") ?: return null
        val json = JSONObject(response)
        return JSONObject().put("type", json.optString("type", "card")).put("id", json.getString("id")).toString()
    }

    private fun smartGlocal(provider: TdApi.PaymentProviderSmartGlocal, test: Boolean, card: Details): String? {
        val url = provider.tokenizeUrl.takeIf { it.startsWith("https://") && it.endsWith(".smart-glocal.com/cds/v1/tokenize/card") }
            ?: if (test) "https://tgb-playground.smart-glocal.com/cds/v1/tokenize/card" else "https://tgb.smart-glocal.com/cds/v1/tokenize/card"
        val body = JSONObject().put(
            "card",
            JSONObject()
                .put("number", card.number)
                .put("expiration_month", card.month)
                .put("expiration_year", "20${card.year}")
                .put("security_code", card.cvc),
        ).toString()
        val response = post(URL(url), body, "application/json", "X-PUBLIC-TOKEN" to provider.publicToken) ?: return null
        val token = JSONObject(response).getJSONObject("data").getString("token")
        return JSONObject().put("token", token).put("type", "card").toString()
    }

    private fun post(url: URL, body: String, type: String, header: Pair<String, String>): String? {
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 30_000
            conn.readTimeout = 80_000
            conn.useCaches = false
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", type)
            conn.setRequestProperty(header.first, header.second)
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            conn.disconnect()
        }
    }
}
