package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Subject
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

@Composable
fun SubjectEditScreen(
    viewModel: TeacherViewModel,
    subjectId: Int?,
    onBackClick: () -> Unit
) {
    val existingSubject by if (subjectId != null) {
        viewModel.getSubjectById(subjectId).observeAsState()
    } else {
        remember { mutableStateOf<Subject?>(null) }
    }

    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("functions") }
    var selectedColor by remember { mutableStateOf("#004AC6") }

    LaunchedEffect(existingSubject) {
        existingSubject?.let {
            name = it.name
            selectedIcon = it.iconName
            selectedColor = it.colorHex
        }
    }

    val icons = listOf(
        "functions" to Icons.Default.Functions,
        "biotech" to Icons.Default.Biotech,
        "history_edu" to Icons.Default.HistoryEdu,
        "language" to Icons.Default.Language,
        "brush" to Icons.Default.Brush,
        "calculate" to Icons.Default.Calculate,
        "code" to Icons.Default.Code,
        "menu_book" to Icons.Default.MenuBook,
        "music_note" to Icons.Default.MusicNote,
        "sports_basketball" to Icons.Default.SportsBasketball
    )

    val colors = listOf(
        "#004AC6", "#006686", "#7D5260", "#6750A4", 
        "#B3261E", "#386A20", "#006874", "#A03D2F",
        "#00639B", "#9C4146"
    )

    Scaffold(
        topBar = {
            TeacherTopBar(
                title = stringResource(if (subjectId == null) R.string.add_subject else R.string.edit_subject),
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.subject_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        if (subjectId == null) {
                            viewModel.addSubject(name, selectedIcon, selectedColor)
                        } else {
                            existingSubject?.let {
                                viewModel.updateSubject(it.copy(name = name, iconName = selectedIcon, colorHex = selectedColor))
                            }
                        }
                        onBackClick()
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
