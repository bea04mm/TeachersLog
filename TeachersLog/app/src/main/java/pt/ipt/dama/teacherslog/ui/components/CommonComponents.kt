package pt.ipt.dama.teacherslog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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

/**
 * Standardized TopBar for the application.
 * Dynamically switches between branding icons and navigation buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationIconClick: (() -> Unit)? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    // Icons synchronized with BottomBar labels using modern AutoMirrored versions
    val titleIcon = when {
        title.contains("Dashboard", true) || title.contains("Teacher", true) -> Icons.Default.Home
        title.contains("Subjects", true) || title.contains("Classes", true) -> Icons.AutoMirrored.Filled.MenuBook
        title.contains("Settings", true) || title.contains("Preferences", true) -> Icons.Default.Settings
        else -> Icons.AutoMirrored.Filled.MenuBook
    }

    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = titleIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (onNavigationIconClick != null) {
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
 * Bottom navigation bar with state-aware active icons.
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
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = currentRoute == "subjects",
            onClick = { onNavigate("subjects") },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_classes), style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.labelSmall) }
        )
    }
}

/**
 * Standardized container with low elevation and adaptive background.
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
 * High-level summary card for the Dashboard.
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
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Card representing a school subject summary.
 */
@Composable
fun SubjectCard(
    name: String,
    average: String,
    students: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TonalCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(color))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Subject Average", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = average, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Students", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = students.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/**
 * Card representing a class summary.
 */
@Composable
fun ClassCard(
    name: String,
    studentCount: Int,
    averageGrade: Double,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TonalCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(color))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                            Text(text = "$studentCount Students", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text(
                        text = "SUBJECT AVERAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f / 10", averageGrade),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
