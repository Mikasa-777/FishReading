package com.example.fishreading.loader

import com.example.fishreading.FishReaderService
import java.io.File

interface BookParser {
    // 所有的解析器只需要消纳一个文件，统一返回高层定义好的章节列表
    fun parse(file: File): List<FishReaderService.Chapter>
}