/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

/**
 * Pings the bot actor. The pot actor is expected to a
 * {@link ch.bfh.akka.botrace.common.botmessage.PingResponseMessage}.
 */
public record PingMessage() implements Message { }
