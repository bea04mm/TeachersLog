package pt.ipt.dama.TeachersLog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.utils.IconMapper
import androidx.compose.ui.platform.LocalLocale

/**
 * CompositionLocal to provide a global SnackbarHostState.
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

/**
 * Extension function to show styled snackbars.
 */
suspend fun SnackbarHostState.showAppSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Long
): SnackbarResult {
    return showSnackbar(
        AppSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            duration = duration,
            type = type
        )
    )
}

/**
 * Defines the semantic types for snackbar notifications.
 */
enum class SnackbarType {
    INFO, SUCCESS, ERROR
}

/**
 * Custom visuals that include a type for styling.
 */
class AppSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Long,
    val type: SnackbarType = SnackbarType.INFO
) : SnackbarVisuals

/**
 * A beautiful, personalized Snackbar component.
 *
 * @param snackbarData The data provided by the SnackbarHost.
 * @param modifier The [Modifier] to apply.
 */
@Composable
fun AppSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val visuals = snackbarData.visuals as? AppSnackbarVisuals
    val type = visuals?.type ?: SnackbarType.INFO

    val containerColor = when (type) {
        SnackbarType.SUCCESS -> AppTheme.success.copy(alpha = 0.9f)
        SnackbarType.ERROR -> AppTheme.critical.copy(alpha = 0.9f)
        SnackbarType.INFO -> AppTheme.colors.primaryContainer
    }
    
    val contentColor = when (type) {
        SnackbarType.SUCCESS, SnackbarType.ERROR -> Color.White
        SnackbarType.INFO -> AppTheme.colors.onPrimaryContainer
    }

    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = AppTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(AppTheme.shapes.small)
                    .background(contentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(type) {
                        SnackbarType.SUCCESS -> IconMapper.Check
                        SnackbarType.ERROR -> IconMapper.Warning
                        SnackbarType.INFO -> IconMapper.Notifications
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = snackbarData.visuals.message,
                style = AppTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = contentColor
            )
            if (snackbarData.visuals.actionLabel != null) {
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(
                        text = snackbarData.visuals.actionLabel!!,
                        color = contentColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * TeacherTopBar is a standardized TopAppBar for the application.
 * 
 * It provides support for both a side menu navigation and a back button,
 * and allows for an optional contextual icon next to the title.
 *
 * @param title The text to display as the screen title.
 * @param modifier The [Modifier] to be applied to the TopAppBar.
 * @param titleIcon Optional icon to display next to the title.
 * @param onNavigationIconClick Callback for the side menu icon. If null, icon is hidden.
 * @param showBackButton Boolean flag to toggle the back button display.
 * @param onBackClick Callback for the back button navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onNavigationIconClick: (() -> Unit)? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Contextual icon with primary theme color
                if (titleIcon != null) {
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    // Spacing between icon and title
                    Spacer(modifier = Modifier.width(12.dp))
                }
                // Bold screen title using project typography
                Text(
                    text = title,
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colors.onSurface
                )
            }
        },
        navigationIcon = {
            // Priority 1: Show Back button if explicitly requested
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = IconMapper.Back, 
                        contentDescription = stringResource(R.string.back),
                        tint = AppTheme.colors.primary
                    )
                }
            } else if (onNavigationIconClick != null) {
                // Priority 2: Show Menu button if a callback is provided
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = IconMapper.Menu,
                        contentDescription = stringResource(R.string.menu),
                        tint = AppTheme.colors.primary
                    )
                }
            }
        },
        actions = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.colors.surfaceContainer
        )
    )
}

/**
 * TeacherBottomBar is the main navigation bar containing tabs for high-level app destinations.
 *
 * @param currentRoute The currently active route string for selection highlighting.
 * @param modifier The [Modifier] to be applied.
 * @param onNavigate Callback executed when a tab is selected.
 */
@Composable
fun TeacherBottomBar(
    currentRoute: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = modifier,
        containerColor = AppTheme.colors.surfaceContainer,
        contentColor = AppTheme.colors.onSurfaceVariant
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = AppTheme.colors.primary,
            selectedTextColor = AppTheme.colors.primary,
            indicatorColor = AppTheme.colors.primaryContainer.copy(alpha = 0.4f),
            unselectedIconColor = AppTheme.colors.onSurfaceVariant,
            unselectedTextColor = AppTheme.colors.onSurfaceVariant
        )

        // Tab: Dashboard
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(IconMapper.Dashboard, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_dashboard), style = AppTheme.typography.labelSmall) },
            colors = navItemColors
        )
        // Tab: Subjects/Classes
        NavigationBarItem(
            selected = currentRoute == "subjects",
            onClick = { onNavigate("subjects") },
            icon = { Icon(IconMapper.Subjects, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_classes), style = AppTheme.typography.labelSmall) },
            colors = navItemColors
        )
        // Tab: Settings
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(IconMapper.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings), style = AppTheme.typography.labelSmall) },
            colors = navItemColors
        )
    }
}

/**
 * Reusable header for sections, ensuring consistent typography and color.
 * 
 * @param label The small label text (subtitle).
 * @param title The large headline text.
 * @param modifier The [Modifier] to apply.
 */
@Composable
fun SectionHeader(
    label: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.primary
        )
        Text(
            text = title,
            style = AppTheme.typography.headlineLarge,
            color = AppTheme.colors.onSurface
        )
    }
}

