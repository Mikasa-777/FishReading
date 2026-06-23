package com.example.fishreading

import com.github.mikasastacy.fishreading.reader.Chapter
import com.github.mikasastacy.fishreading.reader.EpubParser
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.StandardCharsets

// 💡 临时伪造一个 TextSegmenter 以防编译报错，保持测试类纯净独立
object TextSegmenter {
    fun segment(text: String, maxLen: Int): List<String> {
        return text.split(Regex("(?<=[。！？\n])")).map { it.trim() }.filter { it.isNotEmpty() }
    }
}

fun main() {

    // ✨ 新增这行：强行把测试进程的控制台输出流锁定为 UTF-8，彻底擦除 Windows 的流对接干扰
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))

    // =======================================================================
    // 🎯 路径留空：请让你朋友在此处指定他在 Windows 上发生乱码的 EPUB 绝对路径
    // =======================================================================
    val EPUB_FILE_PATH = "C:/Users/Administrator/Downloads/test.epub"

    println("=========================================================")
    println("🔍 FishReading 插件全链路[解压-清洗-渲染]跨平台底层诊断开始...")
    println("=========================================================\n")

    val file = File(EPUB_FILE_PATH)
    if (!file.exists()) {
        println("❌ 错误：在指定路径下找不到测试电子书，请核对路径！")
        return
    }

    // -------------------------------------------------------------------
    // 🧪 流程一：验证【解析清洗层】(EpubParser) 是否完好
    // -------------------------------------------------------------------
    println("[流程一验证]：正在调用 EpubParser 底层解压并清洗文本...")
    var parsedChapters: List<Chapter> = emptyList()
    try {
        parsedChapters = EpubParser.parse(file)
        println("➔ 成功解析出章节数量: ${parsedChapters.size}")

        if (parsedChapters.isNotEmpty()) {
            val firstChapter = parsedChapters.first()
            println("➔ 首个章节名称: ${firstChapter.title}")
            println("➔ 随机抽取该章前 3 句话进行文本健康度核查：")
            println("---------------------------------------------------------")
            firstChapter.lines.take(3).forEach { println(it) }
            println("---------------------------------------------------------")
            println("💡 【核查提示】：")
            println("   如果上面横线里显示的是正常的中文，恭喜你！[解析清洗层] 100% 正常。")
            println("   如果横线里显示的是奇怪的乱码、问号或乱七八糟的符号，说明真凶是 [解析清洗层]！")
        } else {
            println("❌ 拦截警告：解析出来的章节列表为空！可能是 Zip 内部文件名乱码导致匹配失败。")
        }
    } catch (e: Exception) {
        println("❌ [解析层] 运行时发生崩溃: ${e.message}")
        e.printStackTrace()
    }

    println("\n" + "=".repeat(57) + "\n")

    // -------------------------------------------------------------------
    // 🧪 流程二：验证【IDE 字体渲染层】(FishInlayRenderer) 在 Windows 下的本地健康度
    // -------------------------------------------------------------------
    println("[流程二验证]：正在模拟 FishInlayRenderer 本地图形画笔渲染测试...")

    // 模拟一段我们要渲染的小说文字
    val sampleFishText = "// 这是一个阳光明媚的早晨，主角缓缓睁开了眼。"

    // 模拟 IDEA 常用的物理英文字体（Windows 乱码的头号嫌疑犯）
    val suspectFonts = listOf("JetBrains Mono", "Consolas", "Courier New")

    // 建立一个内存画布，拿到与 IDEA 插件里完全等价的 Graphics2D 图形上下文
    val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    val g2d = img.createGraphics()

    for (fontName in suspectFonts) {
        // 模拟调用原先的 g.font = editor.colorsScheme.getFont(...)
        val currentFont = Font(fontName, Font.PLAIN, 14)
        g2d.font = currentFont

        // 核心 API：canDisplayUpTo 会检查这个字体里有没有这些汉字的字形（Glyphs）。
        // 如果返回 -1，说明字体完美支持该文本；如果返回 >= 0 的数字，代表从第几个字符开始无法显示（会变成方块☐）。
        val failIndex = currentFont.canDisplayUpTo(sampleFishText)

        println("➔ 测试字体 [ $fontName ] 渲染中文结果：")
        if (failIndex == -1) {
            println("   ✅ 正常：该字体能完美支撑中文渲染。")
        } else {
            println("   ❌ 沦陷：该字体在 Windows 下由于缺乏中文字形，将在第 $failIndex 个字引发【方块☐乱码】！")
        }
    }
    g2d.dispose()

    println("\n=========================================================")
    println("🏁 诊断结束，请对照上方两个流程的输出判定真凶。")
    println("=========================================================")
}