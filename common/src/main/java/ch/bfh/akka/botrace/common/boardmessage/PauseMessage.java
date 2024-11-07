/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

/**
 * Pauses of the race. The race can be resumed or terminated afterward.
 */
public record PauseMessage() implements Message { }
