package pt.ipt.dama.teacherslog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * SettingsViewModel manages global UI state such as theme preferences and application language.
 *
 * @param application The application context, used for locale initialization.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Internal state for dark mode preference
    private val _isDarkMode = MutableLiveData(true)
    /** Observable state for the dark mode theme. */
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    // Internal state for the current application language
    private val _currentLanguage = MutableLiveData(LocaleManager.getLanguage(application))
    /** Observable state for the current language code (e.g., "en", "pt"). */
    val currentLanguage: LiveData<String> = _currentLanguage

    /**
     * Toggles the application's dark mode setting.
     * @param enabled True to enable dark mode, false for light mode.
     */
    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    /**
     * Updates the application's current language code.
     * @param languageCode The new language code (e.g., "en" or "pt").
     */
    fun setLanguage(languageCode: String) {
        if (_currentLanguage.value != languageCode) {
            _currentLanguage.value = languageCode
        }
    }
}

/**
 * Factory for [SettingsViewModel].
 */
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
