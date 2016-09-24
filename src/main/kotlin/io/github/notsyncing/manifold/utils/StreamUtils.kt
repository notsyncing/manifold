package io.github.notsyncing.manifold.utils

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.function.Supplier
import java.util.stream.Collectors

object StreamUtils {
    fun pump(input: InputStream, output: OutputStream, executor: Executor = ForkJoinPool.commonPool()): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync(Supplier {
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

            return@Supplier totalRead
        }, executor)
    }

    fun stringToStream(s: String): InputStream {
        return ByteArrayInputStream(s.toByteArray(StandardCharsets.UTF_8))
    }

    fun streamToString(s: InputStream, byteLength: Int = 0): String {
        if (byteLength == 0) {
            return BufferedReader(InputStreamReader(s))
                    .lines()
                    .collect(Collectors.joining("\n"))
        } else {
            val buf = ByteArray(byteLength)
            s.read(buf, 0, byteLength)

            return String(buf, StandardCharsets.UTF_8)
        }
    }
}
