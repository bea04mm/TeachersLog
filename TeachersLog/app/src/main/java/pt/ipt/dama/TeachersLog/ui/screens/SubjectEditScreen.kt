package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper

/**
 * SubjectEditContent is the stateless UI component for creating or editing a subject.
 * 
 * It manages name input, icon selection, and theme color selection.
 *
 * @param isEditMode Flag indicating if we are modifying an existing subject.
 * @param initialName Starting name value.
 * @param initialIcon Starting icon identifier.
 * @param initialColor Starting color hex string.
 * @param onSave Callback executed when the save button is pressed with valid data.
 * @param onBackClick Callback for back navigation.
 */
@Composable
fun SubjectEditContent(
    isEditMode: Boolean,
    initialName: String,
    initialIcon: String,
    initialColor: String,
    onSave: (String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    // Sync state when initial values are provided (e.g., from DB)
    LaunchedEffect(initialName, initialIcon, initialColor) {
        name = initialName
        selectedIcon = initialIcon
        selectedColor = initialColor
    }

    val icons = IconMapper.availableIcons

    val colors = listOf(
        "#004AC6", "#006686", "#7D5260", "#6750A4", 
        "#B3261E", "#386A20", "#006874", "#A03D2F",
        "#00639B", "#9C4146"
    )

    Scaffold(
        topBar = { 
            TeacherTopBar(
                title = stringResource(if (!isEditMode) R.string.add_subject else R.string.edit_subject), 
                showBackButton = true, 
                onBackClick = onBackClick
            ) 
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Subject Identity
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text(stringResource(R.string.subject_name)) }, 
                modifier = Modifier.fillMaxWidth(), 
                singleLine = true
            )

            // Dynamic Icon Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.select_icon), 
                    style = AppTheme.typography.labelMedium, 
                    color = AppTheme.colors.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(icons) { (iconName, icon) ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(AppTheme.shapes.extraLarge)
                                .background(if (selectedIcon == iconName) AppTheme.colors.primaryContainer else AppTheme.colors.surfaceContainerLow)
                                .border(width = 2.dp, color = if (selectedIcon == iconName) AppTheme.colors.primary else AppTheme.transparent, shape = AppTheme.shapes.extraLarge)
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon, 
                                contentDescription = null, 
                                tint = if (selectedIcon == iconName) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Theme Color Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.select_color), 
                    style = AppTheme.typography.labelMedium, 
                    color = AppTheme.colors.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(colors) { colorHex ->
                        val color = AppTheme.parseColor(colorHex)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(AppTheme.shapes.extraLarge)
                                .background(color)
                                .border(width = 3.dp, color = if (selectedColor == colorHex) AppTheme.colors.onSurface else AppTheme.transparent, shape = AppTheme.shapes.extraLarge)
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }

            // Persistence
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, selectedIcon, selectedColor) }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save)) 
            }
        }
    }
}

/**
 * SubjectEditScreen connects the UI to the academic data management.
 *
 * @param academicViewModel The ViewModel providing subject CRUD.
 * @param subjectId Optional ID of the subject to modify.
 * @param onBackClick Callback to go back.
 */
@Composable
fun SubjectEditScreen(
    academicViewModel: AcademicViewModel,
    subjectId: Int?,
    onBackClick: () -> Unit,
) {
    val existingSubject by if (subjectId != null) {
        academicViewModel.getSubjectById(subjectId).observeAsState()
    } else {
        remember { mutableStateOf(null) }
    }

    SubjectEditContent(
        isEditMode = subjectId != null,
        initialName = existingSubject?.name ?: "",
        initialIcon = existingSubject?.iconName ?: "functions",
        initialColor = existingSubject?.colorHex ?: "#004AC6",
        onSave = { name, icon, color ->
            if (subjectId == null) {
                academicViewModel.addSubject(name, icon, color)
            } else {
                existingSubject?.let {
                    academicViewModel.updateSubject(it.copy(name = name, iconName = icon, colorHex = color))
                }
            }
            onBackClick()
        },
        onBackClick = onBackClick
    )
}
