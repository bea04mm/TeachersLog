package pt.ipt.dama.TeachersLog.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pt.ipt.dama.TeachersLog.R

/**
 * Utility object to map stored string names (from the database) to localized string resources.
 * This ensures that names like "1st Semester", which are stored as plain text, 
 * can be displayed in the user's current language.
 */
object LocalizedNameMapper {

    /**
     * Maps a semester/period name to its corresponding string resource ID.
     * Returns null if no mapping is found.
     */
    fun getSemesterResource(name: String?): Int? {
        return when (name) {
            "1st Semester", "1.º Semestre" -> R.string.period_1st_semester
            "2nd Semester", "2.º Semestre" -> R.string.period_2nd_semester
            "1st Period", "1.º Período" -> R.string.period_1st_period
            "2nd Period", "2.º Período" -> R.string.period_2nd_period
            "3rd Period", "3.º Período" -> R.string.period_3rd_period
            "Annual", "Anual" -> R.string.period_annual
            else -> null
        }
    }

    /**
     * Composable function that returns the localized name or the original name if no mapping exists.
     */
    @Composable
    fun getLocalizedSemesterName(name: String): String {
        val resId = getSemesterResource(name)
        return if (resId != null) {
            stringResource(resId)
        } else {
            name
        }
    }
}
