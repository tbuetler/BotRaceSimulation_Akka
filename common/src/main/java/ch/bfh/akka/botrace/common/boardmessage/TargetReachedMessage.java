/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

/**
 * Signals that the bot reached the target. The
 * bot will no longer be moved (even if
 * {@link ch.bfh.akka.botrace.common.botmessage.ChosenDirectionMessage}s are received).
 */
public record TargetReachedMessage() implements Message { }
