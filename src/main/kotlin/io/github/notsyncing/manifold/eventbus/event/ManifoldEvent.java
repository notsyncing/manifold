package io.github.notsyncing.manifold.eventbus.event;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class ManifoldEvent<E extends Enum>
{
    protected static final char MAGIC = 0xfc;
    protected static AtomicLong staticCounter = new AtomicLong(0);

    public static final byte VERSION = 0x01;

    private long counter;
    private long replyToCounter;
    private EventType type;
    private EventSendType sendType;
    private E event;
    private String source;
    private String target;
    private String data;
    private Object rawData;

    public ManifoldEvent()
    {
        this.counter = staticCounter.incrementAndGet();
    }

    public ManifoldEvent(E event, String data)
    {
        this();

        this.data = data;
        this.event = event;
        this.type = EventType.Data;
    }

    public ManifoldEvent(E event, Object data)
    {
        this(event, null);

        this.rawData = data;
    }

    public long getCounter()
    {
        return counter;
    }

    public long getReplyToCounter()
    {
        return replyToCounter;
    }

    public void setReplyToCounter(long replyToCounter)
    {
        this.replyToCounter = replyToCounter;
    }

    public ManifoldEvent replyToCounter(long replyToCounter)
    {
        setReplyToCounter(replyToCounter);
        return this;
    }

    public EventType getType()
    {
        return type;
    }

    public void setType(EventType type)
    {
        this.type = type;
    }

    public EventSendType getSendType()
    {
        return sendType;
    }

    public void setSendType(EventSendType sendType)
    {
        this.sendType = sendType;
    }

    public E getEvent()
    {
        return event;
    }

    public void setEvent(E event)
    {
        this.event = event;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public String getData()
    {
        return data;
    }

    public <T> T getData(Class<T> dataType)
    {
        if (rawData != null) {
            return (T) rawData;
        } else {
            return JSON.parseObject(data, dataType);
        }
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public void setData(Object data)
    {
        this.rawData = data;
    }

    public byte[] serialize() throws IOException
    {
        if (data == null) {
            data = JSON.toJSONString(rawData);
        }

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream stream = new DataOutputStream(bytes)) {
            stream.writeChar(MAGIC);
            stream.writeByte(VERSION);
            stream.writeLong(counter);
            stream.writeLong(replyToCounter);
            stream.writeByte(type.ordinal());
            stream.writeByte(sendType.ordinal());

            byte[] s;

            String eventName = event.getClass().getName() + "." + event.name();
            s = eventName.getBytes(StandardCharsets.UTF_8);
            stream.writeInt(s.length);
            stream.write(s);

            s = source.getBytes(StandardCharsets.UTF_8);
            stream.writeInt(s.length);
            stream.write(s);

            if (target != null) {
                s = target.getBytes(StandardCharsets.UTF_8);
                stream.writeInt(s.length);
                stream.write(s);
            } else {
                stream.writeInt(0);
            }

            s = data.getBytes(StandardCharsets.UTF_8);

            stream.writeLong(s.length);
            stream.write(s);

            return bytes.toByteArray();
        }
    }

    public static ManifoldEvent parse(byte[] data) throws IOException, ClassNotFoundException
    {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             DataInputStream stream = new DataInputStream(bytes)) {
            char magic = stream.readChar();

            if (magic != MAGIC) {
                return null;
            }

            byte version = stream.readByte();

            if (version > VERSION) {
                throw new IOException("Current event version is " + VERSION + ", but received version is " + version);
            } else if (version < VERSION) {
                // TODO: Upgrade event data
            }

            ManifoldEvent event = new ManifoldEvent<>();
            event.counter = stream.readLong();
            event.replyToCounter = stream.readLong();
            event.type = EventType.values()[stream.readByte()];
            event.sendType = EventSendType.values()[stream.readByte()];

            byte[] en;
            int len;

            int eventNameLength = stream.readInt();
            en = new byte[eventNameLength];
            len = stream.read(en, 0, eventNameLength);

            if (len != eventNameLength) {
                throw new IOException("Short read when parsing event: event name indicated length " + eventNameLength + ", read length " + len);
            }

            String eventFullName = new String(en);
            Class eventClass = Class.forName(eventFullName.substring(0, eventFullName.lastIndexOf(".")));
            event.event = Enum.valueOf(eventClass, eventFullName.substring(eventFullName.lastIndexOf(".") + 1));

            int eventSourceLength = stream.readInt();
            en = new byte[eventSourceLength];
            len = stream.read(en, 0, eventSourceLength);

            if (len != eventSourceLength) {
                throw new IOException("Short read when parsing event: event source indicated length " + eventSourceLength + ", read length " + len);
            }

            event.source = new String(en);

            int eventTargetLength = stream.readInt();

            if (eventTargetLength > 0) {
                en = new byte[eventTargetLength];
                len = stream.read(en, 0, eventTargetLength);

                if (len != eventTargetLength) {
                    throw new IOException("Short read when parsing event: event target indicated length " + eventTargetLength + ", read length " + len);
                }

                event.target = new String(en);
            }

            long length = stream.readLong();
            byte[] d = new byte[(int) length];

            len = stream.read(d, 0, (int) length);

            if (len != length) {
                throw new IOException("Short read when parsing event: indicated length " + length + ", read length " + len);
            }

            event.data = new String(d, StandardCharsets.UTF_8);

            return event;
        }
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + " { " + source + " " + sendType + " " + type + " to " + target +
                (replyToCounter > 0 ? " reply " + replyToCounter : "") + " event " + event + " data " + data + " }";
    }
}
