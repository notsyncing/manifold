package io.github.notsyncing.manifold.utils

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

object StreamUtils {
    fun pump(input: InputStream, output: OutputStream): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            val buffer = ByteArray(1024)
            var totalRead: Long = 0
            var bytesRead: Int

            while (true) {
                bytesRead = input.read(buffer)

                if (bytesRead < 0) {
                    break
                }

                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
            }

            return@supplyAsync totalRead
        }
    }

    fun stringToStream(s: String): InputStream {
        return ByteArrayInputStream(s.toByteArray(StandardCharsets.UTF_8))
    }

    fun streamToString(s: InputStream): String {
        return BufferedReader(InputStreamReader(s))
                .lines()
                .collect(Collectors.joining("\n"))
    }
}
