/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

/**
 * Tells a bot that it is no longer registered for a race.
 * To become part of a new race, a bot must renew its
 * registration by sending a
 * {@link ch.bfh.akka.botrace.common.botmessage.RegisterMessage}.
 */
public record UnregisteredMessage() implements Message { }
