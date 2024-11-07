/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.botmessage;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * Queries the board for possible directions
 * and the current distance to the target.
 * @param botRef the reference to the bot sending this message.
 */
public record AvailableDirectionsRequestMessage(
    ActorRef<Message> botRef
) implements BotMessage {
    public AvailableDirectionsRequestMessage {
        Objects.requireNonNull(botRef, "botRef cannot be null");
    }
}
