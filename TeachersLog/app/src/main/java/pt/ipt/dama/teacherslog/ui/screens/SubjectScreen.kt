package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.StudentsClass
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.ui.components.ClassCard
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

/**
 * SubjectContent is the stateless UI component that displays the classes within a specific subject.
 *
 * It manages:
 * 1. The display of the subject name as a header.
 * 2. A scrollable list of classes using [LazyColumn].
 * 3. Individual class cards showing student count and average grade.
 * 4. Dialogs for creating, editing, and deleting classes.
 *
 * @param subjectName The name of the subject being displayed.
 * @param classes The list of [StudentsClassWithStats] associated with this subject.
 * @param onClassClick Callback when a class card is clicked.
 * @param onAddClass Callback to create a new class.
 * @param onUpdateClass Callback to update an existing class.
 * @param onDeleteClass Callback to delete a class.
 * @param onBackClick Callback for the back button navigation.
 */
@Composable
fun SubjectContent(
    subjectName: String,
    classes: List<StudentsClassWithStats>,
    onClassClick: (Int, String) -> Unit,
    onAddClass: (String) -> Unit,
    onUpdateClass: (StudentsClass) -> Unit,
    onDeleteClass: (StudentsClass) -> Unit,
    onBackClick: () -> Unit
) {
    val showAddDialog = remember { mutableStateOf(false) }
    val classToEdit = remember { mutableStateOf<StudentsClass?>(null) }
    val classToDelete = remember { mutableStateOf<StudentsClass?>(null) }

    // --- Add/Edit Dialog ---
    if (showAddDialog.value || classToEdit.value != null) {
        var name by remember { mutableStateOf(classToEdit.value?.name ?: "") }
        
        AlertDialog(
            onDismissRequest = { 
                showAddDialog.value = false
                classToEdit.value = null
            },
            title = { Text(stringResource(if (classToEdit.value == null) R.string.add_class else R.string.edit_class)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.class_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            if (classToEdit.value == null) {
                                onAddClass(name)
                            } else {
                                onUpdateClass(classToEdit.value!!.copy(name = name))
                            }
                            showAddDialog.value = false
                            classToEdit.value = null
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog.value = false
                    classToEdit.value = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // --- Delete Confirmation Dialog ---
    if (classToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { classToDelete.value = null },
            title = { Text(stringResource(R.string.delete_class_title)) },
            text = { Text(stringResource(R.string.delete_class_message, classToDelete.value?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        classToDelete.value?.let { onDeleteClass(it) }
                        classToDelete.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete.value = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // Standard top bar with the subject name as title and a back button
            TeacherTopBar(
                title = subjectName,
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_class))
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
            // --- Header Section ---
            item {
                Text(
                    text = stringResource(R.string.class_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Classes List ---
            items(classes) { clazz ->
                ClassCard(
                    name = clazz.name,
                    studentCount = clazz.studentCount,
                    averageGrade = clazz.averageGrade,
                    color = MaterialTheme.colorScheme.primary,
                    gradingScale = clazz.gradingScale,
                    onClick = { onClassClick(clazz.id, clazz.name) },
                    actions = {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .clickable { showMenu = true }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_class)) },
                                    onClick = {
                                        showMenu = false
                                        classToEdit.value = StudentsClass(id = clazz.id, subjectId = clazz.subjectId, name = clazz.name)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_class_title)) },
                                    onClick = {
                                        showMenu = false
                                        classToDelete.value = StudentsClass(id = clazz.id, subjectId = clazz.subjectId, name = clazz.name)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * SubjectScreen is the stateful Composable that connects the [TeacherViewModel]
 * to the [SubjectContent] UI.
 *
 * @param viewModel The shared [TeacherViewModel] providing class data.
 * @param subjectId The ID of the subject whose classes should be fetched.
 * @param subjectName The name of the subject to display.
 * @param onClassClick Callback when a class is selected.
 * @param onBackClick Callback for navigating back to the previous screen.
 */
@Composable
fun SubjectScreen(
    viewModel: TeacherViewModel,
    subjectId: Int,
    subjectName: String,
    onClassClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    // Observe the list of classes for the specific subject from the ViewModel
    val classes by viewModel.getStudentsClassesForSubject(subjectId).observeAsState(emptyList())
    
    SubjectContent(
        subjectName = subjectName,
        classes = classes,
        onClassClick = onClassClick,
        onAddClass = { name -> viewModel.addStudentsClass(name, subjectId) },
        onUpdateClass = { viewModel.updateStudentsClass(it) },
        onDeleteClass = { viewModel.deleteStudentsClass(it) },
        onBackClick = onBackClick
    )
}

/**
 * Preview for the [SubjectContent] UI with sample class data in light mode.
 */
@Preview(showBackground = true, name = "Subject Detail Light Mode")
@Composable
fun SubjectPreview() {
    TeachersLogTheme(darkTheme = false) {
        SubjectContent(
            subjectName = "Mathematics",
            classes = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 4.2, 25, "5"),
                StudentsClassWithStats(2, 1, "10.ºB", 3.8, 22, "20"),
                StudentsClassWithStats(3, 1, "11.ºA", 4.5, 20, "100")
            ),
            onClassClick = { _, _ -> },
            onAddClass = { _ -> },
            onUpdateClass = {},
            onDeleteClass = {},
            onBackClick = {}
        )
    }
}

/**
 * Dark mode preview for the [SubjectContent] UI.
 */
@Preview(showBackground = true, name = "Subject Detail Dark Mode")
@Composable
fun SubjectDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        SubjectContent(
            subjectName = "Natural Sciences",
            classes = listOf(
                StudentsClassWithStats(4, 2, "8.ºC", 4.0, 18, "5"),
                StudentsClassWithStats(5, 2, "9.ºA", 3.5, 24, "20")
            ),
            onClassClick = { _, _ -> },
            onAddClass = { _ -> },
            onUpdateClass = {},
            onDeleteClass = {},
            onBackClick = {}
        )
    }
}
