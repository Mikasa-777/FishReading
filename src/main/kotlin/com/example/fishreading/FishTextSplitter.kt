package com.example.fishreading

object FishTextSplitter {

    /**
     * 智能文本切分与聚合算法（核心优化点 ✨）
     * 黄金舒适区间：20 ~ 50 个字
     */
    fun segmentTextIntelligently(rawText: String, maxLen: Int = 50): List<String> {

        // 1. 先按传统的句子结束符初筛切断
        val rawPieces = rawText.split(Regex("(?<=[。！？\n])"))
        val result = mutableListOf<String>()
        var buffer = StringBuilder()

        for (rawPiece in rawPieces) {
            val piece = rawPiece.trim()
            if (piece.isEmpty()) continue

            // 2. 情况 3：针对单句就长得离谱的超长句，调用下游函数先按逗号分级切碎
            val chunks = if (piece.length > maxLen) {
                splitLongSentence(piece, maxLen)
            } else {
                listOf(piece)
            }

            for (chunk in chunks) {
                if (buffer.isEmpty()) {
                    buffer.append(chunk)
                } else if (buffer.length + chunk.length <= maxLen) {
                    // 情况 1 & 2：如果当前缓冲区加上新句没有超标，直接合并（完美解决过短问题）
                    buffer.append(chunk)
                } else {
                    // 缓冲区满了，冲刷进结果集，并开启新一轮的收集
                    result.add("// ${buffer.toString().trim()}")
                    buffer = StringBuilder(chunk)
                }
            }
        }

        // 别忘了冲刷最后留在缓冲区里的残余文本
        if (buffer.isNotEmpty()) {
            result.add("// ${buffer.toString().trim()}")
        }
        return result
    }

    /**
     * 辅助函数：处理长句，优先按次级标点拆分
     */
    fun splitLongSentence(sentence: String, maxLen: Int): List<String> {
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
                chunks.add(buffer.toString())
                buffer = StringBuilder(trimmed)
            }
        }

        if (buffer.isNotEmpty()) {
            val finalStr = buffer.toString()
            // 如果遇到丧心病狂、连逗号都没有的超级长句，直接强行物理截断
            if (finalStr.length > maxLen) {
                var start = 0
                while (start < finalStr.length) {
                    val end = minOf(start + maxLen, finalStr.length)
                    chunks.add(finalStr.substring(start, end))
                    start = end
                }
            } else {
                chunks.add(finalStr)
            }
        }
        return chunks
    }
}