/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.botmessage;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * Tells the direction to take for the next step. Note that no confirmation
 * is sent back to the respective bot, except if the interval of two or
 * more consecutive messages is smaller than the <code>sleepTime</code>
 * specified in the {@link ch.bfh.akka.botrace.common.boardmessage.SetupMessage}.
 * @param chosenDirection the direction for the next step.
 * @param botRef the reference to the bot sending this message.
 */
public record ChosenDirectionMessage(
    Direction chosenDirection,
    ActorRef<Message> botRef) implements BotMessage {
    public ChosenDirectionMessage {
        Objects.requireNonNull(chosenDirection, "chosenDirection cannot be null");
        Objects.requireNonNull(botRef, "botRef cannot be null");
    }
    @Override
    public String toString() {
        return "ChosenDirectionMessage{"
                + "chosenDirection=" + chosenDirection
                + ", botRef=" + botRef + '}';
    }
}
