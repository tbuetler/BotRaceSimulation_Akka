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
import ch.bfh.akka.botrace.board.model.BoardModel;
import ch.bfh.akka.botrace.common.*;
import ch.bfh.akka.botrace.common.boardmessage.*;
import ch.bfh.akka.botrace.common.botmessage.*;

import java.util.List;

/**
 * The root actor of the Board actor system. It registers itself at the {@link Receptionist}
 * under the service name {@link BoardService#SERVICE_NAME}.
 */
public class BoardRoot extends AbstractOnMessageBehavior<Message> { // root actor

	/** The service key for the board service {@link BoardService#SERVICE_NAME} actor system. */
	public final static ServiceKey<Message> SERVICE_KEY = ServiceKey.create(Message.class, BoardService.SERVICE_NAME);

	private final BoardModel boardModel;

	/**
	 * Factory method which creates the root actor of the board.
	 * @param board a board adapter
	 */
	public static Behavior<Message> create(BoardModel board) {
		return Behaviors.setup(context -> new BoardRoot(context, board));
	}

	/**
	 * Upon creation, it registers its actor reference under the service name
	 * {@value BoardService#SERVICE_NAME} at the {@link Receptionist}.
	 *
	 * @param context context of the actor system.
	 * @param board adapter for the board model.
	 */
	private BoardRoot(ActorContext<Message> context, BoardModel board) {
		super(context);
		boardModel = board;

		// TODO: Maby there are more things to initialize in constructor, idk yet
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
			case StartMessage ignored -> onStartGame();
			case PauseMessage ignored -> onPauseGame();
			case ResumeMessage ignored -> onResumeGame();
			case PingResponseMessage pingResponseMessage								-> onPingResponseMessage(pingResponseMessage);
			case DeregisterMessage deregisterMessage 									-> onDeregisterMessage(deregisterMessage);
			case RegisterMessage registerMessage 										-> onRegisterMessage(registerMessage);
			case AvailableDirectionsRequestMessage availableDirectionsRequestMessage 	-> onAvailableDirectionsRequestMessage(availableDirectionsRequestMessage);
			case ChosenDirectionMessage chosenDirectionMessage 							-> onChosenDirectionMessage(chosenDirectionMessage);

            default -> throw new IllegalStateException("Message not handled: " + message);
        };

		return Behaviors.same();
	}

	Behavior<Message> onStartGame() {
		getContext().getLog().info("Game started");
		return this;
	}

	Behavior<Message> onPauseGame() {
		getContext().getLog().info("Game paused");
		return this;
	}

	Behavior<Message> onResumeGame() {
		getContext().getLog().info("Game resumed");
		return this;
	}

	Behavior<Message> onPingResponseMessage(PingResponseMessage pingResponseMessage) {
		getContext().getLog().info("Ping recieved");

		//TODO: Dont know how to implement it yet
		return this;
	}

	Behavior<Message> onDeregisterMessage(DeregisterMessage deregisterMessage) {
		getContext().getLog().info("Deregistering because: {}", deregisterMessage.reason());
		// unregistering from boardModel
		boardModel.deregister(deregisterMessage.botRef());
		return this;
	}

	Behavior<Message> onRegisterMessage(RegisterMessage registerMessage) {
		getContext().getLog().info("Registering: {}", registerMessage.name());
		// adding to boardModel
		boardModel.registerNewBot(registerMessage.name(), registerMessage.botRef());

		//TODO: What is the sleepTime parameter @SetUpMessage() ??? I just put 600 --> needs to be adjusted

		// sending SetupMessage back to bot
		registerMessage.botRef().tell(new SetupMessage(600));
		return this;
	}

	Behavior<Message> onAvailableDirectionsRequestMessage(AvailableDirectionsRequestMessage availableDirectionsRequestMessage) {
		getContext().getLog().info("Requesting available directions");

		//getting data from boardmodel
		List<Direction> directions = boardModel.getAvailableDirection(availableDirectionsRequestMessage.botRef());
		int distance = boardModel.getDistanceToTarget(availableDirectionsRequestMessage.botRef());

		//sending Message back to Bot
		availableDirectionsRequestMessage.botRef().tell(new AvailableDirectionsReplyMessage(directions, distance));

		return this;
	}

	Behavior<Message> onChosenDirectionMessage(ChosenDirectionMessage chosenDirectionMessage) {

		// Try to play move
		if(boardModel.playChosenDirection(chosenDirectionMessage.chosenDirection(),chosenDirectionMessage.botRef())){
			getContext().getLog().info("Move Played: {}", chosenDirectionMessage.chosenDirection());
		}
		// Send ChosenDirectionIgnoredMessage if failed to play move
		else{
			getContext().getLog().info("Move Failed: {}", chosenDirectionMessage.chosenDirection());
			chosenDirectionMessage.botRef().tell(new ChosenDirectionIgnoredMessage("Move not Possible"));
		}
		return this;
	}
}
