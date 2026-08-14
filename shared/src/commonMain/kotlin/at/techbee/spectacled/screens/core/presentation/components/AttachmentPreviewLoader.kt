package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.util.fastForEach
import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.ioDispatcher
import kotlinx.coroutines.withContext

/**
 * The resolved preview for a single [Attachment]: either an editable drawing
 * (paths + the canvas size they were drawn on) or a path to an image file. All
 * fields are `null` for attachments that can't be previewed (e.g. PDFs).
 */
data class AttachmentPreviewState(
    val drawingPaths: List<PathData>? = null,
    val drawingSize: Size? = null,
    val imagePath: String? = null
)

/**
 * Loads the preview for [attachment], reading the file from disk and parsing SVG
 * drawings **off the main thread** (on [ioDispatcher]) so it's safe to use from a
 * scrolling list without janking composition. Recomputes whenever [attachment]
 * changes and returns an empty [AttachmentPreviewState] until the load completes.
 */
@Composable
fun rememberAttachmentPreviewState(
    attachment: Attachment,
    fileManager: FileManager
): AttachmentPreviewState {
    var state by remember { mutableStateOf(AttachmentPreviewState()) }

    LaunchedEffect(attachment) {
        state = withContext(ioDispatcher) {
            val localPath = attachment.localPath
            when {
                attachment.isSVG() && localPath != null && fileManager.exists(localPath) -> {
                    val fileContent = fileManager.readAttachment(localPath).decodeToString()
                    PathDataSvgConverter.fromSvg(fileContent)?.let {
                        AttachmentPreviewState(drawingPaths = it.paths, drawingSize = Size(it.width, it.height))
                    } ?: AttachmentPreviewState()
                }

                attachment.isImage() && localPath != null && fileManager.exists(localPath) ->
                    AttachmentPreviewState(imagePath = localPath)

                else -> AttachmentPreviewState()
            }
        }
    }

    return state
}

/**
 * Draws [paths] into this [DrawScope], scaled so the drawing's [canvasSize] fills
 * the current width (origin-anchored). No-op if there's nothing to draw or the
 * canvas size is unknown/zero.
 */
fun DrawScope.drawScaledPaths(paths: List<PathData>?, canvasSize: Size?) {
    if (paths == null || canvasSize == null || canvasSize.width <= 0f) return

    val scaleFactor = size.width / canvasSize.width
    scale(scaleFactor, pivot = Offset.Zero) {
        paths.fastForEach { pathData ->
            drawPath(
                path = pathData.paths,
                color = pathData.color,
                thickness = pathData.thickness,
                smoothness = 3
            )
        }
    }
}
