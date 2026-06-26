package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.eraser
import spectacled.shared.generated.resources.ic_canvas_eraser
import spectacled.shared.generated.resources.ic_canvas_marker
import spectacled.shared.generated.resources.ic_canvas_pen
import spectacled.shared.generated.resources.marker
import spectacled.shared.generated.resources.pen
import kotlin.math.abs
import kotlin.time.Clock

data class PathData(
    val id: String,
    val color: Color,
    val thickness: Float,
    val paths: List<Offset>
)

enum class DrawingTool {
    Pen, Marker, Eraser
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvas(
    paths: List<PathData>,
    onAddPath: (PathData) -> Unit = {},
    onRemovePaths: (List<PathData>) -> Unit = {},
    modifier: Modifier = Modifier,
) {

    var selectedTool by remember { mutableStateOf(DrawingTool.Pen) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedThickness by remember { mutableStateOf(10f) }
    var currentPath: PathData? by remember { mutableStateOf(null) }


    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Canvas(
            modifier = Modifier
                .clipToBounds()
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .pointerInput(selectedTool) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (selectedTool == DrawingTool.Pen || selectedTool == DrawingTool.Marker) {
                                currentPath = PathData(
                                    id = Clock.System.now().toString(),
                                    color = selectedColor,
                                    thickness = selectedThickness,
                                    paths = listOf(offset)
                                )
                            } else {
                                // Erase paths at the initial touch position
                                val removedPaths = paths.filter { path ->
                                    path.paths.any { (it - offset).getDistance() < (path.thickness + selectedThickness) / 2 }
                                }
                                onRemovePaths(removedPaths)
                            }
                        },
                        onDragEnd = {
                            currentPath?.let { onAddPath(it) }
                            currentPath = null
                        },
                        onDragCancel = {
                            currentPath?.let { onAddPath(it) }
                            currentPath = null
                        },
                        onDrag = { change, _ ->
                            if (selectedTool == DrawingTool.Pen || selectedTool == DrawingTool.Marker) {
                                currentPath?.let { current ->
                                    currentPath = current.copy(paths = current.paths.plus(change.position))
                                }
                            } else {
                                // Find and remove paths near the current drag position
                                val removedPaths = paths.filter { path ->
                                    path.paths.any { (it - change.position).getDistance() < (path.thickness + selectedThickness) / 2 }
                                }
                                onRemovePaths(removedPaths)
                            }
                        }
                    )
                }
                .pointerInput(selectedTool) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if(selectedTool == DrawingTool.Eraser) {
                                // Find and remove paths near the current drag position
                                val removedPaths = paths.filter { path ->
                                    path.paths.any { (it - tapOffset).getDistance() < (path.thickness + selectedThickness) / 2 }
                                }
                                onRemovePaths(removedPaths)
                            }
                        },
                        onPress = { tapOffset ->
                            if(selectedTool == DrawingTool.Eraser) {
                                // Find and remove paths near the current drag position
                                val removedPaths = paths.filter { path ->
                                    path.paths.any { (it - tapOffset).getDistance() < (path.thickness + selectedThickness) / 2 }
                                }
                                onRemovePaths(removedPaths)
                            }
                        }
                    )
                }
        ) {
            paths.fastForEach { pathData ->
                drawPath(
                    path = pathData.paths,
                    color = pathData.color,
                    thickness = pathData.thickness,
                    smoothness = 3
                )
            }

            currentPath?.let {
                drawPath(
                    path = it.paths,
                    color = it.color,
                    thickness = it.thickness,
                    smoothness = 1
                )
            }
        }

        val standardColors = listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
        val maxThickness = 40
        val markerAlpha = 0.33f

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {

            IconButton(
                onClick = {
                    selectedTool = DrawingTool.Pen
                    selectedColor = selectedColor.copy(alpha = 1f)
                },
                modifier = Modifier.background(
                    if (selectedTool == DrawingTool.Pen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    CircleShape
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_canvas_pen),
                    contentDescription = stringResource(Res.string.pen)
                )
            }

            IconButton(
                onClick = {
                    selectedTool = DrawingTool.Marker
                    selectedColor = selectedColor.copy(alpha = markerAlpha)
                },
                modifier = Modifier.background(
                    if (selectedTool == DrawingTool.Marker) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    CircleShape
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_canvas_marker),
                    contentDescription = stringResource(Res.string.marker)
                )
            }

            IconButton(
                onClick = { selectedTool = DrawingTool.Eraser },
                modifier = Modifier.background(
                    if (selectedTool == DrawingTool.Eraser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    CircleShape
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_canvas_eraser),
                    contentDescription = stringResource(Res.string.eraser)
                )
            }
        }

        AnimatedVisibility(selectedTool != DrawingTool.Eraser) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()),
            ) {
                standardColors
                    .map { if (selectedTool == DrawingTool.Marker) it.copy(alpha = markerAlpha) else it }
                    .forEach { color ->
                        ColorSelectionBox(
                            color = color,
                            selected = selectedColor == color,
                            onColorSelected = { selectedColor = color }
                        )
                    }
            }
        }


        Slider(
            value = selectedThickness,
            onValueChange = { selectedThickness = it },
            valueRange = 1f..maxThickness.toFloat(),
            thumb = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.sizeIn(maxThickness.dp).clip(CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .size(selectedThickness.dp)
                            .clip(CircleShape) // Makes the Box round
                            .background(
                                color = selectedColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center // Centers the content (the Icon)
                    ) { }
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun DrawScope.drawPath(
    path: List<Offset>,
    color: Color,
    thickness: Float = 10f,
    smoothness: Int = 10
) {

    val smoothedPath = Path().apply {
        if(path.isNotEmpty()) {
            moveTo(path.first().x, path.first().y)
            for(i in 1..path.lastIndex) {
                val from = path[i - 1]
                val to = path[i]
                val dx = abs(from.x-to.x)
                val dy = abs(from.y - to.y)

                if(dx >= smoothness || dy >= smoothness) {
                    quadraticTo(
                        x1 = (from.x + to.x) / 2,
                        y1 = (from.y + to.y) / 2,
                        x2 = to.x,
                        y2 = to.y
                    )
                }

            }

        }
    }

    drawPath(
        smoothedPath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}


@Preview
@Composable
private fun DrawingCanvas_Preview() {

    val paths = mutableStateListOf<PathData>()

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        DrawingCanvas(
            paths = paths,
            onAddPath = { paths.add(it) },
            onRemovePaths = { paths.removeAll(it) },
            modifier = Modifier.fillMaxSize()
        )
    }
}