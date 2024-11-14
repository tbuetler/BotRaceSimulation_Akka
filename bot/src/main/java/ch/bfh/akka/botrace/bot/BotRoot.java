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

    /*
    avoidDirection says which direction has been chosen to avoid collision
    -1 = no collision
    0 = left
    1 = right

     */
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

	private Set<Position> visitedPositions = new HashSet<>();
	private Map<Integer, Set<Direction>> visitedDirectionsByDistance = new HashMap<>();
	private int currentX = 0; // Start at position (0, 0)
	private int currentY = 0; // Start at position (0, 0)

	private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message) {
		getContext().getLog().info("Received available directions from board {}", message.directions());
		List<Direction> availableDirections = message.directions();
		int currentDistance = message.distance();

		// Wenn keine Richtung verfügbar ist, ist der Bot wahrscheinlich blockiert.
		if (availableDirections.isEmpty()) {
			getContext().getLog().info("No available directions. Bot is stuck!");
			return this;
		}

		Direction bestDirection = null;
		int smallestDistance = currentDistance;
		Map<Direction, Integer> directionDistances = new HashMap<>();

		// Berechne die Distanz nach jedem möglichen Schritt
		for (Direction direction : availableDirections) {
			int newDistance = simulateDistanceAfterMove(direction, currentDistance);

			// Vermeide Richtungen, die den Bot in bereits besuchte Positionen führen
			if (hasVisitedPosition(currentX, currentY, direction)) {
				continue;
			}

			// Wähle die Richtung mit der kürzesten Distanz zum Ziel
			if (newDistance < smallestDistance) {
				smallestDistance = newDistance;
				bestDirection = direction;
			}

			// Füge die Richtung der Liste der besuchten Richtungen hinzu
			visitedDirectionsByDistance
					.computeIfAbsent(currentDistance, k -> new HashSet<>())
					.add(direction);
		}

		// Falls keine bessere Richtung gefunden wird, wähle eine alternative Richtung
		if (bestDirection == null) {
			bestDirection = findAlternativeDirection(availableDirections, directionDistances, currentDistance);
		}

		// Sende den Befehl zum Bewegen
		boardRef.tell(new ChosenDirectionMessage(bestDirection, botRef));
		recentDirections.add(bestDirection);
		saveCurrentPosition(bestDirection); // Speichere die neue Position nach dem Schritt

		return this;
	}


	// Improved method to handle obstacles by choosing a random direction to avoid obstacles
	private Direction findAlternativeDirection(List<Direction> availableDirections, Map<Direction, Integer> directionDistances, int currentDistance) {
		Direction alternative = null;
		int smallestDistance = currentDistance;

		// Try finding a direction that is not visited and improves distance
		for (Direction dir : availableDirections) {
			int dist = directionDistances.getOrDefault(dir, currentDistance);
			Set<Direction> visitedAtDist = visitedDirectionsByDistance.getOrDefault(dist, new HashSet<>());

			if (!visitedAtDist.contains(dir) && dist < smallestDistance) {
				alternative = dir;
				smallestDistance = dist;
				break;
			}
		}

		// If no optimal direction found, fallback to random direction
		return (alternative != null) ? alternative : availableDirections.get(new Random().nextInt(availableDirections.size()));
	}

	// Method to track visited positions
	private boolean hasVisitedPosition(int x, int y, Direction direction) {
		return visitedPositions.contains(new Position(x, y, direction));
	}

	// Saves the bot's position after it moves
	private void saveCurrentPosition(Direction direction) {
		switch (direction) {
			case N: currentY -= 1; break;
			case E: currentX += 1; break;
			case S: currentY += 1; break;
			case W: currentX -= 1; break;
		}
		visitedPositions.add(new Position(currentX, currentY, direction));
	}

	// Position class to track coordinates and direction
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
	}

	// Simulate the distance change after a move
	private int simulateDistanceAfterMove(Direction direction, int currentDistance) {
		switch(direction) {
			case N: return currentDistance - 1;
			case E: return currentDistance - 1;
			case S: return currentDistance + 1;
			case W: return currentDistance + 1;
			default: return currentDistance;  // Default for unexpected direction
		}
	}

	/*
    private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message){
        getContext().getLog().info("Received available directions from board {}", message.directions());

        List<Direction> directionList = message.directions();
        this.moveCount++;


        // first move of the game
        if(this.currentPhase == Phase.READY){

            List<Direction> root = new ArrayList<>();
            root.add(Direction.N);
            root.add(Direction.S);
            root.add(Direction.W);
            root.add(Direction.E);

            // first move is a random move from directionList
            int random = new Random().nextInt(root.size());
            //boardRef.tell(new ChosenDirectionMessage(root.get(random), this.botRef));
            boardRef.tell(new ChosenDirectionMessage(Direction.E, this.botRef));



            // save played direction && distanceToTarget
            //this.recentDirections.add(root.get(random));
            this.recentDirections.add(Direction.E);
            // change phase to playing
            this.currentPhase = Phase.PLAYING;
            getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
        }

        else if (avoiding) {

            // adjust path since it had to avoid collision
            Direction nextDirection;
            if(avoidTurn == 0){
                nextDirection = getTurnDirection(this.avoidDirection,1);
            }else{
                nextDirection = getTurnDirection(this.avoidDirection, 0);
            }

            if(lookIfMovePossible(directionList, nextDirection)){
                boardRef.tell(new ChosenDirectionMessage(nextDirection, this.botRef));
                avoiding = false;
            }else{
                System.out.println("two objects next to each other");
            }


        } else{

            //calculating optimal direction

            // distance to target is smaller now --> play same move again
            if(recentDistances.getLast()> message.distance()){

                boolean possible = lookIfMovePossible(directionList, recentDirections.getLast());
                // look if possible to play same move again
                if(possible){
                    boardRef.tell(new ChosenDirectionMessage(recentDirections.getLast(), this.botRef));
                    this.recentDirections.add(recentDirections.getLast());
                }else{

                    // avoiding obstacle


                    Direction lastDirection = this.recentDirections.getLast();

                    while(!possible){
                        //int random = new Random().nextInt(0,2);
                        int random = 0;

                        // random for choosing left or right to avoid wall
                        possible = lookIfMovePossible(directionList, getTurnDirection(lastDirection,random));
                        if(possible){
                            boardRef.tell(new ChosenDirectionMessage(getTurnDirection(lastDirection,random), this.botRef));
                            this.avoidTurn = random;
                            this.avoidDirection = getTurnDirection(lastDirection,random);
                            this.avoiding = true;
                        }else{
                            this.getContext().getLog().info("Could not move out of the way");
                            // try rotating again until move is possible
                            lastDirection = getTurnDirection(lastDirection, random);
                        }
                    }

                }
                // TODO: else try other move




                // distance to target is bigger now --> play opposite move
            } else if (recentDistances.getLast()< message.distance()) {

                if(lookIfMovePossible(directionList, getTurnDirection(recentDirections.getLast(), 2))){
                    boardRef.tell(new ChosenDirectionMessage(getTurnDirection(recentDirections.getLast(), 2), this.botRef));
                    this.recentDirections.add(getTurnDirection(recentDirections.getLast(), 2));
                }
                // TODO: else try other move

                // distance is equal play left or right (random)
            }else if (recentDistances.getLast()== message.distance()){

                int random = new Random().nextInt(0,2);

                if(lookIfMovePossible(directionList, getTurnDirection(recentDirections.getLast(), random))){
                    boardRef.tell(new ChosenDirectionMessage(getTurnDirection(recentDirections.getLast(), random), this.botRef));
                    this.recentDirections.add(getTurnDirection(recentDirections.getLast(), random));
                }
                // TODO: else try other move

            }
        }
        this.recentDistances.add(message.distance());
        return this;
    }
	*/

    private Direction getTurnDirection(Direction direction, int index){

        // turn index:
        // 0 = left
        // 1 = right
        // 2 = opposite


        switch(direction){
            case N: if(index == 0){return Direction.W;} else if (index == 1){return Direction.E;} else{return Direction.S;}
            case S: if(index == 0){return Direction.E;} else if (index == 1){return Direction.W;} else{return Direction.N;}
            case E: if(index == 0){return Direction.N;} else if (index == 1){return Direction.S;} else{return Direction.W;}
            case W: if(index == 0){return Direction.S;} else if (index == 1){return Direction.N;} else{return Direction.E;}
            case NE: if(index == 0){return Direction.NW;} else if (index == 1){return Direction.SE;} else{return Direction.NW;}
            case NW: if(index == 0){return Direction.SW;} else if (index == 1){return Direction.NE;} else{return Direction.SE;}
            case SE: if(index == 0){return Direction.NE;} else if (index == 1){return Direction.SW;} else{return Direction.NW;}
            case SW: if(index == 0){return Direction.SE;} else if (index == 1){return Direction.NW;} else{return Direction.NE;}
            default: throw new IllegalArgumentException("Unknown direction: " + this);
        }

    }

    private boolean lookIfMovePossible(List<Direction> directionList, Direction direction){
        if(directionList.contains(direction)){
            return true;
        }else{
            return false;
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