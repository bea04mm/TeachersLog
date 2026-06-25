package pt.ipt.dama.teacherslog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LiveData
import java.util.Calendar
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.Semester
import pt.ipt.dama.teacherslog.data.model.Subject
import pt.ipt.dama.teacherslog.data.model.StudentsClass
import pt.ipt.dama.teacherslog.ui.theme.*

/**
 * Stateless content for the Year and Period creation dialog.
 * Extracted for better preview support and logic separation.
 *
 * @param selectedStartYear The current selected starting year.
 * @param takenStartYears Set of years that cannot be selected.
 * @param periodOptions List of string resource IDs for periods.
 * @param selectedPeriodOption The current selected period resource ID.
 * @param onYearChange Callback when year is incremented/decremented.
 * @param onPeriodChange Callback when a period is selected.
 */
@Composable
fun AddYearAndPeriodContent(
    selectedStartYear: Int,
    takenStartYears: Set<Int>,
    periodOptions: List<Int>,
    selectedPeriodOption: Int,
    onYearChange: (Int) -> Unit,
    onPeriodChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- Year Selection Section ---
        Column {
            Text(
                text = stringResource(R.string.select_starting_year), 
                style = AppTheme.typography.labelMedium, 
                color = AppTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.Center, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    var y = selectedStartYear - 1
                    while (y in takenStartYears && y > 2000) y--
                    if (y !in takenStartYears) onYearChange(y) 
                }) { Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease_year)) }
                
                Surface(
                    color = AppTheme.colors.primaryContainer, 
                    shape = AppTheme.shapes.small,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "$selectedStartYear / ${selectedStartYear + 1}", 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                        style = AppTheme.typography.headlineSmall
                    )
                }
                
                IconButton(onClick = { 
                    var y = selectedStartYear + 1
                    while (y in takenStartYears) y++
                    onYearChange(y) 
                }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase_year)) }
            }
        }

        // --- Period Selection Section ---
        Column {
            Text(
                text = stringResource(R.string.select_period_type), 
                style = AppTheme.typography.labelMedium, 
                color = AppTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            periodOptions.forEach { optionRes ->
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPeriodChange(optionRes) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = (selectedPeriodOption == optionRes), onClick = { onPeriodChange(optionRes) })
                    Text(text = stringResource(optionRes), style = AppTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/**
 * Dialog for creating a new academic year and its first period simultaneously.
 *
 * @param onDismiss Callback executed when the dialog is canceled.
 * @param onAdd Callback triggered when the "Save" button is pressed with valid data.
 * @param existingYears List of existing years used to prevent duplicate start years.
 * @param isMandatory If true, the dialog cannot be dismissed without adding data.
 */
@Composable
fun AddYearAndPeriodDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    existingYears: List<AcademicYear>,
    isMandatory: Boolean = false
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    // Extract registered start years to calculate the next logical available year
    val takenStartYears = existingYears.mapNotNull { it.year.split("/").firstOrNull()?.toIntOrNull() }.toSet()
    
    var initialYear = currentYear
    while (initialYear in takenStartYears) initialYear++
    var selectedStartYear by remember { mutableIntStateOf(initialYear) }
    
    // Standard periods recognized by the application
    val periodOptions = listOf(
        R.string.period_1st_semester, R.string.period_2nd_semester,
        R.string.period_1st_period, R.string.period_2nd_period,
        R.string.period_3rd_period, R.string.period_annual
    )
    var selectedPeriodOption by remember { mutableIntStateOf(periodOptions[0]) }

    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!isMandatory) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        ),
        title = { Text(stringResource(R.string.add_new_year)) },
        text = {
            Column {
                if (isMandatory) {
                    Text(
                        text = "Welcome! To start, please create your first academic year and period.",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                AddYearAndPeriodContent(
                    selectedStartYear = selectedStartYear,
                    takenStartYears = takenStartYears,
                    periodOptions = periodOptions,
                    selectedPeriodOption = selectedPeriodOption,
                    onYearChange = { selectedStartYear = it },
                    onPeriodChange = { selectedPeriodOption = it }
                )
            }
        },
        confirmButton = { 
            Button(onClick = { onAdd("$selectedStartYear/${selectedStartYear + 1}", context.getString(selectedPeriodOption)) }) {
                Text(stringResource(R.string.save)) 
            } 
        },
        dismissButton = { 
            if (!isMandatory) {
                TextButton(onClick = onDismiss) { 
                    Text(stringResource(R.string.cancel)) 
                }
            }
        }
    )
}

/**
 * Dialog for selecting a destination academic period for cloning.
 */
@Composable
fun CloneDestinationDialog(
    title: String,
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYearId by remember { mutableIntStateOf(academicYears.firstOrNull()?.id ?: 0) }
    val semesters by semestersProvider(selectedYearId).observeAsState(emptyList())
    var selectedSemesterId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.clone_destination_label), style = AppTheme.typography.labelMedium)
                
                // Year Selection (Simple dropdown or row)
                ScrollableTabRow(
                    selectedTabIndex = academicYears.indexOfFirst { it.id == selectedYearId }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    academicYears.forEach { year ->
                        Tab(
                            selected = year.id == selectedYearId,
                            onClick = { selectedYearId = year.id; selectedSemesterId = null },
                            text = { Text(year.year) }
                        )
                    }
                }

                // Semester Selection
                Column {
                    semesters.forEach { sem ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSemesterId = sem.id }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = selectedSemesterId == sem.id, onClick = { selectedSemesterId = sem.id })
                            Text(sem.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedSemesterId?.let { onConfirm(it) } },
                enabled = selectedSemesterId != null
            ) {
                Text(stringResource(R.string.clone_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for selecting a destination subject for cloning a class.
 */
@Composable
fun CloneClassDestinationDialog(
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>,
    subjectsProvider: (Int) -> LiveData<List<Subject>>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYearId by remember { mutableIntStateOf(academicYears.firstOrNull()?.id ?: 0) }
    val semesters by semestersProvider(selectedYearId).observeAsState(emptyList())
    var selectedSemesterId by remember { mutableStateOf<Int?>(null) }
    val subjects by if (selectedSemesterId != null) {
        subjectsProvider(selectedSemesterId!!).observeAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }

    // Auto-select first semester when the list loads or year changes
    LaunchedEffect(semesters) {
        if (selectedSemesterId == null || semesters.none { it.id == selectedSemesterId }) {
            selectedSemesterId = semesters.firstOrNull()?.id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clone_class_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.clone_destination_class_label), style = AppTheme.typography.labelMedium)

                // Year Tabs
                ScrollableTabRow(
                    selectedTabIndex = academicYears.indexOfFirst { it.id == selectedYearId }.coerceAtLeast(0),
                    edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}
                ) {
                    academicYears.forEach { year ->
                        Tab(selected = year.id == selectedYearId, onClick = { selectedYearId = year.id; selectedSemesterId = null; selectedSubjectId = null }, text = { Text(year.year) })
                    }
                }

                // Semester Tabs (Smaller)
                if (semesters.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = semesters.indexOfFirst { it.id == selectedSemesterId }.coerceAtLeast(0),
                        edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}
                    ) {
                        semesters.forEach { sem ->
                            Tab(selected = sem.id == selectedSemesterId, onClick = { selectedSemesterId = sem.id; selectedSubjectId = null }, text = { Text(sem.name, style = AppTheme.typography.bodySmall) })
                        }
                    }
                }

                // Subject List
                if (selectedSemesterId != null) {
                    Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        subjects.forEach { subject ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedSubjectId = subject.id }.padding(vertical = 8.dp)
                            ) {
                                RadioButton(selected = selectedSubjectId == subject.id, onClick = { selectedSubjectId = subject.id })
                                Text(subject.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selectedSubjectId?.let { onConfirm(it) } }, enabled = selectedSubjectId != null) { Text(stringResource(R.string.clone_label)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Dialog for cloning an academic year.
 */
@Composable
fun CloneYearDialog(
    sourceYear: AcademicYear,
    existingYears: List<AcademicYear>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val takenStartYears = existingYears.mapNotNull { it.year.split("/").firstOrNull()?.toIntOrNull() }.toSet()
    
    val currentStartYear = sourceYear.year.split("/").firstOrNull()?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    var selectedStartYear by remember { mutableIntStateOf(currentStartYear + 1) }
    while (selectedStartYear in takenStartYears) selectedStartYear++

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clone_item_title, sourceYear.year)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.select_starting_year), style = AppTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.Center, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        var y = selectedStartYear - 1
                        while (y in takenStartYears && y > 2000) y--
                        if (y !in takenStartYears) selectedStartYear = y 
                    }) { Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease_year)) }
                    
                    Surface(
                        color = AppTheme.colors.primaryContainer, 
                        shape = AppTheme.shapes.small,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "$selectedStartYear / ${selectedStartYear + 1}", 
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                            style = AppTheme.typography.headlineSmall
                        )
                    }
                    
                    IconButton(onClick = { 
                        var y = selectedStartYear + 1
                        while (y in takenStartYears) y++
                        selectedStartYear = y 
                    }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase_year)) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm("$selectedStartYear/${selectedStartYear + 1}") }) {
                Text(stringResource(R.string.clone_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for selecting a destination academic year and giving it a name for cloning a semester.
 */
@Composable
fun CloneSemesterDestinationDialog(
    title: String,
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYearId by remember { mutableIntStateOf(academicYears.firstOrNull()?.id ?: 0) }
    val existingSemesters by semestersProvider(selectedYearId).observeAsState(emptyList())

    // Initial name logic
    val baseName = title.substringAfter(stringResource(R.string.clone_label)).trim()
    var newName by remember { mutableStateOf(baseName) }

    val context = LocalContext.current
    val periodOptions = listOf(
        R.string.period_1st_semester, R.string.period_2nd_semester,
        R.string.period_1st_period, R.string.period_2nd_period,
        R.string.period_3rd_period, R.string.period_annual
    )

    // Filter out options that already exist in the target year
    val availableOptions = remember(periodOptions, existingSemesters) {
        periodOptions.filter { resId ->
            val pName = context.getString(resId)
            existingSemesters.none { it.name.equals(pName, ignoreCase = true) }
        }
    }

    // Auto-select first available option if current choice becomes invalid
    LaunchedEffect(availableOptions) {
        if (availableOptions.isNotEmpty()) {
            val currentNameValid = availableOptions.any { context.getString(it) == newName }
            if (!currentNameValid) {
                newName = context.getString(availableOptions.first())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Rename Section
                Column {
                    Text(text = stringResource(R.string.target_period_name_label), style = AppTheme.typography.labelMedium, color = AppTheme.colors.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show only available common names
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        availableOptions.forEach { resId ->
                            val pName = stringResource(resId)
                            FilterChip(
                                selected = newName == pName,
                                onClick = { newName = pName },
                                label = { Text(pName) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 2. Year Selection
                Column {
                    Text(stringResource(R.string.select_starting_year), style = AppTheme.typography.labelMedium, color = AppTheme.colors.primary)
                    Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        academicYears.forEach { year ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedYearId = year.id }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(selected = selectedYearId == year.id, onClick = { selectedYearId = year.id })
                                Text(year.year, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedYearId, newName) }, 
                enabled = newName.isNotBlank() && availableOptions.any { context.getString(it) == newName }
            ) {
                Text(stringResource(R.string.clone_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for selecting a destination class for cloning a student.
 */
@Composable
fun CloneStudentDestinationDialog(
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>,
    subjectsProvider: (Int) -> LiveData<List<Subject>>,
    classesProvider: (Int) -> LiveData<List<StudentsClass>>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYearId by remember { mutableIntStateOf(academicYears.firstOrNull()?.id ?: 0) }
    val semesters by semestersProvider(selectedYearId).observeAsState(emptyList())
    var selectedSemesterId by remember { mutableStateOf<Int?>(null) }
    val subjects by if (selectedSemesterId != null) {
        subjectsProvider(selectedSemesterId!!).observeAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    val classes by if (selectedSubjectId != null) {
        classesProvider(selectedSubjectId!!).observeAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }

    // Auto-select first semester when the list loads or year changes
    LaunchedEffect(semesters) {
        if (selectedSemesterId == null || semesters.none { it.id == selectedSemesterId }) {
            selectedSemesterId = semesters.firstOrNull()?.id
        }
    }

    // Auto-select first subject when the semester changes
    LaunchedEffect(subjects) {
        if (selectedSubjectId == null || subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = subjects.firstOrNull()?.id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clone_student_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.class_header), style = AppTheme.typography.labelMedium)

                // Year Tabs
                ScrollableTabRow(
                    selectedTabIndex = academicYears.indexOfFirst { it.id == selectedYearId }.coerceAtLeast(0),
                    edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}
                ) {
                    academicYears.forEach { year ->
                        Tab(selected = year.id == selectedYearId, onClick = { selectedYearId = year.id; selectedSemesterId = null; selectedSubjectId = null; selectedClassId = null }, text = { Text(year.year) })
                    }
                }

                // Semester Tabs
                if (semesters.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = semesters.indexOfFirst { it.id == selectedSemesterId }.coerceAtLeast(0),
                        edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}
                    ) {
                        semesters.forEach { sem ->
                            Tab(selected = sem.id == selectedSemesterId, onClick = { selectedSemesterId = sem.id; selectedSubjectId = null; selectedClassId = null }, text = { Text(sem.name, style = AppTheme.typography.bodySmall) })
                        }
                    }
                }

                // Subject Tabs
                if (subjects.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = subjects.indexOfFirst { it.id == selectedSubjectId }.coerceAtLeast(0),
                        edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}
                    ) {
                        subjects.forEach { sub ->
                            Tab(selected = sub.id == selectedSubjectId, onClick = { selectedSubjectId = sub.id; selectedClassId = null }, text = { Text(sub.name, style = AppTheme.typography.bodySmall) })
                        }
                    }
                }

                // Class List
                if (selectedSubjectId != null) {
                    Column(modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                        classes.forEach { clazz ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedClassId = clazz.id }.padding(vertical = 8.dp)
                            ) {
                                RadioButton(selected = selectedClassId == clazz.id, onClick = { selectedClassId = clazz.id })
                                Text(clazz.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selectedClassId?.let { onConfirm(it) } }, enabled = selectedClassId != null) { Text(stringResource(R.string.clone_label)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
