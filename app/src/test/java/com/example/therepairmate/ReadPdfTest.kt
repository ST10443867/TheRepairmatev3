package com.example.therepairmate

import org.junit.Test
import java.io.File

class ReadPdfTest {
    @Test
    fun extractPdfContent() {
        val file = File("C:/Users/Lesego Mogara/Downloads/MobileAPI_Documentation.pdf")
        if (!file.exists()) {
            println("PDF not found at ${file.absolutePath}")
            return
        }
        val bytes = file.readBytes()
        val text = String(bytes, Charsets.ISO_8859_1)
        println("--- PDF Raw Content Extract ---")
        val regex = Regex("\\((.*?)\\)")
        val matches = regex.findAll(text)
        for (m in matches) {
            val content = m.groupValues[1]
            if (content.length > 2 && content.any { it.isLetter() }) {
                println(content)
            }
        }
    }
}
