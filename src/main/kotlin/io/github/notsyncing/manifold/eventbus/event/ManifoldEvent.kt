package io.github.notsyncing.manifold.eventbus.event

import com.alibaba.fastjson.JSON

import java.io.*
import java.nio.charset.StandardCharsets
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
    var data: String = ""

    init {
        this.counter = staticCounter.incrementAndGet()
    }

    constructor(event: String, data: String) : this() {
        this.data = data
        this.event = event
        this.type = EventType.Data
    }

    constructor(event: String, data: Any) : this(event, "") {
        this.data = JSON.toJSONString(data)
    }

    fun replyToCounter(counter: Long): ManifoldEvent {
        replyToCounter = counter
        return this
    }

    fun <T> getData(dataType: Class<T>): T {
        return JSON.parseObject(data, dataType)
    }

    @Throws(IOException::class)
    fun serialize(): ByteArray {
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { stream ->
                stream.writeChar(MAGIC.toInt())
                stream.writeByte(VERSION.toInt())
                stream.writeLong(counter)
                stream.writeLong(replyToCounter)
                stream.writeByte(type.ordinal)
                stream.writeByte(sendType.ordinal)

                var s: ByteArray

                s = event.toByteArray(StandardCharsets.UTF_8)
                stream.writeInt(s.size)
                stream.write(s)

                s = source.toByteArray(StandardCharsets.UTF_8)
                stream.writeInt(s.size)
                stream.write(s)

                if (!target.isEmpty()) {
                    s = target.toByteArray(StandardCharsets.UTF_8)
                    stream.writeInt(s.size)
                    stream.write(s)
                } else {
                    stream.writeInt(0)
                }

                s = data.toByteArray(StandardCharsets.UTF_8)

                stream.writeLong(s.size.toLong())
                stream.write(s)

                return bytes.toByteArray()
            }
        }
    }

    override fun toString(): String {
        return this.javaClass.simpleName + " { " + source + " " + sendType + " " + type + " to " + target +
                (if (replyToCounter > 0) " reply " + replyToCounter else "") + " event " + event + " data " + data + " }"
    }

    companion object {
        protected val MAGIC: Char = 0xfc.toChar()
        protected var staticCounter = AtomicLong(0)

        val VERSION: Byte = 0x01

        @Throws(IOException::class, ClassNotFoundException::class)
        fun parse(data: ByteArray): ManifoldEvent? {
            ByteArrayInputStream(data).use { bytes ->
                DataInputStream(bytes).use { stream ->
                    val magic = stream.readChar()

                    if (magic != MAGIC) {
                        return null
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

                    var en: ByteArray
                    var len: Int

                    val eventLength = stream.readInt()
                    en = ByteArray(eventLength)
                    len = stream.read(en, 0, eventLength)

                    if (len != eventLength) {
                        throw IOException("Short read when parsing event: event name indicated length $eventLength, read length $len")
                    }

                    event.event = String(en)

                    val eventSourceLength = stream.readInt()
                    en = ByteArray(eventSourceLength)
                    len = stream.read(en, 0, eventSourceLength)

                    if (len != eventSourceLength) {
                        throw IOException("Short read when parsing event: event source indicated length $eventSourceLength, read length $len")
                    }

                    event.source = String(en)

                    val eventTargetLength = stream.readInt()

                    if (eventTargetLength > 0) {
                        en = ByteArray(eventTargetLength)
                        len = stream.read(en, 0, eventTargetLength)

                        if (len != eventTargetLength) {
                            throw IOException("Short read when parsing event: event target indicated length $eventTargetLength, read length $len")
                        }

                        event.target = String(en)
                    }

                    val length = stream.readLong()
                    val d = ByteArray(length.toInt())

                    if (length > 0) {
                        len = stream.read(d, 0, length.toInt())

                        if (len.toLong() != length) {
                            throw IOException("Short read when parsing event: indicated length $length, read length $len")
                        }

                        event.data = String(d, StandardCharsets.UTF_8)
                    } else {
                        event.data = ""
                    }

                    return event
                }
            }
        }
    }
}
