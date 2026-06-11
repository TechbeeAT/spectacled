package at.techbee.spectacled.screens.about.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.about.data.KtorRemoteGitHubContributorDataSource
import at.techbee.spectacled.screens.about.data.KtorRemoteGitHubReleaseDataSource
import com.mikepenz.aboutlibraries.Libs
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import spectacled.shared.generated.resources.Res


class AboutViewModel(
    private val client: HttpClient,
    val spectacledVariant: SpectacledVariant,
): ViewModel() {

    private val _state = mutableStateOf(AboutState())
    val state by _state

    init {
        viewModelScope.launch {

            launch {
                KtorRemoteGitHubContributorDataSource(client).getContributors().let {
                    _state.value = state.copy(gitHubContributors = it)
                }
            }

            launch {
                KtorRemoteGitHubReleaseDataSource(client).getReleases().let {
                    _state.value = state.copy(gitHubReleases = it)
                }
            }

            launch {
                Libs.Builder()
                    .withJson(Res.readBytes("files/libraries.json").decodeToString())
                    .build().let {
                        _state.value = state.copy(libraries = it)
                    }
            }
        }
    }
}