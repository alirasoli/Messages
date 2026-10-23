package org.fossify.messages.extensions

fun String.getExtensionFromMimeType(): String {
    return when (lowercase()) {
        "image/png" -> ".png"
        "image/apng" -> ".apng"
        "image/webp" -> ".webp"
        "image/svg+xml" -> ".svg"
        "image/gif" -> ".gif"
        else -> ".jpg"
    }
}

fun String.isImageMimeType(): Boolean {
    return lowercase().startsWith("image")
}

fun String.isGifMimeType(): Boolean {
    return lowercase().endsWith("gif")
}

fun String.isVideoMimeType(): Boolean {
    return lowercase().startsWith("video")
}

fun String.isVCardMimeType(): Boolean {
    val lowercase = lowercase()
    return lowercase.endsWith("x-vcard") || lowercase.endsWith("vcard")
}

fun String.isAudioMimeType(): Boolean {
    return lowercase().startsWith("audio")
}

fun String.isCalendarMimeType(): Boolean {
    return lowercase().endsWith("calendar")
}

fun String.isPdfMimeType(): Boolean {
    return lowercase().endsWith("pdf")
}

fun String.isZipMimeType(): Boolean {
    return lowercase().endsWith("zip")
}

fun String.isPlainTextMimeType(): Boolean {
    return lowercase() == "text/plain"
}

/**
 * Returns the first match of [regex] in this text, or null if there is none. When the regex has a
 * capture group the content of the first group is used, so a regex like `code is (\\d+)` works.
 */
fun String.extractVerificationCode(regex: Regex): String? {
    val match = regex.find(this) ?: return null
    val code = if (match.groups.size > 1) match.groups[1]?.value ?: match.value else match.value
    return code.takeIf { it.isNotEmpty() }
}
