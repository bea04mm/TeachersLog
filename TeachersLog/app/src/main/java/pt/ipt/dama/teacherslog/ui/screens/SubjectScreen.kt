package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.ui.components.ClassCard
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

@Composable
fun SubjectContent(
    subjectName: String,
    classes: List<StudentsClassWithStats>,
    onClassClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TeacherTopBar(
                title = subjectName,
                showBackButton = true,
                onBackClick = onBackClick
            )
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

            items(classes) { clazz ->
                ClassCard(
                    name = clazz.name,
                    studentCount = clazz.studentCount,
                    averageGrade = clazz.averageGrade,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onClassClick(clazz.id, clazz.name) }
                )
            }
        }
    }
}

@Composable
fun SubjectScreen(
    viewModel: TeacherViewModel,
    subjectId: Int,
    subjectName: String,
    onClassClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    val classes by viewModel.getStudentsClassesForSubject(subjectId).observeAsState(emptyList())
    SubjectContent(
        subjectName = subjectName,
        classes = classes,
        onClassClick = onClassClick,
        onBackClick = onBackClick
    )
}
