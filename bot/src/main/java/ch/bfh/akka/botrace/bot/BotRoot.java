/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.bot;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.Receptionist.Listing;
import akka.actor.typed.receptionist.ServiceKey;
import ch.bfh.akka.botrace.common.BoardService;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.common.boardmessage.*;
import ch.bfh.akka.botrace.common.boardmessage.PingMessage;
import ch.bfh.akka.botrace.common.botmessage.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 * The root actor of the Bot actor system.
 */



public class BotRoot extends AbstractOnMessageBehavior<Message> { // guardian actor

	public record TimerMessagePlay() implements Message { }
	public record TimerMessageRegistering() implements Message { }
	public record TimerMessageReached() implements Message { }
	static final String TIMER_KEY_PLAY = "PlayTimer";
	static final String TIMER_KEY_REGISTER = "RegisterTimer";
	static final String TIMER_KEY_REACHED = "ReachedTimer";

    private final TimerScheduler<Message> timers;
    private int sleepTime;

    /**
     * The service key instance to lookup for the service name {@link BoardService#SERVICE_NAME} of the board.
     */
    final static ServiceKey<Message> serviceKeyForBoard = ServiceKey
            .create(Message.class, BoardService.SERVICE_NAME);

    /**
     * The response message of the {@link Receptionist}.
     */
    record ListingResponse(Listing listing) implements Message {}

    /**
     * Factory method to create the bot root actor.
     *
     * @return a behavior.
     */
    public static Behavior<Message> create(String botName) {
        return Behaviors.setup(ctx -> Behaviors.withTimers(timers -> new BotRoot(ctx, timers, botName)));
    }

    private enum Phase{
        REGISTERING, READY, PLAYING, PAUSED, TARGET_REACHED
    }
    private Phase currentPhase;
    private int moveCount;
    List<Direction> recentDirections;
    ArrayList<Integer> recentDistances;

    /**
     * Upon creation of the Bot root actor, it tells the {@link Receptionist} its
     * interest in receiving a list of services.
     *
     * @param context the context of this actor system
     */
    private BotRoot(ActorContext<Message> context, TimerScheduler<Message> timers, String botName) {
        super(context);
        this.timers = timers;
        recentDirections = new ArrayList<>();
        this.recentDistances = new ArrayList<>();

        this.moveCount = 0;
        this.currentPhase = Phase.REGISTERING;

        ActorRef<Listing> listingResponseAdapter = context
                .messageAdapter(Listing.class, ListingResponse::new);
        context.getSystem().receptionist().tell(Receptionist.subscribe(serviceKeyForBoard, listingResponseAdapter));
    }

    private ActorRef<Message> boardRef;
    private final String actorName = getContext().getSelf().path().name();
    private final ActorRef<Message> botRef = getContext().getSelf();

    /**
     * Handle incoming messages.
     * @param message a message
     * @return the same behavior
     */
	@Override
	public Behavior<Message> onMessage(Message message) {
		if (currentPhase == Phase.TARGET_REACHED) {
			// Stop further processing of messages once the target is reached
			return this;
		}

        return switch(message){
            case PingMessage ignored                                                   -> onPing();
            case SetupMessage setupMessage                                             -> onSetup(setupMessage);
            case StartMessage ignored                                                  -> onStart();
            case AvailableDirectionsReplyMessage availableDirectionsReplyMessage       -> onAvailableDirectionsReply(availableDirectionsReplyMessage);
            case ChosenDirectionIgnoredMessage chosenDirectionIgnoredMessage           -> onChosenDirectionIgnored(chosenDirectionIgnoredMessage);
            case TargetReachedMessage ignored                                          -> onTargetReached();
            case PauseMessage ignored                                                  -> onPause();
            case ResumeMessage ignored                                                 -> onResume();
            case ListingResponse listingResponse                                       -> onListingResponse(listingResponse);
            case UnregisteredMessage ignored                                           -> onUnregister();
            case UnexpectedMessage unexpectedMessage                                   -> onUnexpectedMessage(unexpectedMessage);
            case TimerMessagePlay ignored                                                  -> onTimerMessage();

			default -> throw new IllegalStateException("Unexpected value: " + message);
		};
	}

	// Track bot's current position (initialized at the start)
	private int currentX = 0;
	private int currentY = 0;
	private int stuckCounter = 0;  // Counter for tracking consecutive dead-ends
	private final int maxStuckTries = 5;  // Maximum attempts to avoid dead-ends before returning to start
	private final Map<Position, Boolean> visitedPositionsMap = new HashMap<>(); // Map to track blocked/unblocked positions

