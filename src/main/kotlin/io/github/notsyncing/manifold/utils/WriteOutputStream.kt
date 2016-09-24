package io.github.notsyncing.manifold.utils

import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.WriteStream
import java.io.OutputStream

class WriteOutputStream(val writeStream: WriteStream<Buffer>) : OutputStream() {
    val buf = ByteArray(1024)
    var counter = 0

    override fun write(b: Int) {
        buf[counter] = b.toByte()
        counter++

        if (counter >= buf.size) {
            flush()
        }
    }

    override fun flush() {
        super.flush()

        if (counter < buf.size) {
            val b = buf.copyOfRange(0, counter)
            writeStream.write(Buffer.buffer(b))
        } else {
            writeStream.write(Buffer.buffer(buf))
        }

        while (writeStream.writeQueueFull()) {
            Thread.sleep(10)
        }

        counter = 0
    }

    override fun close() {
        flush()

        super.close()
    }
}