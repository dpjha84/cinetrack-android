package com.example.data.model

import java.util.Locale

data class RegionOption(
    val code: String,
    val name: String,
    val flag: String
)

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

object RegionalDefaults {
    val supportedRegions = listOf(
        RegionOption("US", "United States", "🇺🇸"),
        RegionOption("GB", "United Kingdom", "🇬🇧"),
        RegionOption("IN", "India", "🇮🇳"),
        RegionOption("JP", "Japan", "🇯🇵"),
        RegionOption("KR", "South Korea", "🇰🇷"),
        RegionOption("FR", "France", "🇫🇷"),
        RegionOption("DE", "Germany", "🇩🇪"),
        RegionOption("ES", "Spain", "🇪🇸"),
        RegionOption("CA", "Canada", "🇨🇦"),
        RegionOption("AU", "Australia", "🇦🇺"),
        RegionOption("BR", "Brazil", "🇧🇷"),
        RegionOption("MX", "Mexico", "🇲🇽"),
        RegionOption("IT", "Italy", "🇮🇹"),
        RegionOption("GLOBAL", "Worldwide / Global", "🌐")
    )

    val supportedLanguages = listOf(
        LanguageOption("all", "All Languages", "All", "🌐"),
        LanguageOption("en", "English", "English", "🇺🇸"),
        LanguageOption("ja", "Japanese", "日本語", "🇯🇵"),
        LanguageOption("ko", "Korean", "한국어", "🇰🇷"),
        LanguageOption("es", "Spanish", "Español", "🇪🇸"),
        LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳"),
        LanguageOption("fr", "French", "Français", "🇫🇷"),
        LanguageOption("de", "German", "Deutsch", "🇩🇪"),
        LanguageOption("it", "Italian", "Italiano", "🇮🇹")
    )

    fun detectDeviceRegion(): RegionOption {
        val deviceCountry = Locale.getDefault().country.uppercase()
        return supportedRegions.find { it.code.equals(deviceCountry, ignoreCase = true) }
            ?: RegionOption(deviceCountry.ifBlank { "US" }, Locale.getDefault().displayCountry.ifBlank { "United States" }, "📍")
    }

    fun detectDeviceLanguage(): LanguageOption {
        val deviceLang = Locale.getDefault().language.lowercase()
        return supportedLanguages.find { it.code.equals(deviceLang, ignoreCase = true) }
            ?: LanguageOption(deviceLang.ifBlank { "en" }, Locale.getDefault().displayLanguage.ifBlank { "English" }, Locale.getDefault().displayLanguage, "🌐")
    }

    fun getLanguageLabel(code: String): String {
        val found = supportedLanguages.find { it.code.equals(code, ignoreCase = true) }
        return found?.let { "${it.flag} ${it.nativeName}" } ?: code.uppercase()
    }

    fun getRegionLabel(code: String): String {
        val found = supportedRegions.find { it.code.equals(code, ignoreCase = true) }
        return found?.let { "${it.flag} ${it.name}" } ?: code.uppercase()
    }
}