	// Handles bot's movement decisions, including dead-end avoidance
	private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message) {
		getContext().getLog().info("Received available directions from board {}", message.directions());
		List<Direction> availableDirections = message.directions();
        timers.startSingleTimer(TIMER_KEY_PLAY, new TimerMessagePlay(), Duration.ofMillis(sleepTime));
        int currentDistance = message.distance();
		moveCount++;

		// Wenn der Bot keine Richtung mehr wählen kann
		if (availableDirections.isEmpty()) {
			getContext().getLog().warn("No available directions! The bot might be stuck!");
			return this;
		}

		// Move towards target if it is one step away
		if (currentDistance == 1) {
			boardRef.tell(new ChosenDirectionMessage(availableDirections.get(0), botRef));
			return this;
		}

		// Stop if target reached
		if (currentDistance == 0) {
			return this;
		}

		// Bot checks for unvisited directions if it's at a dead-end
		if (recentDirections.isEmpty()) {
			Direction firstUnvisitedDirection = findFirstUnvisitedDirection(availableDirections);
			if (firstUnvisitedDirection != null) {
				boardRef.tell(new ChosenDirectionMessage(firstUnvisitedDirection, botRef));
				recentDirections.add(firstUnvisitedDirection);
				Position firstPosition = getNewPosition(firstUnvisitedDirection);
				visitedPositionsMap.put(firstPosition, true);
				stuckCounter = 0;
			}
			return this;
		}

		// Find the best unblocked direction
		Direction bestDirection = null;
		for (Direction direction : availableDirections) {
			Position newPosition = getNewPosition(direction);
			// Ensure position is not blocked
			if (!visitedPositionsMap.containsKey(newPosition) && !newPosition.isBlocked()) {
				bestDirection = direction;
				break;
			}
		}

		// Check if bot is in a deadlock situation
		if (bestDirection == null) {
			stuckCounter++;
			getContext().getLog().info("Bot has attempted {} moves, but is stuck.", stuckCounter);

			// If max attempts reached, return to start
			if (stuckCounter >= maxStuckTries) {
				getContext().getLog().warn("Bot is in a deadlock after {} attempts, returning to start.", maxStuckTries);

				// Calculate return path
				List<Direction> returnPath = calculateReturnPathToStart();

				// Bot returns to start following calculated path
				for (Direction returnDirection : returnPath) {
					boardRef.tell(new ChosenDirectionMessage(returnDirection, botRef));
					getContext().getLog().info("Bot moving back to start, step: {}", returnDirection);
				}

				// Reset tracking variables
				recentDirections.clear();
				visitedPositionsMap.clear();
				visitedPositionsMap.put(new Position(0, 0, Direction.N), true);
				stuckCounter = 0;
			} else {
				// Continue searching for alternative directions
				boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
			}
		} else {
			// Proceed in best found direction
			boardRef.tell(new ChosenDirectionMessage(bestDirection, botRef));
			recentDirections.add(bestDirection);
			visitedPositionsMap.put(getNewPosition(bestDirection), true);
			stuckCounter = 0;
		}

		return this;
	}

	private List<Direction> calculateReturnPathToStart() {
		// Logic for calculating the return path to the starting point.
		return new ArrayList<>(recentDirections);  // Example: Returning in reverse order
	}

	// Map to track blocked positions
	private final Set<Position> blockedPositions = new HashSet<>();

	// In der Position Klasse könnte eine Methode hinzugefügt werden, um festzustellen, ob sie blockiert ist
	class Position {
		int x, y;
		Direction direction;

		public Position(int x, int y, Direction direction) {
			this.x = x;
			this.y = y;
			this.direction = direction;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			Position position = (Position) obj;
			return x == position.x && y == position.y && direction == position.direction;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y, direction);
		}

		// Überprüfen, ob diese Position blockiert ist
		public boolean isBlocked() {
			return blockedPositions.contains(this);
		}
	}

	private Direction findFirstUnvisitedDirection(List<Direction> availableDirections) {
		// Iteriere durch alle verfügbaren Richtungen
		for (Direction direction : availableDirections) {
			// Berechne die neue Position basierend auf der Richtung
			Position newPosition = getNewPosition(direction);

			// Wenn die Position noch nicht besucht wurde, gib die Richtung zurück
			if (!visitedPositionsMap.containsKey(newPosition)) {
				return direction;
			}
		}
		return null;  // Wenn keine unbesuchte Richtung gefunden wird, gib null zurück
	}

	// Returns the position based on the current direction
	private Position getNewPosition(Direction direction) {
		int newX = currentX;
		int newY = currentY;

		switch (direction) {
			case N: newY -= 1; break;
			case E: newX += 1; break;
			case S: newY += 1; break;
			case W: newX -= 1; break;
		}

		return new Position(newX, newY, direction);
	}

    private Behavior<Message> onSetup(SetupMessage setupMessage){
        this.currentPhase = Phase.READY;
        sleepTime = setupMessage.sleepTime();
        getContext().getLog().info("Bot {} got setup message", actorName);
        getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
        return this;
    }

    private Behavior<Message> onStart(){
        this.currentPhase = Phase.PLAYING;
        getContext().getLog().info("Bot {} got start message", actorName);
        getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
        boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
        return this;
    }

    private Behavior<Message> onChosenDirectionIgnored(ChosenDirectionIgnoredMessage chosenDirectionIgnoredMessage){
        getContext().getLog().info("Chosen direction was ignored. Reason: {}", chosenDirectionIgnoredMessage.reason());
        boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
        return this;
    }

	private Behavior<Message> onTargetReached() {
		getContext().getLog().info("Bot {} reached target", actorName);
		System.out.println("Bot has played " + this.moveCount + " moves");
		timers.startSingleTimer(TIMER_KEY_REACHED, new TimerMessagePlay(), Duration.ofMillis(5000));
		getContext().getLog().info("New target reached timer got started");
		boardRef.tell(new DeregisterMessage("Bot reached Target", this.botRef));
		this.currentPhase = Phase.TARGET_REACHED;
		getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
		return this;
	}

    private Behavior<Message> onPause(){
        this.currentPhase = Phase.PAUSED;
        timers.cancel(TIMER_KEY_PLAY);
        getContext().getLog().info("Timer was stopped");
        getContext().getLog().info("Game was paused");
        getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
        return this;
    }

    private Behavior<Message> onResume(){
        this.currentPhase = Phase.PLAYING;
        getContext().getLog().info("Game was resumed");
        getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);

        boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
        return this;
    }

    private Behavior<Message> onUnregister(){
        getContext().getLog().info("Bot {} is deregistered ", actorName);
        return Behaviors.stopped();
    }

    private Behavior<Message> onListingResponse(ListingResponse listingResponse) {
        getContext().getLog().info("Received listing from receptionist");
        for (ActorRef<Message> boardRef : listingResponse.listing.getServiceInstances(serviceKeyForBoard)) {
            this.boardRef = boardRef;
			timers.startSingleTimer(TIMER_KEY_PLAY, new TimerMessagePlay(), Duration.ofMillis(5000));
			getContext().getLog().info("StartedRegisteringTimer");
            boardRef.tell(new RegisterMessage(actorName, botRef));
            getContext().getLog().info("Stored board reference from receptionist");
        }
        return this;
    }

    private Behavior<Message> onPing() {
        getContext().getLog().info("Bot {} got pinged", actorName);
        if(boardRef != null){
            boardRef.tell(new PingResponseMessage(actorName, botRef));
            getContext().getLog().info("Bot {} responded to the ping", actorName);
        }
        else{
            getContext().getLog().info("No board reference found");
        }
        return this;
    }

    private Behavior<Message> onUnexpectedMessage(UnexpectedMessage unexpectedMessage) {
        getContext().getLog().error(unexpectedMessage.description());
        return this;
    }

    private Behavior<Message> onTimerMessage() {
        if(this.currentPhase == Phase.PLAYING){
            boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
            getContext().getLog().info("Timer triggered a request");
        }
        else {
            timers.cancel(TIMER_KEY_PLAY);
            getContext().getLog().info("Timer was stopped");
        }
        return this;
    }

	private Behavior<Message> onTimerMessageRegistering() {
		if(this.currentPhase == Phase.REGISTERING){
			boardRef.tell(new RegisterMessage(actorName, botRef));
			getContext().getLog().info("Timer triggered a new registermessage");
		}
		else {
			timers.cancel(TIMER_KEY_REGISTER);
			getContext().getLog().info("Register Timer was stopped");
		}
		return this;
	}

	private Behavior<Message> onTimerMessageReached() {
		if(this.currentPhase == Phase.TARGET_REACHED){
			boardRef.tell(new DeregisterMessage("Bot reached target but ran into a timeout", botRef));
			getContext().getLog().info("Timer triggered a new deregister message");
		}
		else {
			timers.cancel(TIMER_KEY_REACHED);
			getContext().getLog().info("Reached Timer was stopped");
		}
		return this;
	}

}