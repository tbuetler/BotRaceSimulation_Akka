/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.botmessage;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * Registers a bot with the board. The bot provides a name and
 * an actor ref to enable the board to send back an answer.
 * @param name the name of the bot.
 * @param botRef the reference to the bot sending this message.
 */
public record RegisterMessage(
    String name,
    ActorRef<Message> botRef) implements Message {
    public RegisterMessage {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(botRef, "botRef cannot be null");
    }
}
