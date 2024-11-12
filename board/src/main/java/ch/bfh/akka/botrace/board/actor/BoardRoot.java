/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import ch.bfh.akka.botrace.common.BoardService;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.common.botmessage.AvailableDirectionsRequestMessage;
import ch.bfh.akka.botrace.common.botmessage.ChosenDirectionMessage;
import ch.bfh.akka.botrace.common.botmessage.DeregisterMessage;
import ch.bfh.akka.botrace.common.botmessage.RegisterMessage;

/**
 * The root actor of the Board actor system. It registers itself at the {@link Receptionist}
 * under the service name {@link BoardService#SERVICE_NAME}.
 */
public class BoardRoot extends AbstractOnMessageBehavior<Message> { // root actor

	/** The service key for the board service {@link BoardService#SERVICE_NAME} actor system. */
	public final static ServiceKey<Message> SERVICE_KEY = ServiceKey.create(Message.class, BoardService.SERVICE_NAME);

	/**
	 * Factory method which creates the root actor of the board.
	 * @param board a board adapter
	 */
	public static Behavior<Message> create(Board board) {
		return Behaviors.setup(context -> new BoardRoot(context, board));
	}

	/**
	 * Upon creation, it registers its actor reference under the service name
	 * {@value BoardService#SERVICE_NAME} at the {@link Receptionist}.
	 *
	 * @param context context of the actor system.
	 * @param board adapter for the board model.
	 */
	private BoardRoot(ActorContext<Message> context, Board board) {
		super(context);
		// TODO Initialize...
		context.getLog().info(getClass().getSimpleName() + " created: " + this.getContext().getSelf());

		// Register the board root actor at the receptionist
		context
				.getSystem()
				.receptionist()
				.tell(Receptionist.register(SERVICE_KEY, this.getContext().getSelf().narrow()));
		context.getLog().info(getClass().getSimpleName() + ": Registered at the receptionist");
	}

	/**
	 * Handles the received messages.
	 * @param message a message.
	 * @return the same behavior.
	 */
	@Override
	public Behavior<Message> onMessage(Message message) {

		switch (message){
			case DeregisterMessage deregisterMessage 									-> onDeregisterMessage(deregisterMessage);
			case RegisterMessage registerMessage 										-> onRegisterMessage(registerMessage);
			case AvailableDirectionsRequestMessage availableDirectionsRequestMessage 	-> onAvailableDirectionsRequestMessage(availableDirectionsRequestMessage);
			case ChosenDirectionMessage chosenDirectionMessage 							-> onChosenDirectionMessage(chosenDirectionMessage);

            default -> throw new IllegalStateException("Message not handled: " + message);
        };

		return Behaviors.same();
	}

	Behavior<Message> onDeregisterMessage(DeregisterMessage deregisterMessage) {
		getContext().getLog().info("Deregistering because: {}", deregisterMessage.reason());

		//Todo Implement handling
		return this;
	}

	Behavior<Message> onRegisterMessage(RegisterMessage registerMessage) {
		getContext().getLog().info("Registering: {}", registerMessage.name());

		//Todo Implement handling
		return this;
	}

	Behavior<Message> onAvailableDirectionsRequestMessage(AvailableDirectionsRequestMessage availableDirectionsRequestMessage) {
		getContext().getLog().info("Requesting available directions");

		//Todo Implement handling
		return this;
	}

	Behavior<Message> onChosenDirectionMessage(ChosenDirectionMessage chosenDirectionMessage) {
		getContext().getLog().info("Choosing direction: {}", chosenDirectionMessage.chosenDirection());

		//Todo Implement handling
		return this;
	}
}
