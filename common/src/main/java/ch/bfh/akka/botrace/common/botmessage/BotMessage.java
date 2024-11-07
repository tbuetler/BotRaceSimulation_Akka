/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.botmessage;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Message;

/**
 * Messages sent from bots containing the bot actor reference.
 */
public interface BotMessage extends Message {
    /**
     * Return the actor reference of the bot having sent this message.
     * @return the actor reference of the bot having sent this message.
     */
    ActorRef<Message> botRef();
}
