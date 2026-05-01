package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_entries_found_in_this_folder
import spectacled.shared.generated.resources.no_matching_entries_found
import spectacled.shared.generated.resources.undraw_dreamer

@Composable
fun EmptyListScreen(
    isEmptyFolder: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(Res.drawable.undraw_dreamer),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    //.height(150.dp)
                    .padding(48.dp)
                    .alpha(0.5f)
            )

            Text(
                text = stringResource(
                    if (isEmptyFolder)
                        Res.string.no_entries_found_in_this_folder
                    else
                        Res.string.no_matching_entries_found
                ),
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .alpha(0.5f)
                    .padding(top = 8.dp, bottom = 88.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Preview
@Composable
fun EmptyListScreen_empty_search_Preview() {
    EmptyListScreen(isEmptyFolder = false)
}

@Preview
@Composable
fun EmptyListScreen_empty_list_Preview() {
    EmptyListScreen(isEmptyFolder = true)
}


