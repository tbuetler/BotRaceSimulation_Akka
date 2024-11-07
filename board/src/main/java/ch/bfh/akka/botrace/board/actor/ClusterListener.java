/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;

/**
 * A listener for cluster events.
 * See
 * <a href="https://doc.akka.io/docs/akka/current/typed/index-cluster.html">Akka's Cluster</a>
 * documentation.
 */
public final class ClusterListener extends AbstractBehavior<ClusterListener.Event> {

	/**
	 * Common interface.
	 */
	public interface Event {
	}

	/**
	 * Internal adapted cluster events only.
	 */
	private record ReachabilityChange(ClusterEvent.ReachabilityEvent reachabilityEvent) implements Event {}

	/**
	 * Member change events.
	 */
	private record MemberChange(ClusterEvent.MemberEvent memberEvent) implements Event {}

	/**
	 * Factory method to create the cluster listener actor.
	 * @return a behavior
	 */
	public static Behavior<Event> create() {
		return Behaviors.setup(ClusterListener::new);
	}

	/**
	 * Creates this instance.
	 * @param context the actor system context
	 */
	private ClusterListener(ActorContext<Event> context) {
		super(context);

		Cluster cluster = Cluster.get(context.getSystem());

		ActorRef<ClusterEvent.MemberEvent> memberEventAdapter = context
				.messageAdapter(ClusterEvent.MemberEvent.class, MemberChange::new);
		cluster.subscriptions().tell(Subscribe.create(memberEventAdapter, ClusterEvent.MemberEvent.class));

		ActorRef<ClusterEvent.ReachabilityEvent> reachabilityAdapter = context
				.messageAdapter(ClusterEvent.ReachabilityEvent.class, ReachabilityChange::new);
		cluster.subscriptions().tell(Subscribe.create(reachabilityAdapter, ClusterEvent.ReachabilityEvent.class));

		context.getLog().info(getClass().getSimpleName() + ": Created");
	}

	/**
	 * Handles the reception of event messages.
	 * @return an {@link Receive} instance
	 */
	@Override
	public Receive<Event> createReceive() {
		return newReceiveBuilder()
				.onMessage(ReachabilityChange.class, this::onReachabilityChange)
				.onMessage(MemberChange.class, this::onMemberChange)
				.build();
	}

	/**
	 * Handles the reachability change event.
	 * @param event the event
	 * @return a behavior
	 */
	private Behavior<Event> onReachabilityChange(ReachabilityChange event) {
		if (event.reachabilityEvent instanceof ClusterEvent.UnreachableMember) {
			getContext().getLog().info("Member detected as unreachable: {}", event.reachabilityEvent.member());
		} else if (event.reachabilityEvent instanceof ClusterEvent.ReachableMember) {
			getContext().getLog().info("Member back to reachable: {}", event.reachabilityEvent.member());
		}
		return this;
	}

	/**
	 * Handles the member change event.
	 * @param event the event
	 * @return a behavior
	 */
	private Behavior<Event> onMemberChange(MemberChange event) {
		if (event.memberEvent instanceof ClusterEvent.MemberUp) {
			getContext().getLog().info("Member is up: {}", event.memberEvent.member());
		} else if (event.memberEvent instanceof ClusterEvent.MemberRemoved) {
			getContext()
					.getLog()
					.info("Member is removed: {} after {}", event.memberEvent.member(),
							((ClusterEvent.MemberRemoved) event.memberEvent).previousStatus());
		}
		return this;
	}
}
