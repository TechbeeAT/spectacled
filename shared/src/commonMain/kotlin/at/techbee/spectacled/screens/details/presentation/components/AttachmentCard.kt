package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.MIMETYPE_SVG
import at.techbee.spectacled.screens.core.presentation.components.PathData
import at.techbee.spectacled.screens.core.presentation.components.PathDataSvgConverter
import at.techbee.spectacled.screens.core.presentation.components.drawPath
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import coil3.compose.AsyncImage
import io.ktor.utils.io.core.toByteArray
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.attachment
import spectacled.shared.generated.resources.attachment_sync_state_pending_download
import spectacled.shared.generated.resources.attachment_sync_state_pending_upload
import spectacled.shared.generated.resources.attachment_sync_state_sync_error
import spectacled.shared.generated.resources.attachment_sync_state_synchronized
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.drawing
import spectacled.shared.generated.resources.file_size_kb
import spectacled.shared.generated.resources.unknown
import spectacled.shared.generated.resources.ic_cloud_error

@Composable
fun AttachmentCard(
    attachment: Attachment,
    onAction: (DetailsAction) -> Unit,
    isDownloading: Boolean = false,
    fileManager: FileManager = koinInject<FileManager>(),
    modifier: Modifier = Modifier
) {

    var drawingPaths by remember { mutableStateOf<List<PathData>?>(null) }
    var imagePreview by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attachment) {
        if (attachment.isSVG() && attachment.localPath != null && fileManager.exists(attachment.localPath)) {
            val fileContent = fileManager.readAttachment(attachment.localPath).decodeToString()
            PathDataSvgConverter.fromSvg(fileContent)?.let { drawingPaths = it }
        } else if(attachment.isImage()) {
            if (attachment.localPath != null && fileManager.exists(attachment.localPath)) {
                imagePreview = attachment.localPath
            }
        }
    }

    Card(
        onClick = {
            if(drawingPaths?.isNotEmpty() == true)
                drawingPaths?.let { onAction(DetailsAction.OnShowDrawingCanvasBottomSheet(true, attachment.uid, it)) }
            else
                onAction(DetailsAction.OnOpenAttachment(attachment.uid)) },
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        modifier = modifier
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(
                    onClick = {},
                    enabled = false
                ) {
                    if(drawingPaths?.isNotEmpty() == true)
                        Icon(Icons.Outlined.Draw, stringResource(Res.string.drawing))
                    else
                        Icon(Icons.Outlined.Attachment, stringResource(Res.string.attachment))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.fileName ?: stringResource(Res.string.unknown),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (attachment.size != null) {
                        Text(
                            text = stringResource(Res.string.file_size_kb, (attachment.size / 1024).toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    attachment.syncErrorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                AnimatedVisibility(isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        strokeWidth = 2.dp
                    )
                }

                AnimatedVisibility(!isDownloading && !attachment.isInline) {
                    IconButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Crossfade(attachment.syncState) { attachmentSyncState ->
                            when(attachmentSyncState) {
                                AttachmentSyncState.LOCAL_MODIFIED -> {
                                    Icon(Icons.Outlined.CloudUpload, stringResource(Res.string.attachment_sync_state_pending_upload))
                                }
                                AttachmentSyncState.SYNCED -> {
                                    Icon(Icons.Outlined.CloudDone, stringResource(Res.string.attachment_sync_state_synchronized))
                                }
                                AttachmentSyncState.PENDING_DOWNLOAD -> {
                                    Icon(Icons.Outlined.CloudDownload, stringResource(Res.string.attachment_sync_state_pending_download))
                                }
                                AttachmentSyncState.FAILED -> {
                                    Icon(
                                        painterResource(Res.drawable.ic_cloud_error),
                                        stringResource(Res.string.attachment_sync_state_sync_error)
                                    )
                                }
                            }

                        }
                    }
                }

                IconButton(onClick = { onAction(DetailsAction.OnDeleteAttachment(attachment.uid)) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            AnimatedVisibility(drawingPaths?.isNotEmpty() == true) {
                Canvas(
                    modifier = Modifier
                        .clipToBounds()
                        .fillMaxWidth()
                        .height(600.dp)
                        .background(Color.White)
                ) {
                    drawingPaths?.fastForEach { pathData ->
                        drawPath(
                            path = pathData.paths,
                            color = pathData.color,
                            thickness = pathData.thickness,
                            smoothness = 3
                        )
                    }
                }
            }

            AnimatedVisibility(imagePreview != null) {
                AsyncImage(
                    model = imagePreview,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Preview
@Composable
private fun AttachmentCard_Preview() {
    AttachmentCard(
        attachment = Attachment(
            id = 1L,
            fileName = "test.pdf",
            mimeType = "application/pdf",
            size = 125000L
        ),
        onAction = {},
        fileManager = object: FileManager {
            override fun getAttachmentsDirectory() = "/"
            override fun saveAttachment(fileName: String, bytes: ByteArray) = "/file.pdf"
            override fun readAttachment(path: String) = byteArrayOf()
            override fun deleteAttachment(path: String) = false
            override fun exists(path: String) = true

        },
        modifier = Modifier.padding(8.dp)
    )
}



@Preview
@Composable
private fun AttachmentCard_Drawing_Preview() {

    val sampleSVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 758.94 1165.91\" width=\"758.94\" height=\"1165.91\">\n" +
            "  <path d=\"M 588.98 625.91 Q 588.98 625.91 588.98 625.91 Q 596.96 617.92 604.95 609.93 Q 620.95 586.44 636.95 562.95 Q 650.46 531.44 663.98 499.93 Q 679.97 462.41 695.97 424.89 Q 697.96 404.89 699.95 384.88 Q 695.95 375.32 691.95 365.76 Q 686.92 363.86 681.9 361.96 Q 670.47 361.96 659.03 361.96 Q 529.49 442.45 399.95 522.94 Q 326.96 593.93 253.98 664.92 Q 232.47 703.56 210.97 742.21 Q 206.96 755.66 202.96 769.11 Q 204.4 772.5 205.84 775.88 Q 208.78 777.88 211.71 779.88 Q 242.85 781.89 273.98 783.89 Q 352.47 779.89 430.97 775.88 Q 501.96 753.91 572.96 731.95 Q 607.31 714.43 641.67 696.91 Q 652.93 684.81 664.2 672.72 Q 666.08 667.82 667.96 662.92 Q 659.16 672.69 650.36 682.47 Q 631.66 709.21 612.96 735.96 Q 589.46 793.44 565.97 850.92 Q 557.96 884.43 549.95 917.94 Q 554.25 937.42 558.54 956.9 Q 567.74 966.93 576.95 976.95 Q 602.96 988.41 628.98 999.87 Q 669.97 1003.88 710.97 1007.89 Q 731.58 1005.89 752.19 1003.88 Q 755.56 999.92 758.94 995.95 Q 756.44 997.91 753.94 999.87 Q 748.89 1003.43 743.85 1007 Q 734.53 1019.76 725.21 1032.52 Q 716.88 1048.98 708.55 1065.45 Q 700.25 1089.97 691.95 1114.5 Q 689.95 1130.18 687.96 1145.86 Q 687.96 1154.66 687.96 1163.46 Q 687.96 1164.68 687.96 1165.91\" fill=\"none\" stroke=\"#000000\" stroke-opacity=\"1\" stroke-width=\"5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>\n" +
            "  <metadata id=\"drawingData\"><![CDATA[\n" +
            "W:758.9375,1165.9092\n" +
            "S:2026-07-02T07:59:51.037229Z|ff000000|5.0\n" +
            "588.97656,625.9072\n" +
            "588.97656,625.9072\n" +
            "604.9531,609.92676\n" +
            "636.9453,562.94824\n" +
            "663.97656,499.93262\n" +
            "695.96875,424.88965\n" +
            "699.9531,384.8838\n" +
            "691.9453,365.7627\n" +
            "681.90234,361.9619\n" +
            "659.0283,361.9619\n" +
            "399.95312,522.9424\n" +
            "253.97656,664.9238\n" +
            "210.96875,742.2051\n" +
            "202.96094,769.11426\n" +
            "205.83984,775.876\n" +
            "211.71387,779.88477\n" +
            "273.97656,783.89453\n" +
            "430.96875,775.876\n" +
            "572.96094,731.94824\n" +
            "641.66797,696.91016\n" +
            "664.2012,672.7158\n" +
            "667.96094,662.91895\n" +
            "650.3633,682.4658\n" +
            "612.96094,735.95703\n" +
            "565.96875,850.91895\n" +
            "549.9531,917.94336\n" +
            "558.53906,956.9033\n" +
            "576.9453,976.9502\n" +
            "628.97656,999.8721\n" +
            "710.96875,1007.8906\n" +
            "752.1904,1003.88184\n" +
            "758.9375,995.9502\n" +
            "753.9365,999.8721\n" +
            "743.8506,1006.9961\n" +
            "725.20703,1032.5195\n" +
            "708.55176,1065.4453\n" +
            "691.9453,1114.501\n" +
            "687.96094,1145.8623\n" +
            "687.96094,1163.4551\n" +
            "687.96094,1165.9092\n" +
            "  ]]></metadata>\n" +
            "</svg>"

    AttachmentCard(
        attachment = Attachment(
            id = 1L,
            fileName = "test.svg",
            mimeType = MIMETYPE_SVG,
            size = 125000L
        ),
        onAction = {},
        fileManager = object: FileManager {
            override fun getAttachmentsDirectory() = "/"
            override fun saveAttachment(fileName: String, bytes: ByteArray) = "/drawing.svg"
            override fun readAttachment(path: String) = sampleSVG.toByteArray()
            override fun deleteAttachment(path: String) = false
            override fun exists(path: String) = true

        },
        modifier = Modifier.padding(8.dp)
    )
}


@Preview
@Composable
private fun AttachmentCard_SyncState_SYNCHRONIZED_Preview() {


    AttachmentCard(
        attachment = Attachment(
            id = 1L,
            syncState = AttachmentSyncState.SYNCED,
            fileName = "my document.pdf",
            mimeType = "application/pdf",
            size = 125000L
        ),
        onAction = {},
        fileManager = object: FileManager {
            override fun getAttachmentsDirectory() = "/"
            override fun saveAttachment(fileName: String, bytes: ByteArray) = "/test.pdf"
            override fun readAttachment(path: String) = "".toByteArray()
            override fun deleteAttachment(path: String) = false
            override fun exists(path: String) = true

        },
        modifier = Modifier.padding(8.dp)
    )
}

@Preview
@Composable
private fun AttachmentCard_SyncState_PENDING_DOWNLOAD_Preview() {


    AttachmentCard(
        attachment = Attachment(
            id = 1L,
            syncState = AttachmentSyncState.PENDING_DOWNLOAD,
            fileName = "my document.pdf",
            mimeType = "application/pdf",
            size = 125000L
        ),
        onAction = {},
        fileManager = object: FileManager {
            override fun getAttachmentsDirectory() = "/"
            override fun saveAttachment(fileName: String, bytes: ByteArray) = "/test.pdf"
            override fun readAttachment(path: String) = "".toByteArray()
            override fun deleteAttachment(path: String) = false
            override fun exists(path: String) = true

        },
        modifier = Modifier.padding(8.dp)
    )
}

@Preview
@Composable
private fun AttachmentCard_SyncState_FAILED_Preview() {


    AttachmentCard(
        attachment = Attachment(
            id = 1L,
            syncState = AttachmentSyncState.FAILED,
            syncErrorMessage = "The upload of the attachment failed because of unknown reasons.",
            fileName = "my document.pdf",
            mimeType = "application/pdf",
            size = 125000L
        ),
        onAction = {},
        fileManager = object: FileManager {
            override fun getAttachmentsDirectory() = "/"
            override fun saveAttachment(fileName: String, bytes: ByteArray) = "/test.pdf"
            override fun readAttachment(path: String) = "".toByteArray()
            override fun deleteAttachment(path: String) = false
            override fun exists(path: String) = true

        },
        modifier = Modifier.padding(8.dp)
    )
}