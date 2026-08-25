package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.SplashScreen
import at.techbee.spectacled.shared.BuildKonfig
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about_codename
import spectacled.shared.generated.resources.about_nlnet_thanks
import spectacled.shared.generated.resources.about_version
import spectacled.shared.generated.resources.copyright_info
import spectacled.shared.generated.resources.logo_nlnet
import spectacled.shared.generated.resources.logo_techbee_xml
import spectacled.shared.generated.resources.terms_conditions

@Composable
fun AboutApp(
    spectacledVariant: SpectacledVariant
) {

    val uriHandler = LocalUriHandler.current

    SelectionContainer {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SplashScreen(
                spectacledVariant = spectacledVariant,
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = stringResource(spectacledVariant.appNameStringRes),
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    Res.string.about_version,
                    BuildKonfig.APP_VERSION_STRING,
                    BuildKonfig.APP_BUILD_NUMBER
                ),
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = stringResource(Res.string.about_codename, BuildKonfig.APP_VERSION_CODENAME),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            TextButton(
                content = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.terms_conditions),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew, 
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                            )
                    }
                },
                onClick = {
                    try {
                        uriHandler.openUri("https://www.techbee.at/")   // TODO: Update URL
                    } catch (e: Exception) {
                        Napier.w(e.stackTraceToString())
                    }
                }
            )
            Text(
                text = stringResource(Res.string.copyright_info),
                style = MaterialTheme.typography.bodyLarge
            )

            ElevatedCard(
                onClick = {
                    /*
                    clickCount += 1
                    if(clickCount >= 5) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            flags += Intent.FLAG_ACTIVITY_NEW_TASK
                            data = "https://ko-fi.com/jtxboard".toUri()
                        }
                        context.startActivity(intent)
                    }

                     */
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo_techbee_xml),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(16.dp)
                )

                /*
                Crossfade(targetState = clickCount, label = "techbee_logo_swap") { clicks ->
                    Image(
                        painter = if (clicks < 4) painterResource(id = R.drawable.logo_techbee_svg) else painterResource(
                            id = R.drawable.logo_techbee_front
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(16.dp)
                    )
                }
                AnimatedVisibility(visible = clickCount >= 0) {
                    Text(
                        text = "\"" + messages[if (clickCount > 4) 4 else clickCount] + "\"",
                        style = Typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                 */
            }

            Text(
                text = stringResource(Res.string.about_nlnet_thanks),
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            ElevatedCard(
                onClick = {
                    try {
                        uriHandler.openUri("https://nlnet.nl/")
                    } catch (e: Exception) {
                        Napier.w(e.stackTraceToString())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo_nlnet),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(32.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun AboutApp_Preview(
) {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            AboutApp(spectacledVariant = SpectacledVariant.JOURNALS)
        }
    }
}