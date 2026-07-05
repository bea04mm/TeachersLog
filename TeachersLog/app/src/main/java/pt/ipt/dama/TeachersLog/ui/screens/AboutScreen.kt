package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.theme.AppTheme
import pt.ipt.dama.TeachersLog.utils.IconMapper

/**
 * AboutScreen displays information about the application, its purpose, 
 * authorship, and the technologies used.
 * 
 * It fulfills academic requirements by showing:
 * - Course and Institution details
 * - Author names and numbers
 * - List of third-party libraries and frameworks
 * 
 * @param onBackClick Callback for navigating back to the previous screen.
 */
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppTheme.colors.background,
        topBar = {
            TeacherTopBar(
                title = stringResource(R.string.nav_about),
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- App Branding Header ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val roundedShape = RoundedCornerShape(48.dp)
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(roundedShape)
                            .background(AppTheme.white),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = AppTheme.typography.headlineLarge,
                        color = AppTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.version_info),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Project Details ---
            item {
                TonalCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AboutSectionHeader(
                            title = stringResource(R.string.academic_overview),
                            icon = IconMapper.School,
                            isCentered = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.institution_label),
                            style = AppTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.course_label),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.curriculum_unit_label),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.academic_year_label),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- Authors Section ---
            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutSectionHeader(
                            title = stringResource(R.string.author_header),
                            icon = IconMapper.Group
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.author), style = AppTheme.typography.bodyLarge) },
                            leadingContent = { Icon(IconMapper.Account, contentDescription = null, tint = AppTheme.colors.primary) },
                            colors = ListItemDefaults.colors(containerColor = AppTheme.transparent)
                        )
                    }
                }
            }

            // --- Libraries Section ---
            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutSectionHeader(
                            title = stringResource(R.string.libraries_frameworks_header),
                            icon = IconMapper.Code
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val libs = listOf(
                            R.string.lib_jetpack_compose,
                            R.string.lib_room,
                            R.string.lib_retrofit,
                            R.string.lib_coil,
                            R.string.lib_mlkit,
                            R.string.lib_workmanager,
                            R.string.lib_datastore
                        )
                        libs.forEach { resId ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "•", modifier = Modifier.padding(end = 8.dp), color = AppTheme.colors.primary)
                                Text(text = stringResource(resId), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

/**
 * AboutSectionHeader provides a consistent title and icon for sections in the About screen.
 * 
 * @param title The header text to display.
 * @param icon The ImageVector icon representing the section.
 * @param isCentered If true, the header will be centered horizontally.
 */
@Composable
fun AboutSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCentered: Boolean = false
) {
    Row(
        modifier = if (isCentered) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )
    }
}
