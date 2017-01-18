#![cfg(test)]

use event_node::{EventType, EventSendType, ManifoldEvent};

#[test]
fn test_serialize() {
    let mut event = ManifoldEvent {
        counter: 1,
        reply_to_counter: 2,
        send_type: EventSendType::Groupcast,
        event_type: EventType::Data,
        event: "Test event".to_string(),
        source: "manifold.test.1".to_string(),
        target: "manifold.test.2".to_string(),
        session_id: None,
        data: "Test data".to_string(),
        data_length: 9
    };

    let serialized = event.serialize();
    let parsed_event = ManifoldEvent::parse(&serialized).unwrap();

    assert_eq!(event.counter, parsed_event.counter);
    assert_eq!(event.reply_to_counter, parsed_event.reply_to_counter);
    assert_eq!(event.send_type, parsed_event.send_type);
    assert_eq!(event.event_type, parsed_event.event_type);
    assert_eq!(event.event, parsed_event.event);
    assert_eq!(event.source, parsed_event.source);
    assert_eq!(event.target, parsed_event.target);
    assert_eq!(event.session_id, parsed_event.session_id);
    assert_eq!(event.data, parsed_event.data);
    assert_eq!(event.data_length, parsed_event.data_length);
}