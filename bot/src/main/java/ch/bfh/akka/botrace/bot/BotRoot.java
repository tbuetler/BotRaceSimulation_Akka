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

            default -> throw new IllegalStateException("Unexpected value: " + message);
        };
    }

	// BotRoot.java
	private Map<Integer, Set<Direction>> visitedDirectionsByDistance = new HashMap<>();
	private int currentX = 0; // Start at position (0, 0)
	private int currentY = 0; // Start at position (0, 0)
	private int stuckCounter = 0;  // Zähler für Rückkehrversuche
	private int maxStuckTries = 5;  // Maximalanzahl der Versuche, bevor der Bot zurückgeht
	private Set<Position> visitedPositions = new HashSet<>(); // Gespeicherte besuchte Positionen
	private Set<Position> unvisitedPositions = new HashSet<>();  // Unbesuchte Positionen
	private Map<Position, Boolean> visitedPositionsMap = new HashMap<>();

	// Logik zur Vermeidung von Sackgassen und Wiederverwendung von unbesuchten Positionen
	private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message) {
		getContext().getLog().info("Received available directions from board {}", message.directions());
		List<Direction> availableDirections = message.directions();
		int currentDistance = message.distance();

		// Wenn der Bot keine Richtung mehr wählen kann
		if (availableDirections.isEmpty()) {
			getContext().getLog().warn("No available directions! The bot might be stuck!");
			return this;
		}

		// wenn das ziel neben dem bot ist soll er darauf zu gehen
		if (currentDistance == 1) {
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

		// Wenn der Bot feststellt, dass er nicht weiterkommt
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

		// Wenn der Bot keine unbesuchte Richtung findet, prüft er, ob er in einer Sackgasse steckt
		Direction bestDirection = null;
		for (Direction direction : availableDirections) {
			Position newPosition = getNewPosition(direction);
			// Überprüfen, ob die Position blockiert ist
			if (!visitedPositionsMap.containsKey(newPosition) && !newPosition.isBlocked()) {
				bestDirection = direction;
				break;
			}
		}

		// Wenn keine gültige Richtung gefunden wird, prüft der Bot, ob er bereits in einer Sackgasse ist
		if (bestDirection == null) {
			stuckCounter++;
			getContext().getLog().info("Bot has attempted {} moves, but is stuck.", stuckCounter);

			if (stuckCounter >= maxStuckTries) {
				// Der Bot ist in einer Sackgasse, also muss er zurückkehren
				getContext().getLog().warn("Bot is in a deadlock after {} attempts, returning to start.", maxStuckTries);

				// Berechne die Rückkehrrichtung
				List<Direction> returnPath = calculateReturnPathToStart(); // Berechnung des Rückwegs

				// Der Bot kehrt zurück
				for (Direction returnDirection : returnPath) {
					boardRef.tell(new ChosenDirectionMessage(returnDirection, botRef));
					getContext().getLog().info("Bot moving back to start, step: {}", returnDirection);
				}

				// Rücksetzen der Richtungsliste und Positionen
				recentDirections.clear();
				visitedPositionsMap.clear();
				visitedPositionsMap.put(new Position(0, 0, Direction.N), true); // Stelle sicher, dass der Startpunkt besucht wird
				stuckCounter = 0;  // Zähler zurücksetzen
			} else {
				// Weitere Versuche, eine Richtung zu finden
				boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
			}
		} else {
			// Eine gültige Richtung gefunden, gehe dorthin
			boardRef.tell(new ChosenDirectionMessage(bestDirection, botRef));
			recentDirections.add(bestDirection);
			visitedPositionsMap.put(getNewPosition(bestDirection), true);
			stuckCounter = 0;
		}

		return this;
	}


	private Direction findBestDirection(List<Direction> availableDirections, int currentDistance) {
		Direction bestDirection = null;

		// Priorität auf unbesuchte Positionen legen
		for (Direction direction : availableDirections) {
			Position newPosition = getNewPosition(direction);
			if (!visitedPositionsMap.containsKey(newPosition) && !newPosition.isBlocked()) {
				bestDirection = direction;
				break;
			}
		}

		// Falls keine unbesuchte Position gefunden wurde, versuche eine der bekannten Richtungen
		if (bestDirection == null) {
			for (Direction direction : availableDirections) {
				Position newPosition = getNewPosition(direction);
				if (!newPosition.isBlocked()) {
					bestDirection = direction;
					break;
				}
			}
		}

		return bestDirection;
	}

	private List<Direction> calculateReturnPathToStart() {
		// Hier kann ein Algorithmus implementiert werden, um die Rückkehrrichtung zu berechnen,
		// abhängig davon, wie der Bot auf dem Grid navigiert.
		// Dies kann durch Rückverfolgen der Positionen und Richtungen des Bots erfolgen.
		// Eine einfache Methode könnte es sein, die Richtungen umzukehren und in umgekehrter Reihenfolge zu bewegen.

		return new ArrayList<>(recentDirections);  // Beispiel: Rückkehr in umgekehrter Reihenfolge
	}

	// Füge eine neue Map hinzu, um blockierte Positionen zu verfolgen
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

    private Behavior<Message> onTargetReached(){
        getContext().getLog().info("Bot reached target");
        System.out.println("Bot has played"+ this.moveCount+" moves");
        boardRef.tell(new DeregisterMessage("Bot reached Target", this.botRef));
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