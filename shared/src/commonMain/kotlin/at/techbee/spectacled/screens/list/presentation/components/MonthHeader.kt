package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.formatLocalized
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun MonthHeader(
    icsDateTime: IcsDateTime,
    modifier: Modifier = Modifier
) {

    val monthName = icsDateTime.formatLocalized(IcsDateTimeFormat.FULL_MONTH_NAME)

    val monthText = if(icsDateTime.toLocalDateTime().year != IcsDateTime.now().toLocalDateTime().year)
        "$monthName ${icsDateTime.toLocalDateTime().year}"
    else
        monthName

    ElevatedCard(
        shape = RectangleShape,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clipToBounds()
        ) {

            Icon(
                painter = painterResource(SpectacledVariant.JOURNALS.logoDrawableResource),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = (-36).dp, y = (16).dp)
                    .rotate(20f)
                    .size(96.dp)
                    .alpha(0.2f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = monthText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
