package at.techbee.spectacled.screens.about.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.about.data.KtorRemoteGitHubContributorDataSource
import at.techbee.spectacled.screens.about.data.KtorRemoteGitHubReleaseDataSource
import com.mikepenz.aboutlibraries.Libs
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import spectacled.shared.generated.resources.Res


class AboutViewModel(
    private val client: HttpClient,
    val spectacledVariant: SpectacledVariant,
): ViewModel() {

    private val _state = MutableStateFlow(AboutState())
    val state = _state.asStateFlow()

    init {
        // The GitHub data sources self-dispatch their network now, but the AboutLibraries step
        // still reads and parses libraries.json (CPU) inline here, so keep this launch on IO.
        viewModelScope.launch(ioDispatcher) {

            launch {
                KtorRemoteGitHubContributorDataSource(client).getContributors().let { contributors ->
                    _state.update { it.copy(gitHubContributors = contributors) }
                }
            }

            launch {
                KtorRemoteGitHubReleaseDataSource(client).getReleases().let { releases ->
                    _state.update { it.copy(gitHubReleases = releases) }
                }
            }

            launch {
                Libs.Builder()
                    .withJson(Res.readBytes("files/libraries.json").decodeToString())
                    .build().let { libraries ->
                        _state.update { it.copy(libraries = libraries) }
                    }
            }
        }
    }
}
