package com.github.mikasastacy.fishreading.reader

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

data class Chapter(val title: String, val lines: List<String>)

object EpubParser {
    fun parse(file: File, maxLineLength: Int = 50): List<Chapter> {
        return if (file.isDirectory) {
            DirectoryEpubSource(file).use { source ->
                parse(source, maxLineLength, allowFilenameFallback = false)
            }
        } else {
            ZipEpubSource(file).use { source ->
                parse(source, maxLineLength, allowFilenameFallback = true)
            }
        }
    }

    private fun parse(
        source: EpubSource,
        maxLineLength: Int,
        allowFilenameFallback: Boolean
    ): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val entries = source.spineHtmlPaths().ifEmpty {
            if (allowFilenameFallback) source.htmlPaths() else emptyList()
        }

        var anonymousChapterCount = 1
        for (entry in entries) {
            val rawBytes = source.readBytes(entry) ?: continue
            val htmlContent = String(rawBytes, detectCharset(rawBytes))
            val chapterTitle = extractChapterTitle(htmlContent)
                .takeUnless { it.isBlank() || it.lowercase().contains("untitled") }
                ?: "第 ${anonymousChapterCount++} 部分"

            val cleanText = cleanHtml(htmlContent)
            if (cleanText.isNotBlank()) {
                val chapterLines = TextSegmenter.segment(cleanText, maxLineLength)
                if (chapterLines.isNotEmpty()) {
                    chapters.add(Chapter(chapterTitle, chapterLines))
                }
            }
        }

        return chapters
    }

    private fun EpubSource.spineHtmlPaths(): List<String> {
        val containerXml = readBytes("META-INF/container.xml") ?: return emptyList()
        val container = parseXml(containerXml) ?: return emptyList()
        val opfPath = container.elementsByLocalName("rootfile")
            .firstNotNullOfOrNull { it.getAttribute("full-path").takeIf(String::isNotBlank) }
            ?: return emptyList()
        val opfBytes = readBytes(opfPath) ?: return emptyList()
        val opf = parseXml(opfBytes) ?: return emptyList()
        val opfBasePath = opfPath.substringBeforeLast("/", missingDelimiterValue = "")

        val manifestItems = opf.elementsByLocalName("item").associate { item ->
            item.getAttribute("id") to item.getAttribute("href")
        }
        return opf.elementsByLocalName("itemref").mapNotNull { itemref ->
            val href = manifestItems[itemref.getAttribute("idref")] ?: return@mapNotNull null
            resolveRelativePath(opfBasePath, href).takeIf { isHtmlPath(it) && exists(it) }
        }
    }

    private fun parseXml(bytes: ByteArray): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val xmlBytes = String(bytes, StandardCharsets.UTF_8).trimStart().toByteArray(StandardCharsets.UTF_8)
            factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        } catch (_: Exception) {
            null
        }
    }

    private fun Document.elementsByLocalName(localName: String): List<Element> {
        val namespaced = getElementsByTagNameNS("*", localName)
        val nodes = if (namespaced.length > 0) namespaced else getElementsByTagName(localName)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun resolveRelativePath(basePath: String, href: String): String {
        val cleanHref = href.substringBefore("#")
        return if (basePath.isBlank()) {
            cleanHref
        } else {
            Path.of(basePath).resolve(cleanHref).normalize().toString().replace(File.separatorChar, '/')
        }
    }

    private fun isHtmlPath(path: String): Boolean {
        val lowercasePath = path.lowercase()
        return lowercasePath.endsWith(".xhtml") || lowercasePath.endsWith(".html")
    }

    private interface EpubSource : Closeable {
        fun readBytes(path: String): ByteArray?
        fun htmlPaths(): List<String>
        fun exists(path: String): Boolean = readBytes(path) != null
    }

    private class ZipEpubSource(file: File) : EpubSource {
        private val zip = ZipFile(file, StandardCharsets.UTF_8)

        override fun readBytes(path: String): ByteArray? {
            val entry = zip.getEntry(path) ?: return null
            return zip.getInputStream(entry).use { it.readBytes() }
        }

        override fun htmlPaths(): List<String> {
            return zip.entries().asSequence()
                .filter { !it.isDirectory && isHtmlPath(it.name) }
                .map { it.name }
                .sorted()
                .toList()
        }

        override fun exists(path: String): Boolean = zip.getEntry(path) != null

        override fun close() {
            zip.close()
        }
    }

    private class DirectoryEpubSource(private val root: File) : EpubSource {
        private val rootPath = root.toPath().toAbsolutePath().normalize()

        override fun readBytes(path: String): ByteArray? {
            val file = resolve(path)?.toFile() ?: return null
            if (!file.isFile) return null
            return file.readBytes()
        }

        override fun htmlPaths(): List<String> {
            return root.walkTopDown()
                .filter { it.isFile && isHtmlPath(it.name) }
                .map { rootPath.relativize(it.toPath().toAbsolutePath().normalize()).toString().replace(File.separatorChar, '/') }
                .sorted()
                .toList()
        }

        override fun exists(path: String): Boolean = resolve(path)?.toFile()?.isFile == true

        override fun close() = Unit

        private fun resolve(path: String): Path? {
            val resolved = rootPath.resolve(path).normalize()
            return resolved.takeIf { it.startsWith(rootPath) }
        }
    }

    fun extractChapterTitle(htmlContent: String): String {
        val headingMatch = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.DOT_MATCHES_ALL).find(htmlContent)
        val titleMatch = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL).find(htmlContent)

        return when {
            headingMatch != null -> headingMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            titleMatch != null -> titleMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            else -> ""
        }
    }

    fun cleanHtml(htmlContent: String): String {
        return htmlContent
            .replace(Regex("<head>.*?</head>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .trim()
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()

            decoder.onMalformedInput(CodingErrorAction.REPORT)
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT)

            decoder.decode(ByteBuffer.wrap(bytes))
            StandardCharsets.UTF_8
        } catch (_: Exception) {
            Charset.forName("GBK")
        }
    }
}
