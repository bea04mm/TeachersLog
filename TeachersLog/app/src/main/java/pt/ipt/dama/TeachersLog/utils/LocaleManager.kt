package pt.ipt.dama.TeachersLog.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * LocaleManager is a utility object responsible for managing and applying 
 * language (locale) changes across the application.
 * 
 * It provides methods to update the resources configuration with a specific 
 * language tag and to retrieve the currently active language.
 */

object LocaleManager {
    /**
     * Updates the given context's resources with the specified language.
     * 
     * This method creates a new configuration context with the updated locale 
     * and layout direction, allowing the application to switch languages dynamically.
     *
     * @param context The base [Context] to be updated.
     * @param language The language tag (e.g., "en" for English, "pt" for Portuguese).
     * @return A new [Context] wrapped with the updated configuration.
     */
    fun updateResources(context: Context, language: String): Context {
        // Create a Locale object from the language tag
        val locale = Locale.forLanguageTag(language)
        // Set the default locale for the JVM process
        Locale.setDefault(locale)

        // Clone the current configuration and set the new locale
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        // Ensure the layout direction (LTR or RTL) matches the locale
        config.setLayoutDirection(locale)

        // Return a new context with the applied configuration
        return context.createConfigurationContext(config)
    }

    /**
     * Applies the locale change to the activity's resources directly.
     * This is a workaround for dialogs and other system components that
     * use the Activity's resources directly.
     */
    fun applyLocaleToActivity(context: Context, language: String) {
        val locale = Locale.forLanguageTag(language)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Retrieves the current language code from the context's configuration.
     *
     * @param context The [Context] from which to retrieve the configuration.
     * @return The language code string (e.g., "en", "pt").
     */
    fun getLanguage(context: Context): String {
        // Access the primary locale from the configuration's locale list
        return context.resources.configuration.locales[0].language
    }
}
