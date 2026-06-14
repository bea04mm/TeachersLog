package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

/**
 * SubjectEditContent is the stateless UI component for creating or editing a subject.
 *
 * It manages:
 * 1. A text field for the subject name.
 * 2. A selectable list of icons for visual representation.
 * 3. A selectable list of colors for theme consistency.
 * 4. A radio button group to select the grading scale.
 * 5. Validation to ensure the name is not empty before saving.
 *
 * @param isEditMode Boolean flag to determine if the screen is in 'Edit' or 'Add' mode.
 * @param initialName The initial name of the subject (empty for 'Add' mode).
 * @param initialIcon The initial icon name (e.g., "functions").
 * @param initialColor The initial color hex string (e.g., "#004AC6").
 * @param initialScale The initial grading scale identifier (e.g., "5", "20", "100").
 * @param onSave Callback executed when the user confirms the creation or update.
 * @param onBackClick Callback executed when the user navigates back.
 */
@Composable
fun SubjectEditContent(
    isEditMode: Boolean,
    initialName: String,
    initialIcon: String,
    initialColor: String,
    initialScale: String,
    onSave: (String, String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    // Local state for form fields
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedScale by remember { mutableStateOf(initialScale) }

    // Sync state if initial values change (e.g., when data is loaded from ViewModel)
    LaunchedEffect(initialName, initialIcon, initialColor, initialScale) {
        name = initialName
        selectedIcon = initialIcon
        selectedColor = initialColor
        selectedScale = initialScale
    }

    // Predefined set of icons available for subjects
    val icons = listOf(
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

    // Predefined palette for subject themes
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Name Input ---
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.subject_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // --- Grading Scale Selection ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.grading_scale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val scales = listOf("5" to R.string.scale_5, "20" to R.string.scale_20, "100" to R.string.scale_100)
                scales.forEach { (scale, labelRes) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedScale = scale }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedScale == scale),
                            onClick = { selectedScale = scale }
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // --- Icon Selection ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.select_icon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(icons) { (iconName, icon) ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary 
                                            else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- Color Selection ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.select_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(colors) { colorHex ->
                        val color = Color(colorHex.toColorInt())
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 3.dp,
                                    color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface 
                                            else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Save Button ---
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selectedIcon, selectedColor, selectedScale)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/**
 * SubjectEditScreen is the stateful Composable that connects the [TeacherViewModel]
 * to the [SubjectEditContent] UI.
 *
 * @param viewModel The shared [TeacherViewModel] for CRUD operations.
 * @param subjectId The ID of the subject to edit, or null to create a new one.
 * @param onBackClick Callback for navigating back.
 */
@Composable
fun SubjectEditScreen(
    viewModel: TeacherViewModel,
    subjectId: Int?,
    onBackClick: () -> Unit,
) {
    // Observe existing subject data if in edit mode
    val existingSubject by if (subjectId != null) {
        viewModel.getSubjectById(subjectId).observeAsState()
    } else {
        remember { mutableStateOf(null) }
    }

    SubjectEditContent(
        isEditMode = subjectId != null,
        initialName = existingSubject?.name ?: "",
        initialIcon = existingSubject?.iconName ?: "functions",
        initialColor = existingSubject?.colorHex ?: "#004AC6",
        initialScale = existingSubject?.gradingScale ?: "5",
        onSave = { name, icon, color, scale ->
            if (subjectId == null) {
                viewModel.addSubject(name, icon, color, scale)
            } else {
                existingSubject?.let {
                    viewModel.updateSubject(it.copy(
                        name = name, 
                        iconName = icon, 
                        colorHex = color,
                        gradingScale = scale
                    ))
                }
            }
            onBackClick()
        },
        onBackClick = onBackClick
    )
}

/**
 * Preview for the 'Add Subject' mode in light theme.
 */
@Preview(showBackground = true, name = "Add Subject Light")
@Composable
fun AddSubjectPreview() {
    TeachersLogTheme(darkTheme = false) {
        SubjectEditContent(
            isEditMode = false,
            initialName = "",
            initialIcon = "functions",
            initialColor = "#004AC6",
            initialScale = "5",
            onSave = { _, _, _, _ -> },
            onBackClick = {}
        )
    }
}

/**
 * Preview for the 'Edit Subject' mode in dark theme.
 */
@Preview(showBackground = true, name = "Edit Subject Dark")
@Composable
fun EditSubjectPreview() {
    TeachersLogTheme(darkTheme = true) {
        SubjectEditContent(
            isEditMode = true,
            initialName = "Mathematics",
            initialIcon = "calculate",
            initialColor = "#B3261E",
            initialScale = "20",
            onSave = { _, _, _, _ -> },
            onBackClick = {}
        )
    }
}
