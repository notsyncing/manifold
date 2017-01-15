use std::string::String;
use std::option::Option;
use std::sync::mpsc::channel;
use std::io::{Read, Write, Cursor};
use byteorder::{BigEndian, WriteBytesExt};

mod event_node {
    pub static MAGIC: i16 = 0xfc;
    pub static VERSION: i8 = 1;

    pub struct ManifoldEvent {
        counter: i64,
        reply_to_counter: i64,
        event_type: i8,
        send_type: i8,
        event: String,
        source: String,
        target: String,
        session_id: Option<String>,
        data: String,
        data_stream: Option<Read>,
        data_length: i64
    }

    impl ManifoldEvent {
        fn write_string_with_length(str: &Option<String>, buf: &Vec<u8>, is_long_string: bool) {
            let len = if str == None { 0 } else { str.unwrap().len() };

            if is_long_string {
                buf.write_u64::<BigEndian>(len)
            } else {
                buf.write_u16::<BigEndian>(len)
            }

            if len > 0 {
                for b in str.as_bytes() {
                    buf.write_u8(b);
                }
            }
        }

        pub fn serialize(&self) -> Vec<u8> {
            let mut buf = vec![];

            buf.write_u16::<BigEndian>(MAGIC);
            buf.write_u8(VERSION);
            buf.write_u64::<BigEndian>(self.counter);
            buf.write_u64::<BigEndian>(self.reply_to_counter);
            buf.write_u8(self.event_type);
            buf.write_u8(self.send_type);

            write_string_with_length(self.event, buf, false);
            write_string_with_length(self.source, buf, false);
            write_string_with_length(self.target, buf, false);
            write_string_with_length(self.session_id, buf, false);

            if data_stream != None {
                buf.write_u8(1);
                buf.write_u64::<BigEndian>(self.data_length);
            } else {
                buf.write_u8(0);
                write_string_with_length(Option(self.data), buf, true);
            }

            buf
        }

        fn read_string_with_length(reader: &Cursor<u8>, is_long_string: bool) -> String {
            let len = if is_long_string {
                buf.read_u64::<BigEndian>().unwrap();
            } else {
                buf.read_u16::<BigEndian>().unwrap();
            };

            if len > 0 {
                let mut str_buf = vec![];

                for i in 0..len {
                    str_buf.push(buf.read_u8().unwrap());
                }

                String::from_utf8(str_buf)
            } else {
                ""
            }
        }

        pub fn parse(data: &Vec<u8>) -> Result<ManifoldEvent> {
            let reader = Cursor::new(data);

            let magic = reader.read_u16::<BigEndian>();

            if magic != MAGIC {
                return Err(format!("Invalid magic {}", magic));
            }

            let version = reader.read_u8().unwrap();

            if version != VERSION {
                return Err(format!("Unsupported version {}", version));
            }

            let counter = reader.read_u64::<BigEndian>().unwrap();
            let reply_to_counter = reader.read_u64::<BigEndian>().unwrap();
            let event_type = reader.read_u8().unwrap();
            let send_type = reader.read_u8().unwrap();

            let event = read_string_with_length(reader, false);
            let source = read_string_with_length(reader, false);
            let target = read_string_with_length(reader, false);
            let session_id = read_string_with_length(reader, false);

            let is_stream = reader.read_u8().unwrap();

            let data = if is_stream > 0 {
                // TODO: Support stream
                ""
            } else {
                read_string_with_length(reader, true);
            };

            ManifoldEvent {
                counter: counter,
                reply_to_counter: reply_to_counter,
                event_type: event_type,
                send_type: send_type,
                event: event,
                source: source,
                target: target,
                session_id: if session_id.len() > 0 { Some(session_id) } else { None },
                data: data,
                data_stream: None,
                data_length: data.len()
            }
        }
    }

    pub struct ManifoldEventNode {
        name: String,
        groups: Vec<String>,
        load: i32,

        channels: (Sender<ManifoldEvent>, Receiver<ManifoldEvent>)
    }
}