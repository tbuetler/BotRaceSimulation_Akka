/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.cli;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import ch.bfh.akka.botrace.board.actor.Board;
import ch.bfh.akka.botrace.board.actor.BoardRoot;
import ch.bfh.akka.botrace.board.actor.ClusterListener;
import ch.bfh.akka.botrace.common.Message;

public class BoardMain {
    /**
     * Entry point for the Board actor system.
     * @param args not used
     */
    public static void main(String[] args) {
        // Create the board Akka system with initial actors.
        ActorSystem<Void> board = ActorSystem.create(rootBehavior(), "ClusterSystem");
        board.log().info("Board Actor System created");
    }

    /**
     * Creates the two actors {@link ClusterListener} and {@link BoardRoot}.
     * @return a void behavior
     */
    private static Behavior<Void> rootBehavior() {
        return Behaviors.setup(context -> {

            context.spawn(ClusterListener.create(),"ClusterListener");
            context.spawn(BoardRoot.create(new Board() {}), "BoardRoot");

            return Behaviors.empty();
        });
    }

}
