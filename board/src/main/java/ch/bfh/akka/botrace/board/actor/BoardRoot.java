/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import ch.bfh.akka.botrace.board.model.BoardModel;
import ch.bfh.akka.botrace.board.model.BoardUpdateListener;
import ch.bfh.akka.botrace.common.BoardService;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.common.boardmessage.*;
import ch.bfh.akka.botrace.common.botmessage.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * The root actor of the Board actor system. It registers itself at the {@link Receptionist}
 * under the service name {@link BoardService#SERVICE_NAME}.
 */
public class BoardRoot extends AbstractOnMessageBehavior<Message> { // root actor

	public record TimerMessage() implements Message { }

	private final TimerScheduler<Message> timers;

	/** The service key for the board service {@link BoardService#SERVICE_NAME} actor system. */
	public final static ServiceKey<Message> SERVICE_KEY = ServiceKey.create(Message.class, BoardService.SERVICE_NAME);

	BoardModel boardModel;

	/**
	 * Factory method which creates the root actor of the board.
	 * @param board a board adapter
	 */
	public static Behavior<Message> create(BoardModel board) {
		return Behaviors.setup(ctx -> Behaviors.withTimers(timers -> new BoardRoot(ctx, timers, board)));
	}

	/**
	 * Upon creation, it registers its actor reference under the service name
	 * {@value BoardService#SERVICE_NAME} at the {@link Receptionist}.
	 *
	 * @param context context of the actor system.
	 * @param board adapter for the board model.
	 */
	private BoardRoot(ActorContext<Message> context, TimerScheduler<Message> timers, BoardModel board) {
		super(context);
		boardModel = board;
		this.timers = timers;

		// TODO: Maby there are more things to initialize in constructor, idk yet
		context.getLog().info(getClass().getSimpleName() + " created: " + this.getContext().getSelf());

		// Register the board root actor at the receptionist
		context
				.getSystem()
				.receptionist()
				.tell(Receptionist.register(SERVICE_KEY, this.getContext().getSelf().narrow()));
		context.getLog().info(getClass().getSimpleName() + ": Registered at the receptionist");

		timers.startTimerAtFixedRate(new TimerMessage(), Duration.ofSeconds(5));
	}


	/*TODO: sennd startMessage to all bots if the user presses start in the GUI
	   startMessage sends the parameter speed.
	 */


	private List<BoardUpdateListener> listeners = new ArrayList<>();

	// Register a listener
	public void addBoardUpdateListener(BoardUpdateListener listener) {
		listeners.add(listener);
	}

	// Notify all listeners of an update
	public void notifyBoardUpdate() {
		for (BoardUpdateListener listener : listeners) {
			listener.boardUpdated();
		}
	}

	private void notifyTargetReached(String name) {
		for (BoardUpdateListener listener : listeners) {
			listener.notifyTargetReached(name);
		}
	}


	/**
	 * Handles the received messages.
	 * @param message a message.
	 * @return the same behavior.
	 */
	@Override
	public Behavior<Message> onMessage(Message message) {

		switch (message){
			case PingResponseMessage pingResponseMessage								-> onPingResponseMessage(pingResponseMessage);
			case DeregisterMessage deregisterMessage 									-> onDeregisterMessage(deregisterMessage);
			case RegisterMessage registerMessage 										-> onRegisterMessage(registerMessage);
			case AvailableDirectionsRequestMessage availableDirectionsRequestMessage 	-> onAvailableDirectionsRequestMessage(availableDirectionsRequestMessage);
			case ChosenDirectionMessage chosenDirectionMessage 							-> onChosenDirectionMessage(chosenDirectionMessage);
			case StartRaceMessage startRaceMessage										-> onStartRaceMessage(startRaceMessage);

			// Events coming from user input sending to all bots
			case PauseMessage pauseMessage 												-> onPauseMessage(pauseMessage);
			case ResumeMessage resumeMessage											-> onResumeMessage(resumeMessage);

			case TimerMessage ignored                                                  -> onTimerMessage();
			case TimeoutMessage timeoutMessage 										   -> onTimeout(timeoutMessage);

            default -> throw new IllegalStateException("Message not handled: " + message);
        };

		return Behaviors.same();
	}

	private Behavior<Message> onPauseMessage(PauseMessage pauseMessage) {
		getContext().getLog().info("Pausing Race");

		for(ActorRef<Message> ref : boardModel.getBots()){
			ref.tell(new PauseMessage());
		}

		return this;
	}

	private Behavior<Message> onTimerMessage() {

		for (ActorRef<Message> ref : boardModel.getBots()) {
			getContext().getLog().info("Sending ping to " + ref.path().name());
			ref.tell(new PingMessage());
			String key = "timeout_" + ref.path().name(); //key for each bot
			getContext().getLog().info("Setting timeout for " + ref.path().name());
			timers.startSingleTimer(key, new TimeoutMessage(ref), Duration.ofSeconds(5));
		}
		return this;
	}

	private Behavior<Message> onResumeMessage(ResumeMessage resumeMessage) {
		getContext().getLog().info("Starting race");

		for(ActorRef<Message> ref : boardModel.getBots()){
			ref.tell(new ResumeMessage());
		}
		return this;
	}

	Behavior<Message> onPingResponseMessage(PingResponseMessage pingResponseMessage) {
		getContext().getLog().info("Ping response received from " + pingResponseMessage.name());

		// cancel the timeout when a response is received
		String key = "timeout_" + pingResponseMessage.name();
		timers.cancel(key);
		return this;
	}

	Behavior<Message> onDeregisterMessage(DeregisterMessage message) {
		getContext().getLog().info("Deregistering because: {}", message.reason());
		// unregistering from boardModel
		boardModel.deregister(message.botRef());
		message.botRef().tell(new UnregisteredMessage());

		return this;
	}

	Behavior<Message> onStartRaceMessage(StartRaceMessage startRaceMessage) {
		getContext().getLog().info("Starting race");

		for(ActorRef<Message> ref : boardModel.getBots()){
			ref.tell(new StartMessage());
		}
		return this;
	}

	Behavior<Message> onRegisterMessage(RegisterMessage message) {
		getContext().getLog().info("Registering: {}", message.name());
		// adding to boardModel
		boardModel.registerNewBot(message.name(), message.botRef());

		// ping the bot
		message.botRef().tell(new PingMessage());

		//TODO: What is the sleepTime parameter @SetUpMessage() ??? I just put 600 --> needs to be adjusted

		// sending SetupMessage back to bot
		message.botRef().tell(new SetupMessage(600));
		return this;
	}

	Behavior<Message> onAvailableDirectionsRequestMessage(AvailableDirectionsRequestMessage message) {
		getContext().getLog().info("Requesting available directions");

		//getting data from boardmodel
		List<Direction> directions = boardModel.getAvailableDirection(message.botRef());
		int distance = boardModel.getDistanceToTarget(message.botRef());

		//sending Message back to Bot
		message.botRef().tell(new AvailableDirectionsReplyMessage(directions, distance));

		return this;
	}

	Behavior<Message> onChosenDirectionMessage(ChosenDirectionMessage message) {

		// Try to play move
		if(boardModel.playChosenDirection(message.chosenDirection(),message.botRef())){
			getContext().getLog().info("Move Played: {}", message.chosenDirection());

			// if bot finished -> new TargetReachedMessage
			if(boardModel.checkIfBotFinished(message.botRef())){
				message.botRef().tell(new TargetReachedMessage());
				notifyTargetReached(boardModel.getPlayerName().get(message.botRef()));
				getContext().getLog().info("Target has been reached by: " + boardModel.getPlayerName().get(message.botRef()));
			} else {
				notifyBoardUpdate();
			}
		}
		// Send ChosenDirectionIgnoredMessage if failed to play move
		else{
			getContext().getLog().info("Move Failed: {}", message.chosenDirection());
			message.botRef().tell(new ChosenDirectionIgnoredMessage("Move not Possible"));
		}
		return this;
	}

	private Behavior<Message> onTimeout(TimeoutMessage timeoutMessage) {
		getContext().getLog().info("Timeout reached for " + timeoutMessage.botRef().path().name());
		getContext().getLog().info("Deregistering bot " + timeoutMessage.botRef().path().name());
		boardModel.deregister(timeoutMessage.botRef());
		return this;
	}
}
