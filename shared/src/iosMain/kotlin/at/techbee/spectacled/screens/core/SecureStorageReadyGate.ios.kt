package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable

@Composable
actual fun SecureStorageReadyGate(content: @Composable () -> Unit) = content()
