package com.github.mikasastacy.fishreading.reader

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubParserTest {
    @Test
    fun `extracts heading title before document title`() {
        val html = "<html><head><title>Fallback</title></head><body><h2><span>Chapter One</span></h2></body></html>"

        assertEquals("Chapter One", EpubParser.extractChapterTitle(html))
    }

    @Test
    fun `cleans head tags and common html entities`() {
        val html = "<html><head><title>Hidden</title></head><body><p>A&nbsp;&ldquo;quote&rdquo;</p></body></html>"

        assertEquals("A “quote”", EpubParser.cleanHtml(html))
    }

    @Test
    fun `parses directory epub chapters in spine order`() {
        val directory = createDirectoryEpub(
            "book.epub",
            mapOf(
                "chapters/second.xhtml" to chapterHtml("Second", "第二章正文。"),
                "chapters/first.xhtml" to chapterHtml("First", "第一章正文。")
            ),
            spineItems = listOf(
                ManifestItem("first", "chapters/first.xhtml"),
                ManifestItem("second", "chapters/second.xhtml")
            )
        )

        val chapters = EpubParser.parse(directory, maxLineLength = 20)

        assertEquals(listOf("First", "Second"), chapters.map { it.title })
        assertEquals(listOf("// First第一章正文。"), chapters[0].lines)
        assertEquals(listOf("// Second第二章正文。"), chapters[1].lines)
    }

    @Test
    fun `returns empty chapters for directory epub without container xml`() {
        val directory = createTempDirectory().resolve("book.epub").toFile().apply {
            mkdirs()
        }

        assertTrue(EpubParser.parse(directory).isEmpty())
    }

    @Test
    fun `returns empty chapters for directory epub without valid spine`() {
        val directory = createDirectoryEpub(
            "book.epub",
            mapOf("chapters/one.xhtml" to chapterHtml("One", "正文。")),
            spineItems = emptyList()
        )

        assertTrue(EpubParser.parse(directory).isEmpty())
    }

    @Test
    fun `parses zip epub chapters in spine order`() {
        val file = createZipEpub(
            mapOf(
                "EPUB/chapters/b.xhtml" to chapterHtml("B", "第二章正文。"),
                "EPUB/chapters/a.xhtml" to chapterHtml("A", "第一章正文。")
            ),
            opfPath = "EPUB/content.opf",
            spineItems = listOf(
                ManifestItem("a", "chapters/a.xhtml"),
                ManifestItem("b", "chapters/b.xhtml")
            )
        )

        val chapters = EpubParser.parse(file, maxLineLength = 20)

        assertEquals(listOf("A", "B"), chapters.map { it.title })
    }

    @Test
    fun `falls back to html filename order for zip epub without opf`() {
        val file = createZipEpubWithoutContainer(
            mapOf(
                "chapters/b.xhtml" to chapterHtml("B", "第二章正文。"),
                "chapters/a.xhtml" to chapterHtml("A", "第一章正文。")
            )
        )

        val chapters = EpubParser.parse(file, maxLineLength = 20)

        assertEquals(listOf("A", "B"), chapters.map { it.title })
    }

    private fun createDirectoryEpub(
        directoryName: String,
        htmlFiles: Map<String, String>,
        spineItems: List<ManifestItem>
    ): File {
        val directory = createTempDirectory().resolve(directoryName).toFile().apply {
            mkdirs()
        }
        writeTextFile(directory, "META-INF/container.xml", containerXml("content.opf"))
        writeTextFile(directory, "content.opf", opfXml(spineItems))
        htmlFiles.forEach { (path, content) -> writeTextFile(directory, path, content) }
        return directory
    }

    private fun createZipEpub(
        htmlFiles: Map<String, String>,
        opfPath: String,
        spineItems: List<ManifestItem>
    ): File {
        return createZipEpubWithoutContainer(
            htmlFiles + mapOf(
                "META-INF/container.xml" to containerXml(opfPath),
                opfPath to opfXml(spineItems)
            )
        )
    }

    private fun createZipEpubWithoutContainer(entries: Map<String, String>): File {
        val file = createTempDirectory().resolve("book.epub").toFile()
        ZipOutputStream(Files.newOutputStream(file.toPath())).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return file
    }

    private fun writeTextFile(root: File, relativePath: String, content: String) {
        val file = root.resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    private fun containerXml(opfPath: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="$opfPath" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
    """.trimIndent()

    private fun opfXml(items: List<ManifestItem>): String {
        val manifest = items.joinToString("\n") {
            """    <item id="${it.id}" href="${it.href}" media-type="application/xhtml+xml"/>"""
        }
        val spine = items.joinToString("\n") {
            """    <itemref idref="${it.id}"/>"""
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
              <manifest>
            $manifest
              </manifest>
              <spine>
            $spine
              </spine>
            </package>
        """.trimIndent()
    }

    private fun chapterHtml(title: String, body: String) = """
        <html><head><title>$title</title></head><body><h1>$title</h1><p>$body</p></body></html>
    """.trimIndent()

    private data class ManifestItem(val id: String, val href: String)
}
