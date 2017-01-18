#![crate_name = "manifold_rt"]
#![crate_type = "lib"]

#[macro_use]
extern crate enum_primitive;
extern crate num;
extern crate byteorder;
extern crate libc;
extern crate concurrent_hashmap;

mod pthread;

pub mod event_node;
pub mod worker;
pub mod interface;

#[cfg(test)]
#[path = "tests/event_tests.rs"]
mod event_tests;