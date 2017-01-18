use num::FromPrimitive;
use std::string::String;
use std::option::Option;
use std::sync::mpsc::{channel, Sender, Receiver};
use std::io::{Read, Write, Cursor, Error, ErrorKind};
use std::fmt::Debug;
use byteorder::{BigEndian, ReadBytesExt, WriteBytesExt};

pub static MAGIC: u16 = 0xfc;
pub static VERSION: u8 = 1;

enum_from_primitive! {
    #[derive(Debug, PartialEq, Copy, Clone)]
    pub enum EventSendType {
        Unicast = 0,
        Groupcast = 1,
        Broadcast = 2,
        GroupUnicast = 3,
        MultiGroupUnicast = 4
    }
}

enum_from_primitive! {
    #[derive(Debug, PartialEq, Copy, Clone)]
    pub enum EventType {
        Beacon = 0,
        Data = 1
    }
}

pub struct ManifoldEvent {
    pub counter: u64,
    pub reply_to_counter: u64,
    pub event_type: EventType,
    pub send_type: EventSendType,
    pub event: String,
    pub source: String,
    pub target: String,
    pub session_id: Option<String>,
    pub data: String,
    pub data_length: u64
}

impl ManifoldEvent {
    fn write_string_with_length(str: &Option<&String>, buf: &mut Vec<u8>, is_long_string: bool) {
        let len = if str.is_none() { 0 } else { str.unwrap().len() };

        if is_long_string {
            buf.write_u64::<BigEndian>(len as u64);
        } else {
            buf.write_u16::<BigEndian>(len as u16);
        }

        if len > 0 {
            for b in str.unwrap().as_bytes() {
                buf.write_u8(*b);
            }
        }
    }

    pub fn serialize(&self) -> Vec<u8> {
        let mut buf = vec![];

        buf.write_u16::<BigEndian>(MAGIC);
        buf.write_u8(VERSION);
        buf.write_u64::<BigEndian>(self.counter);
        buf.write_u64::<BigEndian>(self.reply_to_counter);
        buf.write_u8(self.event_type as u8);
        buf.write_u8(self.send_type as u8);

        ManifoldEvent::write_string_with_length(&Some(&self.event), &mut buf, false);
        ManifoldEvent::write_string_with_length(&Some(&self.source), &mut buf, false);
        ManifoldEvent::write_string_with_length(&Some(&self.target), &mut buf, false);
        ManifoldEvent::write_string_with_length(&self.session_id.as_ref(), &mut buf, false);

        buf.write_u8(0);
        ManifoldEvent::write_string_with_length(&Some(&self.data), &mut buf, true);

        buf
    }

    fn read_string_with_length(reader: &mut Read, is_long_string: bool) -> String {
        let len = if is_long_string {
            reader.read_u64::<BigEndian>().unwrap()
        } else {
            reader.read_u16::<BigEndian>().unwrap() as u64
        };

        if len > 0 {
            let mut str_buf = vec![];

            for i in 0..len {
                str_buf.push(reader.read_u8().unwrap());
            }

            String::from_utf8(str_buf).unwrap()
        } else {
            "".to_string()
        }
    }

    pub fn parse(data: &Vec<u8>) -> Result<ManifoldEvent, Error> {
        let mut reader = Cursor::new(data);

        let magic = reader.read_u16::<BigEndian>().unwrap();

        if magic != MAGIC {
            return Err(Error::new(ErrorKind::Other, format!("Invalid magic {}", magic)));
        }

        let version = reader.read_u8().unwrap();

        if version != VERSION {
            return Err(Error::new(ErrorKind::Other, format!("Unsupported version {}", version)));
        }

        let counter = reader.read_u64::<BigEndian>().unwrap();
        let reply_to_counter = reader.read_u64::<BigEndian>().unwrap();
        let event_type = EventType::from_u8(reader.read_u8().unwrap()).unwrap();
        let send_type = EventSendType::from_u8(reader.read_u8().unwrap()).unwrap();

        let event = ManifoldEvent::read_string_with_length(&mut reader, false);
        let source = ManifoldEvent::read_string_with_length(&mut reader, false);
        let target = ManifoldEvent::read_string_with_length(&mut reader, false);
        let session_id = ManifoldEvent::read_string_with_length(&mut reader, false);

        let is_stream = reader.read_u8().unwrap();

        let data = if is_stream > 0 {
            return Err(Error::new(ErrorKind::Other, "Stream as data is not supported!"))
        } else {
            ManifoldEvent::read_string_with_length(&mut reader, true)
        };

        let data_len = data.len();

        Ok(ManifoldEvent {
            counter: counter,
            reply_to_counter: reply_to_counter,
            event_type: event_type,
            send_type: send_type,
            event: event,
            source: source,
            target: target,
            session_id: if session_id.len() > 0 { Some(session_id) } else { None },
            data: data,
            data_length: data_len as u64
        })
    }
}

unsafe impl Send for ManifoldEvent {}

unsafe impl Sync for ManifoldEvent {}

pub struct ManifoldEventNode<'a> {
    pub name: String,
    pub groups: Vec<String>,
    pub load: i32,

    pub channel_tx: Sender<&'a ManifoldEvent>,
    pub channel_rx: Receiver<&'a ManifoldEvent>
}

impl<'a> ManifoldEventNode<'a> {
    pub fn new_with_groups(name: String, groups: Vec<String>) -> ManifoldEventNode<'a> {
        let (tx, rx) = channel();

        ManifoldEventNode {
            name: name,
            groups: groups,
            load: 0,

            channel_tx: tx,
            channel_rx: rx
        }
    }

    pub fn new(name: String) -> ManifoldEventNode<'a> {
        ManifoldEventNode::new_with_groups(name, vec![])
    }
}