package at.techbee.spectacled.screens.core.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.color
import spectacled.shared.generated.resources.more_colors
import spectacled.shared.generated.resources.no_color
import spectacled.shared.generated.resources.recent_colors
import dev.zt64.compose.pipette.CircularColorPicker
import dev.zt64.compose.pipette.HsvColor
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSelectorElement(
    recentColors: List<Color>,
    preselectedColor: Color?,
    onColorChanged: (Color) -> Unit,
    skipPartialSelection: Boolean,
    modifier: Modifier = Modifier,
) {

    var selectedColor by remember { mutableStateOf(preselectedColor) }
    var showMore by remember { mutableStateOf(skipPartialSelection) }

    val defaultColors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFF00FF00), // Green
        Color(0xFF0000FF), // Blue
        Color(0xFFFFFF00), // Yellow
        Color(0xFF00FFFF), // Cyan
        Color(0xFFFF00FF), // Magenta

        // Formerly alpha = 0.5
        Color(0xFFFF8080), // Light Red
        Color(0xFF80FF80), // Light Green
        Color(0xFF8080FF), // Light Blue
        Color(0xFFFFFF80), // Light Yellow
        Color(0xFF80FFFF), // Light Cyan
        Color(0xFFFF80FF), // Light Magenta

        // Formerly alpha = 0.2
        Color(0xFFFFCCCC), // Very Light Red
        Color(0xFFCCFFCC), // Very Light Green
        Color(0xFFCCCCFF), // Very Light Blue
        Color(0xFFFFFFCC), // Very Light Yellow
        Color(0xFFCCFFFF), // Very Light Cyan
        Color(0xFFFFCCFF)  // Very Light Magenta
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.color),
            style = MaterialTheme.typography.titleLarge
        )

        ColorSelectionBox(
            color = Color.Transparent,
            selected = preselectedColor == Color.Unspecified,
            onColorSelected = {
                selectedColor = Color.Unspecified
                onColorChanged(Color.Unspecified)
                              },
            content = {
                Icon(
                    imageVector = Icons.Outlined.FormatColorReset,
                    contentDescription = stringResource(Res.string.no_color)
                )
            }
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            maxItemsInEachRow = 6,
            maxLines = if(showMore) 10 else 1
        ) {
            defaultColors.forEach { defaultColor ->
                ColorSelectionBox(
                    color = defaultColor,
                    selected = selectedColor == defaultColor,
                    onColorSelected = {
                        selectedColor = defaultColor
                        onColorChanged(defaultColor)
                    }
                )
            }
        }

        AnimatedVisibility(showMore) {
            CircularColorPicker(
                color = { HsvColor(
                    if(selectedColor == null || selectedColor == Color.Transparent || selectedColor == Color.Unspecified) Color.White else selectedColor!!
                ) },
                onColorChange = {
                    selectedColor = it.toColor()
                    onColorChanged(it.toColor())
                },
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        AnimatedVisibility(showMore && recentColors.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.recent_colors),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    //verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    maxItemsInEachRow = 6,
                    maxLines = if (showMore) 10 else 1
                ) {
                    recentColors.forEach { recentColor ->
                        ColorSelectionBox(
                            color = recentColor,
                            selected = selectedColor == recentColor,
                            onColorSelected = {
                                selectedColor = recentColor
                                onColorChanged(recentColor)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(!showMore) {
            TextButton(
                onClick = { showMore = !showMore },
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            ) {
                Text(stringResource(Res.string.more_colors))
            }
        }
    }
}

@Composable
private fun ColorSelectionBox(
    color: Color,
    selected: Boolean,
    onColorSelected: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape) // Makes the Box round
            .background(
                color = color,
                shape = CircleShape
            )
            .border(
                width = if(selected) 4.dp else 1.dp,
                color = Color.LightGray,
                shape = CircleShape
            )
            .clickable {
                onColorSelected()
            },
        contentAlignment = Alignment.Center // Centers the content (the Icon)
    ) {
        content()
    }
}


@Preview
@Composable
private fun ColorSelectorElement_Preview() {
    ColorSelectorElement(
        recentColors = listOf(Color.Blue, Color.Red, Color.Green),
        preselectedColor = Color.Transparent,
        skipPartialSelection = false,
        onColorChanged = {}
    )
}

@Preview
@Composable
private fun ColorSelectorElement_Preview_skipPartial() {
    ColorSelectorElement(
        recentColors = listOf(Color.Blue, Color.Red, Color.Green),
        preselectedColor = Color.Unspecified,
        skipPartialSelection = true,
        onColorChanged = {}
    )
}
