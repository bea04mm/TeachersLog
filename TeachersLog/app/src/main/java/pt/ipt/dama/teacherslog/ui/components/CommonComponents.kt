package pt.ipt.dama.teacherslog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.ui.res.stringResource
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.utils.IconMapper

/**
 * TeacherTopBar is a standardized TopAppBar for the application.
 * 
 * It dynamically switches its icon based on the title and provides
 * support for both a side menu navigation and a back button.
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    // Spacing between icon and title
                    Spacer(modifier = Modifier.width(12.dp))
                }
                // Bold screen title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            // Priority 1: Show Back button if explicitly requested
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (onNavigationIconClick != null) {
                // Priority 2: Show Menu button if a callback is provided
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = Icons.Default.Menu, 
                        contentDescription = stringResource(R.string.menu),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tab: Dashboard
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(IconMapper.Dashboard, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.labelSmall) },
            colors = navItemColors
        )
        // Tab: Subjects/Classes
        NavigationBarItem(
            selected = currentRoute == "subjects",
            onClick = { onNavigate("subjects") },
            icon = { Icon(IconMapper.Subjects, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_classes), style = MaterialTheme.typography.labelSmall) },
            colors = navItemColors
        )
        // Tab: Settings
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(IconMapper.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.labelSmall) },
            colors = navItemColors
        )
    }
}

/**
 * TonalCard is a reusable surface container with a subtle background color and low elevation.
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
    color: Color = MaterialTheme.colorScheme.primary
) {
    TonalCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Decorative icon bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Descriptive label
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            // Primary value
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f", averageGrade) + suffix,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Student count pill at the bottom right
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            // Use the formatted string resource for student count
                            Text(
                                text = stringResource(R.string.students_count_label, studentCount),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compatibility wrapper for SubjectCard using the unified [AcademicSummaryCard].
 * 
 * @param name The name of the subject.
 * @param average The average grade string.
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
    average: String,
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
        averageGrade = average.toDoubleOrNull() ?: 0.0,
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
