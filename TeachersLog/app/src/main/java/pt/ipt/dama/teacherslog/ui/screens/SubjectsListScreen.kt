/**
 * Displays a scrollable list of all school subjects for the selected year.
 * Each card shows the subject average grade and student count.
 */
package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Subject
import pt.ipt.dama.teacherslog.data.model.SubjectWithStats
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

@Composable
fun SubjectsListContent(
    subjects: List<SubjectWithStats>,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllClassesClick: () -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
    onDeleteSubjectClick: (Subject) -> Unit,
) {
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { /* TODO */ },
            title = { Text(stringResource(R.string.delete_subject_title)) },
            text = { Text(stringResource(R.string.delete_subject_message, subjectToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        subjectToDelete?.let { onDeleteSubjectClick(it) }
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { /* TODO */ }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = { 
            TeacherTopBar(
                title = stringResource(R.string.nav_classes),
                onNavigationIconClick = onMenuClick
            ) 
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSubjectClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_subject))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.my_subjects),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.active_curriculum),
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onViewAllClassesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.view_all_classes))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(subjects) { subject ->
                val subjectColor = Color(subject.colorHex.toColorInt())
                TonalCard(
                    modifier = Modifier.clickable { onSubjectClick(subject.id, subject.name) }
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(subjectColor))
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(subjectColor.copy(alpha = 0.1f), MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(subject.iconName) {
                                        "functions" -> Icons.Default.Functions
                                        "biotech" -> Icons.Default.Biotech
                                        else -> Icons.Default.HistoryEdu
                                    },
                                    contentDescription = null,
                                    tint = subjectColor
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = subject.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = stringResource(R.string.average_format, String.format(Locale.getDefault(), "%.2f", subject.averageGrade)), 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit)) },
                                        onClick = {
                                            showMenu = false
                                            onEditSubjectClick(subject.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_label)) },
                                        onClick = {
                                            showMenu = false
                                            subjectToDelete = Subject(
                                                id = subject.id,
                                                name = subject.name,
                                                academicYearId = 0, // Not needed for deletion
                                                iconName = subject.iconName,
                                                colorHex = subject.colorHex
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectsListScreen(
    viewModel: TeacherViewModel,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllClassesClick: () -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
) {
    val subjects by viewModel.subjectsWithStats.observeAsState(emptyList())
    SubjectsListContent(
        subjects = subjects,
        onMenuClick = onMenuClick,
        onSubjectClick = onSubjectClick,
        onViewAllClassesClick = onViewAllClassesClick,
        onAddSubjectClick = onAddSubjectClick,
        onEditSubjectClick = onEditSubjectClick,
        onDeleteSubjectClick = { viewModel.deleteSubject(it) }
    )
}

@Preview(showBackground = true)
@Composable
fun SubjectsListPreview() {
    TeachersLogTheme {
        SubjectsListContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.2),
                SubjectWithStats(2, "Science", "biotech", "#006686", 8, 3.8)
            ),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onViewAllClassesClick = {},
            onAddSubjectClick = {},
            onEditSubjectClick = {},
            onDeleteSubjectClick = {}
        )
    }
}
