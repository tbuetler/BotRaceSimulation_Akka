/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.botmessage;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * The response message sent by a bot having received the
 * {@link ch.bfh.akka.botrace.common.boardmessage.PingMessage}.
 * @param name the name of the bot.
 * @param botRef its actor reference.
 */
public record PingResponseMessage(String name,
                                  ActorRef<Message> botRef) implements BotMessage {
    public PingResponseMessage {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(botRef, "botRef cannot be null");
    }
}
