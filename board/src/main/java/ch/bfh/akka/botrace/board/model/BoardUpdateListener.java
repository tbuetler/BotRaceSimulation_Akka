package ch.bfh.akka.botrace.board.model;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.common.Message;

public interface BoardUpdateListener {
    void boardUpdated();
    void notifyTargetReached(String name);
}
