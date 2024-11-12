/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.actor;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;

import java.util.List;

/**
 * Abstraction of the board, the playground. To be used by the actors. To
 * be implemented by, say, the model of the board.
 */
public interface Board {

    void registerNewBot(String name, ActorRef <Message> botRef);
    boolean playChosenDirection(Direction direction, ActorRef<Message> botRef);
    List<Direction> getAvailableDirection(ActorRef<Message> botRef);
    boolean checkIfBotFinished(ActorRef<Message> botRef);
    boolean isMoveValid(ActorRef<Message> botRef, Direction direction);
}
