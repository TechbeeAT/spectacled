package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable

/**
 * On Android/iOS/Desktop this renders [content] immediately - a no-op gate. On Web, KSafe's
 * AES key is a non-extractable WebCrypto CryptoKey and WebCrypto is async-only, so a
 * synchronous read of an encrypted value (e.g. UserAppPreferencesStore.claudeUserApiKey,
 * which reads through KSafe's getDirect) can race the key becoming available and silently
 * return the default instead of the stored value. This gates [content] on KSafe's own
 * documented awaitCacheReady() completing for every KSafe-backed store first.
 */
@Composable
expect fun SecureStorageReadyGate(content: @Composable () -> Unit)
