package io.github.notsyncing.manifold.eventbus.event

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.utils.StreamUtils
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicLong

class ManifoldEvent() {
    var counter: Long = 0
        private set
    var replyToCounter: Long = 0
    var type: EventType = EventType.Data
    var sendType: EventSendType = EventSendType.Unicast
    var event: String = ""
    var source: String = ""
    var target: String = ""
    var sessionId: String? = null

    var data: String = ""
        set(value) {
            field = value
            dataBytes = value.toByteArray(StandardCharsets.UTF_8)
            dataLength = dataBytes!!.size.toLong()
        }

    var dataStream: InputStream? = null
    var dataLength: Long = 0

    private var dataBytes: ByteArray? = null

    init {
        this.counter = staticCounter.incrementAndGet()
    }

    constructor(event: String, data: String) : this() {
        this.data = data
        this.event = event
        this.type = EventType.Data
    }

    constructor(event: String, data: InputStream, dataLength: Long) : this(event, "") {
        this.dataStream = data
        this.dataLength = dataLength
    }

    constructor(event: String, data: Any) : this(event, if (data is String) data else JSON.toJSONString(data))

    fun replyToCounter(counter: Long): ManifoldEvent {
        replyToCounter = counter
        return this
    }

    fun <T> getData(dataType: Class<T>): T {
        return JSON.parseObject(data, dataType)
    }

    private fun DataOutputStream.writeStringWithLength(str: String?, isLongStr: Boolean = false) {
        if (str?.isEmpty() == false) {
            val s = str!!.toByteArray(StandardCharsets.UTF_8)

            if (isLongStr) {
                this.writeLong(s.size.toLong())
            } else {
                this.writeShort(s.size)
            }

            this.write(s)
        } else {
            if (isLongStr) {
                this.writeLong(0)
            } else {
                this.writeShort(0)
            }
        }
    }

    fun serialize(): ByteArray {
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { stream ->
                serialize(stream).get()
                return bytes.toByteArray()
            }
        }
    }

    fun serialize(stream: DataOutputStream, executor: Executor = ForkJoinPool.commonPool()): CompletableFuture<Void> {
        stream.writeChar(MAGIC.toInt())
        stream.writeByte(VERSION.toInt())
        stream.writeLong(counter)
        stream.writeLong(replyToCounter)
        stream.writeByte(type.ordinal)
        stream.writeByte(sendType.ordinal)

        stream.writeStringWithLength(event)
        stream.writeStringWithLength(source)
        stream.writeStringWithLength(target)
        stream.writeStringWithLength(sessionId)

        if (dataStream != null) {
            stream.writeByte(1)
            stream.writeLong(dataLength)

            return StreamUtils.pump(dataStream!!, stream, executor).thenApply { null }
        } else {
            val s = dataBytes!!
            stream.writeByte(0)
            stream.writeLong(dataLength)
            stream.write(s)

            return CompletableFuture.completedFuture(null)
        }
    }

    override fun toString(): String {
        return "${this::class.java.simpleName} { $source $sendType $type to $target" +
                (if (replyToCounter > 0) " reply $replyToCounter" else "") + " event $event " +
                (if (dataStream != null) "data stream $dataStream (length $dataLength) " else "data $data ") +
                (if (sessionId?.isEmpty() == false) "session $sessionId" else "") + " }"
    }

    companion object {
        protected val MAGIC: Char = 0xfc.toChar()
        protected var staticCounter = AtomicLong(0)

        val VERSION: Byte = 0x01

        fun parse(data: ByteArray): ManifoldEvent? {
            ByteArrayInputStream(data).use { bytes ->
                DataInputStream(bytes).use { stream ->
                    return parse(stream).get()
                }
            }
        }

        private fun DataInputStream.readStringWithLength(isLongStr: Boolean = false, exceptionMessage: String = ""): String? {
            val len = if (isLongStr) this.readLong().toInt() else this.readUnsignedShort()

            if (len <= 0) {
                return null
            }

            val buf = ByteArray(len)
            val readLen = this.read(buf, 0, len)

            if (readLen != len) {
                throw IOException(exceptionMessage.replace("[len]", len.toString()).replace("[readLen]", readLen.toString()))
            }

            return String(buf)
        }

        fun parse(stream: DataInputStream): CompletableFuture<ManifoldEvent?> {
            val magic = stream.readChar()

            if (magic != MAGIC) {
                return CompletableFuture.completedFuture(null)
            }

            val version = stream.readByte()

            if (version > VERSION) {
                throw IOException("Current event version is $VERSION, but received version is $version")
            } else if (version < VERSION) {
                // TODO: Upgrade event data
            }

            val event = ManifoldEvent()
            event.counter = stream.readLong()
            event.replyToCounter = stream.readLong()
            event.type = EventType.values()[stream.readByte().toInt()]
            event.sendType = EventSendType.values()[stream.readByte().toInt()]

            event.event = stream.readStringWithLength(exceptionMessage = "Short read when parsing event: event name indicated length [len], read length [readLen]") ?: ""
            event.source = stream.readStringWithLength(exceptionMessage = "Short read when parsing event: event source indicated length [len], read length [readLen]") ?: ""
            event.target = stream.readStringWithLength(exceptionMessage = "Short read when parsing event: event target indicated length [len], read length [readLen]") ?: ""
            event.sessionId = stream.readStringWithLength(exceptionMessage = "Short read when parsing event: event session id indicated length [len], read length [readLen]")

            val isStream = stream.readByte() > 0
            val length = stream.readLong()

            if (length > 0) {
                if (isStream) {
                    event.dataLength = length
                    event.dataStream = stream
                } else {
                    val d = ByteArray(length.toInt())
                    val len = stream.read(d, 0, length.toInt())

                    if (len.toLong() != length) {
                        throw IOException("Short read when parsing event: indicated length $length, read length $len")
                    }

                    event.dataBytes = d
                    event.dataLength = d.size.toLong()
                    event.data = String(d, StandardCharsets.UTF_8)
                }
            } else {
                event.data = ""
            }

            return CompletableFuture.completedFuture(event)
        }
    }
}
