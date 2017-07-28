package io.github.notsyncing.manifold.utils

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FileUtils {
    fun deleteRecursive(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path)
                .forEach { Files.delete(it) }
    }

    private fun extractFile(zipIn: ZipInputStream, file: File) {
        BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
            val buffer = ByteArray(1024)
            var location = zipIn.read(buffer)

            while (location != -1) {
                outputStream.write(buffer, 0, location)
                location = zipIn.read(buffer)
            }
        }
    }

    fun unzip(source: File, destination: File, charset: Charset) {
        ZipInputStream(FileInputStream(source), charset).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val file = File(destination, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    val parent = file.parentFile
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                    extractFile(zipIn, file)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    fun unzip(source: Path, destination: Path, charset: Charset = Charset.forName("utf-8")) {
        unzip(source.toFile(), destination.toFile(), charset)
    }
}