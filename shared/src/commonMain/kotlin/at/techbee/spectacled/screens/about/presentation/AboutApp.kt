package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.shared.BuildKonfig
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.copyright_info
import spectacled.shared.generated.resources.logo_techbee_svg
import spectacled.shared.generated.resources.logo_techbee_xml
import spectacled.shared.generated.resources.spectacled_notes_svg
import spectacled.shared.generated.resources.spectacled_notes_xml
import spectacled.shared.generated.resources.terms_conditions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AboutApp(
    spectacledVariant: SpectacledVariant = koinInject<SpectacledVariant>()
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

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(32.dp)) // Adjust corner radius as needed
                    //.background(MaterialTheme.colorScheme.primary), // Placeholder color,
                    .background(Color(0, 136, 123)),
                contentAlignment = Alignment.Center
            ) {

                getPlatform().PlatformDependentImage(
                    resourceXML = Res.drawable.spectacled_notes_xml,
                    resourceSVG = Res.drawable.spectacled_notes_svg,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            Text(
                text = stringResource(spectacledVariant.appNameStringRes),
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version: ${BuildKonfig.APP_VERSION_STRING}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Build: ${BuildKonfig.APP_BUILD_NUMBER}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Codename: ${BuildKonfig.APP_VERSION_CODENAME}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.terms_conditions),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                content = {
                    Text(
                        text = "<Link to terms & conditions>",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onClick = {
                    try {
                        uriHandler.openUri("https://www.techbee.at/")   // TODO: Update URL
                    } catch (e: Exception) {
                        println(e.stackTraceToString())
                    }
                }
            )
            Text(
                text = stringResource(Res.string.copyright_info),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
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

                getPlatform().PlatformDependentImage(
                    resourceXML = Res.drawable.logo_techbee_xml,
                    resourceSVG = Res.drawable.logo_techbee_svg,
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
        }
    }
}

@Preview
@Composable
fun AboutApp_Preview(
) {
    AboutApp(spectacledVariant = SpectacledVariant.JOURNALS)
}