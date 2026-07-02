package pt.ipt.dama.TeachersLog.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized utility for managing application-wide iconography.
 * 
 * Provides a single source of truth for both dynamic subject icons stored in the database
 * and static application icons used for navigation and branding.
 */
object IconMapper {

    // --- Core Navigation Icons ---
    
    /** Icon for the Dashboard/Home destination. */
    val Dashboard = Icons.Default.Home
    
    /** Icon for the Subjects/Curriculum destination. */
    val Subjects = Icons.AutoMirrored.Filled.MenuBook
    
    /** Icon for the Settings/Configuration destination. */
    val Settings = Icons.Default.Settings
    
    /** Icon for the User Account/Profile destination. */
    val Account = Icons.Default.Person

    /** Icon for the Sign-Out action. */
    val Logout = Icons.AutoMirrored.Filled.Logout

    // --- Dynamic Subject Icon Mapping ---

    /**
     * Maps a string identifier stored in the database to its corresponding Material ImageVector.
     * 
     * @param name The icon identifier string (e.g., "functions", "biotech").
     * @return The matching ImageVector, or [Subjects] as a default fallback.
     */
    fun getIconByName(name: String?): ImageVector {
        return when (name) {
            "functions" -> Icons.Default.Functions
            "biotech" -> Icons.Default.Biotech
            "history_edu" -> Icons.Default.HistoryEdu
            "language" -> Icons.Default.Language
            "brush" -> Icons.Default.Brush
            "calculate" -> Icons.Default.Calculate
            "code" -> Icons.Default.Code
            "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
            "music_note" -> Icons.Default.MusicNote
            "sports_basketball" -> Icons.Default.SportsBasketball
            else -> Subjects
        }
    }

    /**
     * Complete list of icons available for users to assign to Subjects.
     * Each entry is a pair of (Identifier String, Representative Icon).
     */
    val availableIcons = listOf(
        "functions" to Icons.Default.Functions,
        "biotech" to Icons.Default.Biotech,
        "history_edu" to Icons.Default.HistoryEdu,
        "language" to Icons.Default.Language,
        "brush" to Icons.Default.Brush,
        "calculate" to Icons.Default.Calculate,
        "code" to Icons.Default.Code,
        "menu_book" to Icons.AutoMirrored.Filled.MenuBook,
        "music_note" to Icons.Default.MusicNote,
        "sports_basketball" to Icons.Default.SportsBasketball
    )
}
