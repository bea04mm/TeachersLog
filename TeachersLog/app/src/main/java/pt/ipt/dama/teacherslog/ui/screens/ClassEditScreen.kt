package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.GradingComponent
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import java.util.UUID

/**
 * UI State for a single grading component during class creation/editing.
 * Includes a temporary UI ID to manage relationships before database persistence.
 * 
 * @property uiId Unique temporary identifier for the UI list.
 * @property id Database identifier (0 for new ones).
 * @property parentUiId Temporary ID of the parent group.
 * @property name Human-readable name.
 * @property weight Weight percentage string.
 */
data class ClassComponentEditState(
    val uiId: String = UUID.randomUUID().toString(),
    val id: Int = 0,
    val parentUiId: String? = null,
    val name: String = "",
    val weight: String = ""
)

/**
 * ClassEditContent is the stateless UI for managing a student class.
 *
 * @param isEditMode Flag for editing existing vs creating new.
 * @param initialName Starting name.
 * @param initialScale Starting scale.
 * @param initialComponents Starting criteria list with hierarchy support.
 * @param onSave Callback for persisting data with parent mappings.
 * @param onBackClick Callback for navigation.
 */
@Composable
fun ClassEditContent(
    isEditMode: Boolean,
    initialName: String,
    initialScale: String,
    initialComponents: List<ClassComponentEditState>,
    onSave: (String, String, List<GradingComponent>, Map<Int, Int>) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedScale by remember { mutableStateOf(initialScale) }
    val components = remember { mutableStateListOf<ClassComponentEditState>() }

    // Sync internal state when data becomes available
    LaunchedEffect(initialName, initialScale, initialComponents) {
        if (name.isEmpty()) name = initialName
        if (selectedScale == "5") selectedScale = initialScale
        if (components.isEmpty() && initialComponents.isNotEmpty()) {
            components.addAll(initialComponents)
        } else if (components.isEmpty() && !isEditMode) {
            // Default setup for a new class
            components.add(ClassComponentEditState(name = "Tests", weight = "60"))
            components.add(ClassComponentEditState(name = "Assignments", weight = "40"))
        }
    }

    // Validation logic for weights
    val rootWeight = components.filter { it.parentUiId == null }.sumOf { it.weight.toIntOrNull() ?: 0 }
    val isRootValid = rootWeight == 100

    Scaffold(
        topBar = {
            TeacherTopBar(
                title = stringResource(if (isEditMode) R.string.edit_class else R.string.add_class),
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Class Identity ---
            item {
                Text(text = stringResource(R.string.class_identity_header), style = AppTheme.typography.labelSmall, color = AppTheme.colors.primary)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.class_name)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
            }

            // --- Scale Selection ---
            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.grading_scale), style = AppTheme.typography.labelMedium, color = AppTheme.colors.primary)
                        listOf("5" to R.string.scale_5, "20" to R.string.scale_20, "100" to R.string.scale_100).forEach { (scale, labelRes) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedScale = scale }.padding(vertical = 4.dp)) {
                                RadioButton(selected = (selectedScale == scale), onClick = { selectedScale = scale })
                                Text(text = stringResource(labelRes), style = AppTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }

            // --- Hierarchical Components Section ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(text = stringResource(R.string.grading_components), style = AppTheme.typography.labelSmall, color = AppTheme.colors.primary)
                    Text(
                        text = "${rootWeight}%", 
                        color = if (isRootValid) AppTheme.success else AppTheme.critical,
                        style = AppTheme.typography.titleLarge
                    )
                }
            }

            // Rendering Logic supporting 1-level nesting
            val rootComponents = components.filter { it.parentUiId == null }
            rootComponents.forEach { root ->
                item(key = root.uiId) {
                    val children = components.filter { it.parentUiId == root.uiId }
                    val childrenWeight = children.sumOf { it.weight.toIntOrNull() ?: 0 }
                    val isGroupValid = children.isEmpty() || childrenWeight == 100

                    ComponentRow(
                        state = root,
                        isSubItem = false,
                        isValid = isGroupValid,
                        currentChildSum = if (children.isNotEmpty()) childrenWeight else null,
                        onUpdate = { updated -> 
                            val idx = components.indexOfFirst { it.uiId == root.uiId }
                            if (idx != -1) components[idx] = updated
                        },
                        onRemove = { components.removeAll { it.uiId == root.uiId || it.parentUiId == root.uiId } },
                        onAddSub = { components.add(ClassComponentEditState(parentUiId = root.uiId)) }
                    )
                }

                // Render children rows
                val children = components.filter { it.parentUiId == root.uiId }
                items(children, key = { it.uiId }) { child ->
                    Box(modifier = Modifier.padding(start = 32.dp)) {
                        ComponentRow(
                            state = child, isSubItem = true, isValid = true,
                            onUpdate = { updated ->
                                val idx = components.indexOfFirst { it.uiId == child.uiId }
                                if (idx != -1) components[idx] = updated
                            },
                            onRemove = { components.removeIf { it.uiId == child.uiId } },
                            onAddSub = {} 
                        )
                    }
                }
            }

            item {
                TextButton(onClick = { components.add(ClassComponentEditState()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.add_component))
                }
            }

            // --- Persist Changes ---
            item {
                val allGroupsValid = rootComponents.all { root ->
                    val children = components.filter { it.parentUiId == root.uiId }
                    children.isEmpty() || children.sumOf { it.weight.toIntOrNull() ?: 0 } == 100
                }

                Button(
                    onClick = {
                        val sortedList = mutableListOf<ClassComponentEditState>()
                        rootComponents.forEach { r ->
                            sortedList.add(r)
                            sortedList.addAll(components.filter { it.parentUiId == r.uiId })
                        }
                        val finalEntities = sortedList.map { GradingComponent(id = it.id, name = it.name, weightPercentage = it.weight.toIntOrNull() ?: 0, classId = 0) }
                        val parentMap = mutableMapOf<Int, Int>()
                        sortedList.forEachIndexed { index, state ->
                            if (state.parentUiId != null) {
                                val parentIdx = sortedList.indexOfFirst { it.uiId == state.parentUiId }
                                if (parentIdx != -1) parentMap[index] = parentIdx
                            }
                        }
                        onSave(name, selectedScale, finalEntities, parentMap)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = name.isNotBlank() && isRootValid && allGroupsValid && components.isNotEmpty()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(stringResource(R.string.save_class))
                }
            }
        }
    }
}

