/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * Tells the bot that the previous
 * {@link ch.bfh.akka.botrace.common.botmessage.ChosenDirectionMessage} was ignored.
 * Typically, the reason is that the bot sent the
 * {@link ch.bfh.akka.botrace.common.botmessage.ChosenDirectionMessage} too
 * fast or that the bot is about to enter an obstacle.
 * @param reason indication of the reason for this message.
 */
public record ChosenDirectionIgnoredMessage(
    String reason) implements Message {
    public ChosenDirectionIgnoredMessage {
        Objects.requireNonNull(reason, "reason cannot be null");
    }
}