/**
 * TonalCard is a reusable surface container with a subtle background color and low elevation.
 * Uses the project's shapes and surfaceContainer for better visibility and Dark Mode support.
 *
 * @param modifier The [Modifier] to be applied.
 * @param content The Composable content to display inside the card.
 */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

/**
 * StatCard displays a summary metric with a representative icon and descriptive label.
 *
 * @param title The label describing the metric.
 * @param value The prominent headline value of the metric.
 * @param icon The icon to display in the decorative bubble.
 * @param modifier The [Modifier] to be applied.
 * @param color The accent color for the icon and its translucent background.
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.primary
) {
    TonalCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Decorative icon bubble using project shapes
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppTheme.shapes.small)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Descriptive label
            Text(
                text = title,
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.outline
            )
            // Primary value
            Text(
                text = value,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.onSurface
            )
        }
    }
}

/**
 * Unified card component for summarizing academic entities like Subjects or Classes.
 *
 * @param name The primary name/title to display (Subject or Class name).
 * @param averageGrade The numerical grade to display as an average.
 * @param studentCount Total number of students enrolled.
 * @param color The branding color for the accent bar and icons.
 * @param averageLabel The localized label for the average grade.
 * @param gradingScale The grading scale identifier (e.g., "5", "20", "100").
 * @param modifier The [Modifier] to be applied.
 * @param icon Optional [androidx.compose.ui.graphics.vector.ImageVector] to display next to the name.
 * @param actions Optional action buttons/menu to display in the top right.
 * @param onClick Callback for when the card is selected.
 */
@Composable
fun AcademicSummaryCard(
    name: String,
    averageGrade: Double,
    studentCount: Int,
    color: Color,
    averageLabel: String,
    gradingScale: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    val suffix = when(gradingScale) {
        "20" -> " / 20"
        "100" -> " %"
        else -> " / 5"
    }

    // Use centralized logic for color and percentage
    val percentage = AppTheme.calculatePercentage(averageGrade, gradingScale)
    val performanceColor = AppTheme.getGradeColor(percentage)

    TonalCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column {
            // Colored accent bar at the top of the card
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(color))
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row with optional icon, name and optional actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Wrapper for Icon and Title to keep them centered relative to each other
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon, 
                                contentDescription = null, 
                                tint = color, 
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = name,
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.onSurface
                        )
                    }
                    
                    // Render actions if provided (e.g., options menu)
                    if (actions != null) {
                        actions()
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Bottom row with stats and student count pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Average performance section
                    Column {
                        Text(
                            text = averageLabel,
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.outline
                        )
                        Text(
                            text = String.format(LocalLocale.current.platformLocale, "%.2f", averageGrade) + suffix,
                            style = AppTheme.typography.titleMedium,
                            color = performanceColor
                        )
                    }
                    // Student count pill at the bottom right
                    Surface(
                        color = AppTheme.colors.surfaceContainerLow,
                        shape = AppTheme.circle
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = IconMapper.Account, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp),
                                tint = AppTheme.colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Use the formatted string resource for student count
                            Text(
                                text = stringResource(R.string.students_count_label, studentCount),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable dialog for confirming the deletion of any academic item.
 * 
 * @param title The title of the dialog.
 * @param message The confirmation message.
 * @param onConfirm Callback when the delete button is pressed.
 * @param onDismiss Callback when the dialog is canceled.
 */
@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = AppTheme.typography.titleLarge) },
        text = { Text(text = message, style = AppTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error),
                shape = AppTheme.shapes.medium
            ) {
                Text(text = stringResource(R.string.delete_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        shape = AppTheme.shapes.large,
        containerColor = AppTheme.colors.surfaceContainerHigh
    )
}

/**
 * Compatibility wrapper for SubjectCard using the unified [AcademicSummaryCard].
 *
 * @param name The name of the subject.
 * @param averageGrade The average grade as a Double.
 * @param students The total student count.
 * @param color The branding color.
 * @param icon The representative icon.
 * @param gradingScale The grading scale identifier.
 * @param modifier The [Modifier] to be applied.
 * @param actions Optional action buttons/menu.
 * @param onClick Callback for selection.
 */
@Composable
fun SubjectCard(
    name: String,
    averageGrade: Double,
    students: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradingScale: String,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    AcademicSummaryCard(
        name = name,
        averageGrade = averageGrade,
        studentCount = students,
        color = color,
        averageLabel = stringResource(R.string.subject_average_label),
        gradingScale = gradingScale,
        modifier = modifier,
        icon = icon,
        actions = actions,
        onClick = onClick
    )
}

/**
 * Compatibility wrapper for ClassCard using the unified [AcademicSummaryCard].
 *
 * @param name The name of the class.
 * @param studentCount The number of students.
 * @param averageGrade The average grade.
 * @param color The branding color.
 * @param gradingScale The grading scale identifier.
 * @param modifier The [Modifier] to be applied.
 * @param iconName Optional icon identifier string.
 * @param actions Optional action buttons/menu.
 * @param onClick Callback for selection.
 */
@Composable
fun ClassCard(
    name: String,
    studentCount: Int,
    averageGrade: Double,
    color: Color,
    gradingScale: String,
    modifier: Modifier = Modifier,
    iconName: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    AcademicSummaryCard(
        name = name,
        averageGrade = averageGrade,
        studentCount = studentCount,
        color = color,
        averageLabel = stringResource(R.string.class_average_label),
        gradingScale = gradingScale,
        modifier = modifier,
        icon = iconName?.let { IconMapper.getIconByName(it) },
        actions = actions,
        onClick = onClick
    )
}
