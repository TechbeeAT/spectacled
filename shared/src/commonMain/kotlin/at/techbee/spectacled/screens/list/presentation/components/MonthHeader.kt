package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun MonthHeader(
    icsDateTime: IcsDateTime,
    modifier: Modifier = Modifier
) {

    val formatter = PlatformInstantFormatter(icsDateTime)
    val monthName = formatter.formatFullMonthName()

    val monthText = if(icsDateTime.toLocalDateTime().year != IcsDateTime.now().toLocalDateTime().year)
        "$monthName ${icsDateTime.toLocalDateTime().year}"
    else
        monthName

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clipToBounds()
    ) {

        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-32).dp, y = (4).dp)
                .rotate(20f)
                .size(96.dp)
                .alpha(0.2f),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )

        Text(
            text = monthText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}



@Composable
@Preview
private fun MonthHeader_Preview() {
    MonthHeader(
        icsDateTime = IcsDateTime.now()
    )
}


@Composable
@Preview
private fun MonthHeader_withYear_Preview() {
    MonthHeader(
        icsDateTime = IcsDateTime.now().copy(instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds().plus(31556952000))),
    )
}
