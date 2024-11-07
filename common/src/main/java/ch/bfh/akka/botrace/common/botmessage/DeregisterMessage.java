/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.botmessage;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * De-registers a bot from the board. The bot is no longer
 * interested in this race.
 * @param reason an indication of a reason.
 * @param botRef the reference to the bot sending this message.
 */
public record DeregisterMessage(
        String reason,
        ActorRef<Message> botRef) implements BotMessage {
    public DeregisterMessage {
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(botRef, "botRef cannot be null");
    }
}
