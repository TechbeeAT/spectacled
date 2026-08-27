package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun CustomBottomSnackbarHost(
    snackbarHostState: SnackbarHostState,
) {
    SnackbarHost(snackbarHostState) {
        Snackbar(
            modifier = Modifier.padding(bottom = 112.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(it.visuals.message)
        }
    }
}



@Preview
@Composable
private fun CustomBottomSnackbarHost_Preview() {
    val snackbarHostState = remember { SnackbarHostState() }

    MaterialTheme {
        CustomBottomSnackbarHost(
            snackbarHostState = snackbarHostState
        )
    }
    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar("This is a snackbar")
    }
}
