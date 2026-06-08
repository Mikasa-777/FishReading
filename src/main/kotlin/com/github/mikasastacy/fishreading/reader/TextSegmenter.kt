package com.github.mikasastacy.fishreading.reader

object TextSegmenter {
    fun segment(rawText: String, maxLen: Int = 50): List<String> {
        val rawPieces = rawText.split(Regex("(?<=[。！？\n])"))
        val result = mutableListOf<String>()
        var buffer = StringBuilder()

        for (rawPiece in rawPieces) {
            val piece = rawPiece.trim()
            if (piece.isEmpty()) continue

            val chunks = if (piece.length > maxLen) splitLongSentence(piece, maxLen) else listOf(piece)
            for (chunk in chunks) {
                if (buffer.isEmpty()) {
                    buffer.append(chunk)
                } else if (buffer.length + chunk.length <= maxLen) {
                    buffer.append(chunk)
                } else {
                    result.add("// ${buffer.toString().trim()}")
                    buffer = StringBuilder(chunk)
                }
            }
        }

        if (buffer.isNotEmpty()) {
            result.add("// ${buffer.toString().trim()}")
        }
        return result
    }

    private fun splitLongSentence(sentence: String, maxLen: Int): List<String> {
        val subPieces = sentence.split(Regex("(?<=[，；、,;])"))
        val chunks = mutableListOf<String>()
        var buffer = StringBuilder()

        for (sub in subPieces) {
            val trimmed = sub.trim()
            if (trimmed.isEmpty()) continue

            if (buffer.isEmpty()) {
                buffer.append(trimmed)
            } else if (buffer.length + trimmed.length <= maxLen) {
                buffer.append(trimmed)
            } else {
                chunks.addAll(splitHardIfNeeded(buffer.toString(), maxLen))
                buffer = StringBuilder(trimmed)
            }
        }

        if (buffer.isNotEmpty()) {
            chunks.addAll(splitHardIfNeeded(buffer.toString(), maxLen))
        }
        return chunks
    }

    private fun splitHardIfNeeded(text: String, maxLen: Int): List<String> {
        if (text.length <= maxLen) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + maxLen, text.length)
            chunks.add(text.substring(start, end))
            start = end
        }
        return chunks
    }
}
