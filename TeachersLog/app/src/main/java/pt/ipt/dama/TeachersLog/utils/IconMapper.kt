package pt.ipt.dama.TeachersLog.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
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
    val Dashboard = Icons.Default.Home
    val Subjects = Icons.AutoMirrored.Filled.MenuBook
    val Settings = Icons.Default.Settings
    val Account = Icons.Default.Person
    val Logout = Icons.AutoMirrored.Filled.Logout

    // --- Common Action Icons ---
    val Add = Icons.Default.Add
    val Remove = Icons.Default.Remove
    val Save = Icons.Default.Save
    val Delete = Icons.Default.Delete
    val Edit = Icons.Default.Edit
    val More = Icons.Default.MoreVert
    val Copy = Icons.Default.ContentCopy
    val Camera = Icons.Default.PhotoCamera
    val Notes = Icons.AutoMirrored.Filled.Notes
    val Warning = Icons.Default.Warning
    val Visibility = Icons.Default.Visibility
    val VisibilityOff = Icons.Default.VisibilityOff
    val Analytics = Icons.Default.Analytics
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Menu = Icons.Default.Menu
    val SubItem = Icons.Default.SubdirectoryArrowRight
    val AddPhoto = Icons.Default.AddAPhoto
    val Notifications = Icons.Default.Notifications
    val Translate = Icons.Default.Translate
    val Palette = Icons.Default.Palette
    val CloudSync = Icons.Default.CloudSync
    val CloudDownload = Icons.Default.CloudDownload
    val AutoAwesome = Icons.Default.AutoAwesome
    val School = Icons.Default.School
    val Group = Icons.Default.Group
    val Code = Icons.Default.Code

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
