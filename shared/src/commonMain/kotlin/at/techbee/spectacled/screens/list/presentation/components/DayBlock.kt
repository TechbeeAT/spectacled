package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.formatLocalized

@Composable
fun DayBlock(
    icsDateTime: IcsDateTime,
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(max = 48.dp)
    ) {
        Text(
            text = icsDateTime.formatLocalized(IcsDateTimeFormat.DAY_OF_WEEK_SHORT),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = icsDateTime.toLocalDateTime().day.toString(),
            fontWeight = FontWeight.Bold
        )
    }

}


@Composable
@Preview
private fun DayBlock_Preview() {
    DayBlock(icsDateTime = IcsDateTime.now())
}
