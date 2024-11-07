/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.cli;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import ch.bfh.akka.botrace.board.actor.BoardRoot;
import ch.bfh.akka.botrace.board.actor.ClusterListener;

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

            // TODO Create listener for cluster domain events

            // TODO Create board root actor

            return Behaviors.empty();
        });
    }

}
