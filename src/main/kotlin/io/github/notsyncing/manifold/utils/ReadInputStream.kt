package io.github.notsyncing.manifold.utils

import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import java.io.InputStream

class ReadInputStream(val readStream: ReadStream<Buffer>) : InputStream() {
    private var buf: Buffer? = null
    private var counter = 0

    var ended = false

    init {
        readStream.handler {
            buf = it
            readStream.pause()
        }

        readStream.endHandler {
            ended = true
        }
    }

    override fun read(): Int {
        while (buf == null) {
            Thread.sleep(10)

            if (ended) {
                return -1
            }
        }

        val b = buf!!.getUnsignedByte(counter)
        counter++

        if (counter >= buf!!.length()) {
            buf = null
            counter = 0
            readStream.resume()
        }

        return b.toInt()
    }

    override fun available(): Int {
        return buf?.length() ?: 0
    }
}