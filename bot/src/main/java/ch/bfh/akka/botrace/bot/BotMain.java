/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.bot;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

/**
 * The root actor of the Bot actor system.
 */
public class BotMain {

	/**
	 * Entry point for the Bot actor system.
	 * @param args not used
	 */
	public static void main(String[] args) {
		// Create an Akka system
		ActorSystem<Void> bot = ActorSystem.create(rootBehavior(), "ClusterSystem");
		bot.log().info("Player Actor System created");
	}

	/**
	 * Creates the two actors {@link ClusterListener} and {@link BotRoot}.
	 * @return a void behavior
	 */
	private static Behavior<Void> rootBehavior() {
		return Behaviors.setup(context -> {

			// Create an actor that handles cluster domain events
			context.spawn(ClusterListener.create(), "ClusterListener");

			context.spawn(BotRoot.create("CompetitorBot"), "CompetitorBot");

			// For the competition, only one bot actor will be allowed
			//context.spawn(BotRoot.create("aName..."), "aBot");

			return Behaviors.empty();
		});
	}
}
