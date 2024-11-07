/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

/**
 * Defines the sleep time between
 * {@link ch.bfh.akka.botrace.common.botmessage.ChosenDirectionMessage}'s from bots.
 * @param sleepTime the time to elapse in milliseconds.
 */
public record SetupMessage(
    int sleepTime) implements Message { }
