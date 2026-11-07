package com.pixelgram.android.util

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import java.util.Locale

data class PhoneDetails(val flag: String, val country: String, val carrier: String?)

object Phones {
    private val util by lazy { PhoneNumberUtil.getInstance() }
    private val codes by lazy { util.supportedCallingCodes.map(Int::toString).toSet() }

    fun isCallingCode(code: String) = code in codes

    fun isCodePrefix(code: String) = code.isNotEmpty() && codes.any { it.startsWith(code) }

    fun completesCode(code: String): Boolean =
        isCallingCode(code) && (code.length == 1 || codes.none { it != code && it.startsWith(code) })

    fun codeForRegion(region: String): String? =
        util.getCountryCodeForRegion(region.uppercase(Locale.ROOT)).takeIf { it != 0 }?.toString()

    private fun region(code: String, national: String): String? {
        val main = code.toIntOrNull()?.let(util::getRegionCodeForCountryCode)?.takeIf { it != "ZZ" && it != "001" }
        if (national.length >= 3) {
            val number = PhoneNumber().setCountryCode(code.toInt()).setNationalNumber(national.take(17).toLongOrNull() ?: return main)
            util.getRegionCodeForNumber(number)?.takeIf { it != "ZZ" && it != "001" }?.let { return it }
        }
        return main
    }

    fun country(code: String, national: String = ""): Pair<String, String>? {
        if (!isCallingCode(code)) return null
        val region = region(code, national) ?: return null
        return flag(region) to Locale("", region).getDisplayCountry(Locale.getDefault())
    }

    fun formatNational(code: String, national: String): String {
        if (national.isEmpty() || !isCallingCode(code)) return national
        val formatter = util.getAsYouTypeFormatter(util.getRegionCodeForCountryCode(code.toInt()))
        var out = ""
        for (c in "+$code$national") out = formatter.inputDigit(c)
        val prefix = "+$code"
        if (!out.startsWith(prefix)) return national
        val rest = out.removePrefix(prefix).trim().replace('-', ' ')
        return if (rest.filter(Char::isDigit) == national) rest else national
    }

    fun placeholder(code: String): String? {
        if (!isCallingCode(code)) return null
        val region = util.getRegionCodeForCountryCode(code.toInt())
        val example = util.getExampleNumberForType(region, PhoneNumberUtil.PhoneNumberType.MOBILE) ?: return null
        val national = util.getNationalSignificantNumber(example)
        return formatNational(code, national).map { if (it.isDigit()) '0' else it }.joinToString("")
    }

    fun split(digits: String): Pair<String, String>? {
        for (length in 1..3) {
            val code = digits.take(length)
            if (digits.length > length && isCallingCode(code)) return code to digits.drop(length)
        }
        return null
    }

    fun formatInternational(digits: String): String? {
        val (code, national) = split(digits) ?: return null
        return "$code ${formatNational(code, national)}"
    }

    fun details(digits: String): PhoneDetails? {
        val (code, national) = split(digits) ?: return null
        val (flag, name) = country(code, national) ?: return null
        val number = runCatching { util.parse("+$digits", null) }.getOrNull()
        val carrier = number?.let { runCatching { PhoneNumberToCarrierMapper.getInstance().getNameForNumber(it, Locale.getDefault()) }.getOrNull() }
        return PhoneDetails(flag, name, carrier?.takeIf { it.isNotBlank() })
    }

    private fun flag(region: String): String {
        if (region.length != 2) return ""
        return region.uppercase(Locale.ROOT).map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    }
}
