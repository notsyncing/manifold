use std::thread;
use std::thread::{Thread, JoinHandle};
use std::os::unix::thread::{JoinHandleExt, RawPthread};
use std::io::{Error, ErrorKind};
use event_node::ManifoldEventNode;
use pthread::*;
use interface;

pub struct ManifoldWorker<'a> {
    thread: Option<&'a JoinHandle<()>>,
    pub event_node: ManifoldEventNode<'a>,
}

impl<'a> ManifoldWorker<'a> {
    pub fn new<F>(event_node_name: String, event_node_groups: Vec<String>, block: F) -> ManifoldWorker<'a>
        where F: Fn(&ManifoldWorker) -> (), F: Send + 'static {
        let event_node = ManifoldEventNode::new(event_node_name, event_node_groups);

        let worker = ManifoldWorker {
            thread: None,
            event_node: event_node,
        };

        let thread = thread::spawn(|| { block(&worker) });

        interface::register_worker(&worker);
        interface::register_event_node(&worker.event_node);

        worker.thread = Some(&thread);

        worker
    }

    fn set_priority(&self, sched_policy: SchedPolicy, priority: i32) -> Result<bool, Error> {
        let h = self.thread.as_pthread_t();
        let param = SchedParam {
            sched_priority: priority
        };

        let r = set_sched_param(h, sched_policy, &param);

        if r >= 0 {
            Ok(true)
        } else {
            Err(Error::new(ErrorKind::Other,
                      format!("Failed to set thread scheduler priority to {} {}: {}",
                              sched_policy as i32, priority, r)))
        }
    }

    pub fn realtime(&self, priority: i32) -> Result<bool, Error> {
        self.set_priority(SchedPolicy::Fifo, priority)
    }

    pub fn normal(&self, priority: i32) -> Result<bool, Error> {
        self.set_priority(SchedPolicy::Other, priority)
    }
}