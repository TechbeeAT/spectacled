package at.techbee.spectacled.screens.core.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttachmentTest {

    @Test
    fun svgDetection() {
        assertTrue(Attachment(mimeType = MIMETYPE_SVG).isSVG())
        assertFalse(Attachment(mimeType = "image/png").isSVG())
        assertFalse(Attachment(mimeType = null).isSVG())
    }

    @Test
    fun imageDetection() {
        assertTrue(Attachment(mimeType = "image/png").isImage())
        assertTrue(Attachment(mimeType = MIMETYPE_SVG).isImage())   // SVG is also an image/*
        assertFalse(Attachment(mimeType = "application/pdf").isImage())
        assertFalse(Attachment(mimeType = null).isImage())
    }
}
