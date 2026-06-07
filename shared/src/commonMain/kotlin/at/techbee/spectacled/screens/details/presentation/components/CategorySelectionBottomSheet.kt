package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.create_category
import spectacled.shared.generated.resources.search_add_category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionBottomSheet(
    allCategories: List<String>,
    selectedCategories: List<String>,
    onCategoryAdded: (String) -> Unit,
    onCategoryRemoved: (String) -> Unit,
    onDismiss: () -> Unit
    ) {

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {

            Text(
                text = stringResource(Res.string.category),
                style = MaterialTheme.typography.titleLarge
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                allCategories
                    .sortedBy { it.uppercase() }
                    .forEach { category ->
                        FilterChip(
                            selected = selectedCategories.contains(category),
                            onClick = {
                                if (selectedCategories.contains(category))
                                    onCategoryRemoved(category)
                                else
                                    onCategoryAdded(category)
                            },
                            label = { Text(category) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, stringResource(Res.string.category)) }
                        )
                    }
            }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(Res.string.search_add_category)) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                onCategoryAdded(searchQuery)
                                searchQuery = ""
                            }
                            keyboardController?.hide()
                        },
                        enabled = searchQuery.isNotBlank(),
                        content = { Icon(Icons.Outlined.NewLabel, stringResource(Res.string.create_category)) }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (searchQuery.isNotBlank()) {
                            onCategoryAdded(searchQuery)
                            searchQuery = ""
                        }
                    }
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CategorySelectionBottomSheet_Preview() {
    CategorySelectionBottomSheet(
        allCategories = listOf("Category 5", "Category 1", "Category 2", "Category 3", "Category 4"),
        selectedCategories = listOf("Category 2"),
        onCategoryAdded = { },
        onCategoryRemoved = { },
        onDismiss = { }
    )
}
