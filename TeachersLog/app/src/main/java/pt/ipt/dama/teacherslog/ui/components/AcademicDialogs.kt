package pt.ipt.dama.teacherslog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.AcademicYear

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
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.primary,
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
                }) { Icon(Icons.Default.Remove, contentDescription = "Decrease year") }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer, 
                    shape = RoundedCornerShape(8.dp), 
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "$selectedStartYear / ${selectedStartYear + 1}", 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = { 
                    var y = selectedStartYear + 1
                    while (y in takenStartYears) y++
                    onYearChange(y) 
                }) { Icon(Icons.Default.Add, contentDescription = "Increase year") }
            }
        }

        // --- Period Selection Section ---
        Column {
            Text(
                text = stringResource(R.string.select_period_type), 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.primary,
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
                    Text(text = stringResource(optionRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
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
 */
@Composable
fun AddYearAndPeriodDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    existingYears: List<AcademicYear>
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
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_new_year)) },
        text = {
            AddYearAndPeriodContent(
                selectedStartYear = selectedStartYear,
                takenStartYears = takenStartYears,
                periodOptions = periodOptions,
                selectedPeriodOption = selectedPeriodOption,
                onYearChange = { selectedStartYear = it },
                onPeriodChange = { selectedPeriodOption = it }
            )
        },
        confirmButton = { 
            Button(onClick = { onAdd("$selectedStartYear/${selectedStartYear + 1}", context.getString(selectedPeriodOption)) }) {
                Text(stringResource(R.string.save)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text(stringResource(R.string.cancel)) 
            } 
        }
    )
}
