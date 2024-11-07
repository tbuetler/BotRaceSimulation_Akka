/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common;

/**
 * Marker trait to tell Akka to serialize messages into CBOR using Jackson for sending over the network
 * See application.conf where it is bound to a serializer. For more details see
 * <a href="https://doc.akka.io/docs/akka/current/serialization-jackson.html">Serialization with Jackson</a>
 */
public interface CborSerializable { }
