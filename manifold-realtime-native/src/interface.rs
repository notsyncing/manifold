use std::thread::{spawn, JoinHandle};
use std::sync::mpsc::{channel, Sender, Receiver};
use concurrent_hashmap::*;
use event_node::*;
use worker::*;

struct ManifoldRTInterface<'a, 'b> {
    workers: Vec<&'a ManifoldWorker<'a>>,
    recv_callback: Option<Fn(&Vec<u8>) -> ()>,

    event_handler_stop: bool,
    event_handler_thread: Option<JoinHandle<()>>,

    event_node_by_name: ConcHashMap<String, &'a ManifoldEventNode<'a>>,
    event_node_by_group: ConcHashMap<String, Vec<&'a ManifoldEventNode<'a>>>,

    event_channel_tx: Sender<&'b Vec<u8>>,
    event_channel_rx: Receiver<&'b Vec<u8>>
}

impl<'a, 'b> ManifoldRTInterface<'a, 'b> {
    fn new() -> ManifoldRTInterface<'a, 'b> {
        let (tx, rx) = channel();

        let intf = ManifoldRTInterface {
            workers: vec![],
            recv_callback: None,

            event_handler_stop: false,
            event_handler_thread: None,

            event_node_by_name: ConcHashMap::new(),
            event_node_by_group: ConcHashMap::new(),

            event_channel_tx: tx,
            event_channel_rx: rx
        };

        let thread = spawn(|| {
            while !intf.event_handler_stop {
                let buf = intf.event_channel_rx.recv().unwrap();
                let event_result = ManifoldEvent::parse(buf);

                if event_result.is_err() {
                    continue
                }

                let event = event_result.unwrap();

                if event.send_type == EventSendType::Unicast {
                    if let Some(mut node) = intf.event_node_by_name.find_mut(&event.target) {
                        node.get().channel_tx.send(&event);
                    }
                } else if event.send_type == EventSendType::Groupcast {
                    if let Some(mut list) = intf.event_node_by_group.find_mut(&event.target) {
                        for node in list.get() {
                            node.channel_tx.send(&event);
                        }
                    }
                } else if event.send_type == EventSendType::GroupUnicast {
                    // TODO: Support this!
                } else if event.send_type == EventSendType::Broadcast {
                    for w in intf.workers {
                        w.event_node.channel_tx.send(&event);
                    }
                }
            }
        });

        intf.event_handler_thread = thread;

        intf
    }
}

static mut INTERFACE: Option<&'static ManifoldRTInterface<'static, 'static>> = None;

pub fn start() {
    if INTERFACE.is_none() {
        INTERFACE = Ok(&ManifoldRTInterface::new())
    }
}

pub fn stop() {
    if let Some(intf) = INTERFACE {
        intf.event_handler_stop = true;
        intf.event_handler_thread.join()
    }
}

pub fn send(data: Vec<u8>) {
    if let Some(intf) = INTERFACE {
        intf.event_channel_tx.send(&data)
    }
}

pub fn set_recv_callback(handler: Fn(&Vec<u8>) -> ()) {
    if let Some(intf) = INTERFACE {
        intf.recv_callback = Ok(handler)
    }
}

pub fn clear_recv_callback() {
    if let Some(intf) = INTERFACE {
        intf.recv_callback = None
    }
}

pub fn register_worker(worker: &ManifoldWorker) {
    if let Some(intf) = INTERFACE {
        intf.workers.push(worker)
    }
}

pub fn register_event_node(node: &ManifoldEventNode) {
    if let Some(intf) = INTERFACE {
        let name = node.name;
        intf.event_node_by_name.insert(name, node);

        for g in node.groups {
            if let Some(mut list) = intf.event_node_by_group.find_mut(&name) {
                list.get().push(node)
            } else {
                intf.event_node_by_group.insert(name, vec![node])
            }
        }
    }
}