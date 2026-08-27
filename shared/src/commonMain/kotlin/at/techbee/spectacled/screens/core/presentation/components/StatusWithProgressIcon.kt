package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.theme.AppTheme

@Composable
fun StatusWithProgressIcon(
    status: Status,
    percent: Long? = null,
) {

    val text = when(status) {
        Status.NEEDS_ACTION -> "0"
        Status.IN_PROCESS -> (percent?:1L).toString()
        Status.COMPLETED -> "100"
        else -> null
    }

    val icon = when(status) {
        Status.FINAL -> Icons.Outlined.Check
        Status.DRAFT -> Icons.Outlined.QuestionMark
        Status.CANCELLED -> Icons.Outlined.Close
        else -> null
    }

    val progress = when(status) {
        Status.FINAL, Status.COMPLETED -> 1f
        Status.DRAFT -> 0.33f
        Status.IN_PROCESS  -> (percent?:1L)/100f
        Status.NEEDS_ACTION, Status.CANCELLED -> 0f
    }

    Box(contentAlignment = Alignment.Center) {

        text?.let {
            Text(
                text = text,
                color = ProgressIndicatorDefaults.circularColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }

        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = ProgressIndicatorDefaults.circularColor,
                modifier = Modifier.size(14.dp)
            )
        }


        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
@Preview
private fun StatusWithProgress_Icon_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusWithProgressIcon(status = Status.FINAL)
            StatusWithProgressIcon(status = Status.DRAFT)
            StatusWithProgressIcon(status = Status.CANCELLED)

            StatusWithProgressIcon(status = Status.NEEDS_ACTION)
            StatusWithProgressIcon(status = Status.IN_PROCESS, 33)
            StatusWithProgressIcon(status = Status.COMPLETED, 100)
        }

    }
}