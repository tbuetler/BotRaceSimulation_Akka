/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.bot;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.Receptionist.Listing;
import akka.actor.typed.receptionist.ServiceKey;
import ch.bfh.akka.botrace.common.BoardService;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.common.boardmessage.*;
import ch.bfh.akka.botrace.common.boardmessage.PingMessage;
import ch.bfh.akka.botrace.common.botmessage.*;

import java.util.*;

/**
 * The root actor of the Bot actor system.
 */
public class BotRoot extends AbstractOnMessageBehavior<Message> { // guardian actor

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
        return Behaviors.setup(c -> new BotRoot(c, botName));
    }

    private enum Phase{
        REGISTERING, READY, PLAYING, PAUSED, TARGET_REACHED
    }
    private Phase currentPhase;
    private int moveCount;
    List<Direction> recentDirections;
    ArrayList<Integer> recentDistances;

    // Parameters for collision-avoiding
    private boolean avoiding = false;
    private int avoidTurn = -1;
    private Direction avoidDirection;

    /**
     * Upon creation of the Bot root actor, it tells the {@link Receptionist} its
     * interest in receiving a list of services.
     *
     * @param context the context of this actor system
     */
    private BotRoot(ActorContext<Message> context, String botName) {
        super(context);
        recentDirections = new ArrayList<>();
        this.recentDistances = new ArrayList<>();

        this.moveCount = 0;
        this.currentPhase = Phase.REGISTERING;
        // TODO initialize the root actor...

        ActorRef<Listing> listingResponseAdapter = context
                .messageAdapter(Listing.class, ListingResponse::new);
        context.getSystem().receptionist().tell(Receptionist.subscribe(serviceKeyForBoard, listingResponseAdapter));
        // TODO continue initialization, if necessary...

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

		// Weiter mit der normalen Verarbeitung der Nachrichten
		return switch(message){
			case PingMessage ignored -> onPing();
			case SetupMessage setupMessage -> onSetup(setupMessage);
			case StartMessage ignored -> onStart();
			case AvailableDirectionsReplyMessage availableDirectionsReplyMessage -> onAvailableDirectionsReply(availableDirectionsReplyMessage);
			case ChosenDirectionIgnoredMessage chosenDirectionIgnoredMessage -> onChosenDirectionIgnored(chosenDirectionIgnoredMessage);
			case TargetReachedMessage ignored -> onTargetReached();
			case PauseMessage ignored -> onPause();
			case ResumeMessage ignored -> onResume();
			case ListingResponse listingResponse -> onListingResponse(listingResponse);
			case UnregisteredMessage ignored -> onUnregister();
			case UnexpectedMessage unexpectedMessage -> onUnexpectedMessage(unexpectedMessage);

			default -> throw new IllegalStateException("Unexpected value: " + message);
		};
	}

	// Track bot's current position (initialized at the start)
	private int currentX = 0;
	private int currentY = 0;
	private int stuckCounter = 0;  // Counter for tracking consecutive dead-ends
	private int maxStuckTries = 5;  // Maximum attempts to avoid dead-ends before returning to start
	private Set<Position> visitedPositions = new HashSet<>(); // Track visited positions
	private Set<Position> unvisitedPositions = new HashSet<>();  // Track unvisited positions
	private Map<Position, Boolean> visitedPositionsMap = new HashMap<>(); // Map to track blocked/unblocked positions

	// Handles bot's movement decisions, including dead-end avoidance
	private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message) {
		getContext().getLog().info("Received available directions from board {}", message.directions());
		List<Direction> availableDirections = message.directions();
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
	private Set<Position> blockedPositions = new HashSet<>();

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

	private boolean allFieldsVisited() {
		// Logik, um zu prüfen, ob alle Felder besucht wurden
		return unvisitedPositions.isEmpty();
	}

	private void saveCurrentPosition(Direction direction) {
		// Aktualisiere die Position und entferne sie aus der Liste der unbesuchten Felder
		Position currentPosition = new Position(currentX, currentY, direction);
		visitedPositions.add(currentPosition);
		unvisitedPositions.remove(currentPosition);

		// Nach jeder Bewegung die aktuelle Position des Bots aktualisieren
		switch (direction) {
			case N: currentY -= 1; break;
			case E: currentX += 1; break;
			case S: currentY += 1; break;
			case W: currentX -= 1; break;
		}
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

	// Helper method to calculate direction towards a given position
	private Direction calculateDirectionToPosition(Position targetPosition) {
		// Determine the relative direction to move towards the target position
		int deltaX = targetPosition.x - currentX;
		int deltaY = targetPosition.y - currentY;

		if (Math.abs(deltaX) > Math.abs(deltaY)) {
			// Horizontal movement (left or right)
			if (deltaX > 0) {
				return Direction.E;
			} else {
				return Direction.W;
			}
		} else {
			// Vertical movement (up or down)
			if (deltaY > 0) {
				return Direction.S;
			} else {
				return Direction.N;
			}
		}
	}

    private Behavior<Message> onSetup(SetupMessage setupMessage){
        this.currentPhase = Phase.READY;
        getContext().getLog().info("Bot {} got setup message", actorName);
        getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
        return this;
    }

    private Behavior<Message> onStart(){
        getContext().getLog().info("Bot {} got start message", actorName);
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
		boardRef.tell(new DeregisterMessage("Bot reached Target", this.botRef));
		this.currentPhase = Phase.TARGET_REACHED;
		getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
		return this;
	}


    private Behavior<Message> onPause(){
        this.currentPhase = Phase.PAUSED;
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
}