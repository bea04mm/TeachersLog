package pt.ipt.dama.teacherslog.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Utility to manage and apply locale changes across the application.
 */
object LocaleManager {

    /**
     * Updates the context with the selected language code ("en", "pt").
     */
    fun updateResources(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Helper to get the current language code from the current locale.
     */
    fun getLanguage(context: Context): String {
        return context.resources.configuration.locales[0].language
    }
}
