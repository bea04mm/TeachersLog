package pt.ipt.dama.TeachersLog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.AcademicYear
import pt.ipt.dama.TeachersLog.data.model.Semester
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper

/**
 * The content displayed inside the side navigation drawer.
 *
 * It provides two main scrollable sections:
 * 1. Academic Years Management: Selection and deletion of school years.
 * 2. Academic Periods Management: Selection and deletion of terms/semesters.
 *
 * @param academicYears List of all registered academic years.
 * @param selectedYearId The ID of the currently selected year for filtering.
 * @param semesters List of semesters for the currently selected year.
 * @param selectedSemesterId The ID of the currently selected semester/period.
 * @param onYearSelect Callback triggered when an academic year is selected.
 * @param onYearDelete Callback triggered when the delete icon for a year is clicked.
 * @param onAddYear Callback to open the academic year creation dialog.
 * @param onSemesterSelect Callback triggered when a semester/period is selected.
 * @param onSemesterDelete Callback triggered when the delete icon for a period is clicked.
 * @param onAddPeriod Callback to open the period creation dialog.
 */
@Composable
fun TeacherDrawerContent(
    academicYears: List<AcademicYear>,
    selectedYearId: Int?,
    semesters: List<Semester>,
    selectedSemesterId: Int?,
    onYearSelect: (Int) -> Unit,
    onYearDelete: (AcademicYear) -> Unit,
    onYearClone: (AcademicYear) -> Unit,
    onAddYear: () -> Unit,
    onSemesterSelect: (Int) -> Unit,
    onSemesterDelete: (Semester) -> Unit,
    onSemesterClone: (Semester) -> Unit,
    onAddPeriod: () -> Unit
) {
    // Define shared colors for selected drawer items to match bottom bar indicator
    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = AppTheme.colors.primaryContainer.copy(alpha = 0.4f),
        selectedIconColor = AppTheme.colors.primary,
        selectedTextColor = AppTheme.colors.primary,
        unselectedIconColor = AppTheme.colors.onSurfaceVariant,
        unselectedTextColor = AppTheme.colors.onSurfaceVariant
    )

    ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- Academic Years Section ---
            // Occupies 50% of the space when balanced with the period section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.academic_years_header), 
                    style = AppTheme.typography.labelSmall, 
                    color = AppTheme.colors.primary, 
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(academicYears) { year ->
                        val isSelected = year.id == selectedYearId
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(), 
                                    horizontalArrangement = Arrangement.SpaceBetween, 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(year.year, style = if (isSelected) AppTheme.typography.titleMedium else AppTheme.typography.bodyLarge)
                                    // Actions: Show direct Clone if selected, or Menu if not
                                    if (isSelected) {
                                        IconButton(onClick = { onYearClone(year) }, modifier = Modifier.size(24.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_clone_year),
                                                tint = AppTheme.colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.cd_options),
                                                    tint = AppTheme.colors.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.clone_label)) },
                                                    onClick = { showMenu = false; onYearClone(year) },
                                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete_label)) },
                                                    onClick = { showMenu = false; onYearDelete(year) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = { onYearSelect(year.id) },
                            icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            shape = AppTheme.shapes.medium,
                            colors = drawerItemColors
                        )
                    }
                }
                OutlinedButton(onClick = onAddYear, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.add_new_year))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Academic Periods Section ---
            // Occupies the other 50% of the vertical space
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.academic_periods_header_label),
                    style = AppTheme.typography.labelSmall, 
                    color = AppTheme.colors.primary, 
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(semesters) { semester ->
                        val isSelected = semester.id == selectedSemesterId
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(), 
                                    horizontalArrangement = Arrangement.SpaceBetween, 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val displayName = LocalizedNameMapper.getLocalizedSemesterName(semester.name)
                                    Text(displayName, style = if (isSelected) AppTheme.typography.titleMedium else AppTheme.typography.bodyLarge)
                                    if (isSelected) {
                                        IconButton(onClick = { onSemesterClone(semester) }, modifier = Modifier.size(24.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_clone_period),
                                                tint = AppTheme.colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.cd_options),
                                                    tint = AppTheme.colors.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.clone_label)) },
                                                    onClick = { showMenu = false; onSemesterClone(semester) },
                                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete_label)) },
                                                    onClick = { showMenu = false; onSemesterDelete(semester) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = { onSemesterSelect(semester.id) },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                            shape = AppTheme.shapes.medium,
                            colors = drawerItemColors
                        )
                    }
                }
                OutlinedButton(onClick = onAddPeriod, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.add_period))
                }
            }
        }
    }
}