/**
 * Reusable row for an individual grading component or group.
 */
@Composable
fun ComponentRow(
    state: ClassComponentEditState,
    isSubItem: Boolean,
    isValid: Boolean,
    currentChildSum: Int? = null,
    onUpdate: (ClassComponentEditState) -> Unit,
    onRemove: () -> Unit,
    onAddSub: () -> Unit
) {
    TonalCard(
        modifier = Modifier.border(
            width = if (!isValid) 2.dp else 0.dp,
            color = if (!isValid) AppTheme.critical else AppTheme.transparent,
            shape = AppTheme.shapes.medium
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isSubItem) Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = AppTheme.colors.outline)
                OutlinedTextField(value = state.name, onValueChange = { onUpdate(state.copy(name = it)) }, placeholder = { Text(if (isSubItem) "Sub-item Name" else "Group Name") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = state.weight, onValueChange = { if (it.all { c -> c.isDigit() }) onUpdate(state.copy(weight = it)) }, placeholder = { Text("%") }, modifier = Modifier.width(70.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = null, tint = AppTheme.colors.error) }
            }
            if (!isSubItem) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onAddSub) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Add Sub-item", style = AppTheme.typography.labelMedium)
                    }
                    if (currentChildSum != null) {
                        Text(
                            text = "Sub-total: ${currentChildSum}%", 
                            color = if (isValid) AppTheme.success else AppTheme.critical,
                            style = AppTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * ClassEditScreen stateful container.
 * 
 * @param viewModel Shared teacher logic.
 * @param subjectId Parent subject context.
 * @param classId Optional class identifier for editing.
 * @param onBackClick Navigation callback.
 */
@Composable
fun ClassEditScreen(
    viewModel: TeacherViewModel,
    subjectId: Int,
    classId: Int?,
    onBackClick: () -> Unit
) {
    val existingClass by if (classId != null) viewModel.getStudentsClassById(classId).observeAsState() else remember { mutableStateOf(null) }
    val existingComponents by if (classId != null) viewModel.getGradingComponentsForClass(classId).observeAsState(emptyList()) else remember { mutableStateOf(emptyList()) }

    val initialUIComponents = remember(existingComponents) {
        val mapping = mutableMapOf<Int, String>()
        existingComponents.forEach { mapping[it.id] = UUID.randomUUID().toString() }
        existingComponents.map { ClassComponentEditState(uiId = mapping[it.id]!!, id = it.id, parentUiId = mapping[it.parentId], name = it.name, weight = it.weightPercentage.toString()) }
    }

    ClassEditContent(
        isEditMode = classId != null, initialName = existingClass?.name ?: "", initialScale = existingClass?.gradingScale ?: "5",
        initialComponents = initialUIComponents,
        onSave = { name, scale, entities, parentMap ->
            viewModel.upsertClassWithHierarchy(classId, subjectId, name, scale, entities, parentMap)
            onBackClick()
        },
        onBackClick = onBackClick
    )
}
