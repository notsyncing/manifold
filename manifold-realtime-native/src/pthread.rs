use std::os::unix::thread::RawPthread;

#[repr(C)]
pub struct SchedParam {
    pub sched_priority: i32,
}

enum_from_primitive! {
    #[derive(Debug, PartialEq, Copy, Clone)]
    pub enum SchedPolicy {
        Fifo = 1,
        RoundRobin = 2,
        Other = 0
    }
}

#[link(name = "pthread")]
extern {
    fn pthread_setschedparam(target_thread: RawPthread, policy: i32, param: *const SchedParam) -> i32;
}

pub fn set_sched_param(target_thread: RawPthread, policy: SchedPolicy, param: &SchedParam) -> i32 {
    unsafe { pthread_setschedparam(target_thread, policy as i32, param as *const _) }
}
