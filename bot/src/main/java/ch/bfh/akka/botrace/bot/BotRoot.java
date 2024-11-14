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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        this.recentDistances = new ArrayList<>();
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

    private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message){
        getContext().getLog().info("Received available directions from board {}", message.directions());

        List<Direction> directionList = message.directions();


        // first move of the game
        if(this.currentPhase == Phase.READY){
            this.moveCount++;

            // first move is a random move from directionList
            int random = new Random().nextInt(directionList.size());
            boardRef.tell(new ChosenDirectionMessage(directionList.get(random), this.botRef));

            // save played direction && distanceToTarget
            this.recentDirections.add(directionList.get(random));
            this.recentDistances.add(message.distance());

            // change phase to playing
            this.currentPhase = Phase.PLAYING;
            getContext().getLog().info("Bot {} switched to Phase: {}", actorName, this.currentPhase);
        }else{

            //calculating optimal direction

            // distance to target is smaller now --> play same move again
            if(recentDistances.getLast()> message.distance()){

                // look if possible to play same move again
                if(lookIfMovePossible(directionList, recentDirections.getLast())){
                    boardRef.tell(new ChosenDirectionMessage(recentDirections.getLast(), this.botRef));
                }
                // TODO: else try other move


                // distance to target is bigger now --> play opposite move
            } else if (recentDistances.getLast()< message.distance()) {

                if(lookIfMovePossible(directionList, getTurnDirection(recentDirections.getLast(), 2))){
                    boardRef.tell(new ChosenDirectionMessage(getTurnDirection(recentDirections.getLast(), 2), this.botRef));
                }


            }else if (recentDistances.getLast()== message.distance()){

            }


        }




        this.moveCount++;

        return this;
    }

    private Direction getTurnDirection(Direction direction, int index){
        // index:
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