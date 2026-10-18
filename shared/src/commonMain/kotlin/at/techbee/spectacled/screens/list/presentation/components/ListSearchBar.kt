package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.list.presentation.ListAction
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSearchBar(
    listFilterCriteria: ListFilterCriteria,
    onAction: (ListAction) -> Unit,
    modifier: Modifier = Modifier,
    searchBarFocusRequester: FocusRequester = remember { FocusRequester() }
) {

    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {

        TextField(
            placeholder = { Text(stringResource(Res.string.search)) },
            value = listFilterCriteria.searchQuery ?: "",
            onValueChange = { onAction(ListAction.OnListFilterCriteriaChanged(listFilterCriteria.copy(searchQuery = it))) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Filter,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .focusRequester(searchBarFocusRequester)
        )
    }
}

@Preview
@Composable
fun ListSearchBar_Preview() {

    ListSearchBar(
        listFilterCriteria = ListFilterCriteria(),
        onAction = { }
    )
}
