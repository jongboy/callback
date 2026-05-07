package com.callbacksms.app.service

import java.io.ByteArrayOutputStream

object MmsPduBuilder {
    private const val IMG_NAME = "image.jpg"
    private const val TXT_NAME = "text.txt"
    private const val SMIL_NAME = "smil.xml"

    private const val CT_APPLICATION_SMIL = "application/smil"
    private const val TYPE_MULTIPART_RELATED = 0x33
    private const val TYPE_APPLICATION_SMIL = 0x53
    private const val TYPE_IMAGE_JPEG = 0x1E
    private const val TYPE_TEXT_PLAIN = 0x03
    private const val CHARSET_UTF8 = 106

    fun buildImageTextPdu(to: String, text: String, imageBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val smilBytes = buildSmil().toByteArray(Charsets.UTF_8)

        out.octet(0x8C).octet(0x80) // X-Mms-Message-Type: m-send-req
        out.octet(0x98).textString("T${System.currentTimeMillis().toString(16)}")
        out.octet(0x8D).shortInteger(0x12) // X-Mms-MMS-Version: 1.2
        out.octet(0x85).longInteger(System.currentTimeMillis() / 1000)
        out.octet(0x89).valueLength(1).octet(0x81) // From: insert-address-token
        out.octet(0x97).textString("$to/TYPE=PLMN")

        out.octet(0x84)
        out.multipartRelatedContentType()

        out.uintvar(3)
        out.part(
            contentTypeToken = TYPE_APPLICATION_SMIL,
            name = SMIL_NAME,
            contentId = "smil",
            contentLocation = SMIL_NAME,
            charset = CHARSET_UTF8,
            data = smilBytes
        )
        out.part(
            contentTypeToken = TYPE_IMAGE_JPEG,
            name = IMG_NAME,
            contentId = "image",
            contentLocation = IMG_NAME,
            charset = 0,
            data = imageBytes
        )
        out.part(
            contentTypeToken = TYPE_TEXT_PLAIN,
            name = TXT_NAME,
            contentId = "text",
            contentLocation = TXT_NAME,
            charset = CHARSET_UTF8,
            data = textBytes
        )

        return out.toByteArray()
    }

    private fun buildSmil() =
        "<smil><head><layout>" +
            "<root-layout width=\"320\" height=\"480\"/>" +
            "<region id=\"Image\" left=\"0\" top=\"0\" width=\"100%\" height=\"70%\" fit=\"meet\"/>" +
            "<region id=\"Text\" left=\"0\" top=\"70%\" width=\"100%\" height=\"30%\" fit=\"scroll\"/>" +
            "</layout></head><body><par dur=\"8000ms\">" +
            "<img src=\"$IMG_NAME\" region=\"Image\"/>" +
            "<text src=\"$TXT_NAME\" region=\"Text\"/>" +
            "</par></body></smil>"

    private fun ByteArrayOutputStream.multipartRelatedContentType() {
        val value = ByteArrayOutputStream()
        value.shortInteger(TYPE_MULTIPART_RELATED)
        value.octet(0x8A).textString("<smil>")
        value.octet(0x89).textString(CT_APPLICATION_SMIL)
        valueLength(value.size())
        write(value.toByteArray())
    }

    private fun ByteArrayOutputStream.part(
        contentTypeToken: Int,
        name: String,
        contentId: String,
        contentLocation: String,
        charset: Int,
        data: ByteArray
    ) {
        val headers = ByteArrayOutputStream()
        val contentTypeHeader = ByteArrayOutputStream()

        contentTypeHeader.shortInteger(contentTypeToken)
        contentTypeHeader.octet(0x85).textString(name)
        if (charset != 0) {
            contentTypeHeader.octet(0x81).shortInteger(charset)
        }

        headers.valueLength(contentTypeHeader.size())
        headers.write(contentTypeHeader.toByteArray())
        headers.octet(0xC0).quotedString("<$contentId>")
        headers.octet(0x8E).textString(contentLocation)

        uintvar(headers.size())
        uintvar(data.size)
        write(headers.toByteArray())
        write(data)
    }

    private fun ByteArrayOutputStream.octet(value: Int): ByteArrayOutputStream {
        write(value and 0xFF)
        return this
    }

    private fun ByteArrayOutputStream.shortInteger(value: Int): ByteArrayOutputStream {
        write((value or 0x80) and 0xFF)
        return this
    }

    private fun ByteArrayOutputStream.valueLength(value: Int): ByteArrayOutputStream {
        if (value < 31) {
            write(value)
        } else {
            write(31)
            uintvar(value)
        }
        return this
    }

    private fun ByteArrayOutputStream.longInteger(value: Long): ByteArrayOutputStream {
        val bytes = mutableListOf<Int>()
        var remaining = value
        while (remaining > 0) {
            bytes.add(0, (remaining and 0xFF).toInt())
            remaining = remaining ushr 8
        }
        val encoded = if (bytes.isEmpty()) listOf(0) else bytes
        write(encoded.size)
        encoded.forEach { write(it) }
        return this
    }

    private fun ByteArrayOutputStream.uintvar(value: Int): ByteArrayOutputStream {
        if (value < 128) {
            write(value)
            return this
        }
        val bytes = mutableListOf<Int>()
        var remaining = value
        while (remaining > 0) {
            bytes.add(0, remaining and 0x7F)
            remaining = remaining ushr 7
        }
        for (i in 0 until bytes.size - 1) {
            write(bytes[i] or 0x80)
        }
        write(bytes.last())
        return this
    }

    private fun ByteArrayOutputStream.textString(value: String): ByteArrayOutputStream {
        write(value.toByteArray(Charsets.US_ASCII))
        write(0)
        return this
    }

    private fun ByteArrayOutputStream.quotedString(value: String): ByteArrayOutputStream {
        write(34)
        write(value.toByteArray(Charsets.US_ASCII))
        write(0)
        return this
    }
}
