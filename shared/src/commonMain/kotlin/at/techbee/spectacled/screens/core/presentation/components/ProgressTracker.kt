package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ProgressTracker() {

    Box(
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator()

        Icon(Icons.Outlined.Check, null)


    }
}

@Preview
@Composable
fun ProgressTracker_Preview() {
    ProgressTracker()
}